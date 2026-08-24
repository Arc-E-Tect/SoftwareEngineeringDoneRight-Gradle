package com.arc_e_tect.gradle.doppelganger.scan;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.model.PathTemplates;
import com.arc_e_tect.gradle.doppelganger.detect.ContractVerificationSource;
import com.arc_e_tect.gradle.doppelganger.detect.VerifiedContractTest;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link ContractVerificationSource} for the Atlassian OpenAPI request validator: a test method
 * whose call chain contains either {@code .filter(new OpenApiValidationFilter(...))} (REST
 * Assured style) or a direct {@code .validateRequest(...)} / {@code .validateResponse(...)} call,
 * paired with a REST-Assured-style request built via {@code given()...when()...get(...)} calls in
 * the same method.
 *
 * <p>The library's Maven coordinates changed after v3.0.0 from {@code swagger-request-validator-*}
 * to {@code openapi-request-validator-*} under {@code com.atlassian.oai}; this scanner matches by
 * simple class/method names only, so both old and new artifact versions are recognised without
 * needing either on its own classpath.</p>
 */
public class OpenApiRequestValidatorScanner implements ContractVerificationSource {

    private static final Map<String, HttpVerb> VERB_BUILDER_METHODS = Map.of(
            "get", HttpVerb.GET,
            "post", HttpVerb.POST,
            "put", HttpVerb.PUT,
            "delete", HttpVerb.DELETE,
            "patch", HttpVerb.PATCH);

    /** Creates a new {@code OpenApiRequestValidatorScanner}. */
    public OpenApiRequestValidatorScanner() {}

    /** {@inheritDoc} */
    @Override
    public List<Endpoint> scan(File rootDir) throws IOException {
        List<Endpoint> endpoints = new ArrayList<>();
        for (VerifiedContractTest test : scanWithStatusCodes(rootDir)) {
            endpoints.add(test.endpoint());
        }
        return endpoints;
    }

    /** {@inheritDoc} */
    @Override
    public List<VerifiedContractTest> scanWithStatusCodes(File rootDir) throws IOException {
        List<VerifiedContractTest> tests = new ArrayList<>();
        for (File javaFile : collectJavaFiles(rootDir)) {
            tests.addAll(scanFile(javaFile));
        }
        return tests;
    }

    private List<VerifiedContractTest> scanFile(File sourceFile) throws IOException {
        List<VerifiedContractTest> tests = new ArrayList<>();

        ParseResult<CompilationUnit> parseResult = new JavaParser(
                new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21))
                .parse(sourceFile);
        if (!parseResult.isSuccessful() || parseResult.getResult().isEmpty()) {
            return tests;
        }

        CompilationUnit cu = parseResult.getResult().get();
        String fileName = sourceFile.getName();

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
            String declaringClass = buildFqcn(cu, cls);
            cls.getMethods().forEach(method -> {
                VerifiedContractTest test = verifiedTestForMethod(method, declaringClass, fileName);
                if (test != null) {
                    tests.add(test);
                }
            });
        });

        return tests;
    }

    private VerifiedContractTest verifiedTestForMethod(MethodDeclaration method, String declaringClass, String fileName) {
        List<MethodCallExpr> calls = method.findAll(MethodCallExpr.class);

        boolean validated = calls.stream().anyMatch(this::isValidationCall);
        if (!validated) {
            return null;
        }

        boolean isRestAssuredStyle = calls.stream().anyMatch(c -> c.getNameAsString().equals("given"))
                && calls.stream().anyMatch(c -> c.getNameAsString().equals("when"));
        if (!isRestAssuredStyle) {
            return null;
        }

        for (MethodCallExpr call : calls) {
            VerbAndPath verbAndPath = asVerbAndPath(call);
            if (verbAndPath != null) {
                String signature = method.getNameAsString() + "()";
                int line = method.getBegin().map(p -> p.line).orElse(0);
                Endpoint endpoint = new Endpoint(
                        verbAndPath.verb(), verbAndPath.path(), declaringClass, signature, fileName, line);
                return new VerifiedContractTest(endpoint, StatusCodeDetector.detect(calls).orElse(null));
            }
        }
        return null;
    }

    private boolean isValidationCall(MethodCallExpr call) {
        String name = call.getNameAsString();
        if (name.equals("validateRequest") || name.equals("validateResponse")) {
            return true;
        }
        if (!name.equals("filter") || call.getArguments().isEmpty()) {
            return false;
        }
        Expression arg = call.getArgument(0);
        return arg.isObjectCreationExpr()
                && arg.asObjectCreationExpr().getType().getNameAsString().equals("OpenApiValidationFilter");
    }

    private VerbAndPath asVerbAndPath(MethodCallExpr call) {
        HttpVerb verb = VERB_BUILDER_METHODS.get(call.getNameAsString());
        if (verb == null || call.getArguments().isEmpty()) {
            return null;
        }
        Expression first = call.getArgument(0);
        if (!first.isStringLiteralExpr()) {
            return null;
        }
        return new VerbAndPath(verb, PathTemplates.normalize(first.asStringLiteralExpr().asString()));
    }

    private List<File> collectJavaFiles(File dir) {
        List<File> files = new ArrayList<>();
        collectJavaFiles(dir, files);
        return files;
    }

    private void collectJavaFiles(File dir, List<File> files) {
        if (!dir.isDirectory()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isFile() && child.getName().endsWith(".java")) {
                files.add(child);
            } else if (child.isDirectory()) {
                collectJavaFiles(child, files);
            }
        }
    }

    private String buildFqcn(CompilationUnit cu, ClassOrInterfaceDeclaration cls) {
        String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
        String nestedName = buildNestedName(cls);
        return pkg.isEmpty() ? nestedName : pkg + "." + nestedName;
    }

    private String buildNestedName(ClassOrInterfaceDeclaration cls) {
        if (cls.getParentNode().isPresent()
                && cls.getParentNode().get() instanceof ClassOrInterfaceDeclaration parent) {
            return buildNestedName(parent) + "." + cls.getNameAsString();
        }
        return cls.getNameAsString();
    }

    /** HTTP verb and path template resolved from a single request-builder call. */
    private record VerbAndPath(HttpVerb verb, String path) {}
}
