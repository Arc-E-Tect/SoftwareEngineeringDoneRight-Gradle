package com.arc_e_tect.gradle.doppelganger.scan;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.model.PathTemplates;
import com.arc_e_tect.gradle.doppelganger.detect.ContractVerificationSource;
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
 * {@link ContractVerificationSource} for Spring RestDocs: a test method whose call chain contains
 * a {@code mockMvc.perform(get(...)/post(...)/put(...)/delete(...)/patch(...))} call and also
 * includes {@code .andDo(document(...))} somewhere in the same method body.
 *
 * <p>Matches by simple method name only, the same way {@code ControllerScanner} matches mapping
 * annotations by simple name, so this scanner needs neither Spring MVC Test nor Spring RestDocs
 * on its own classpath.</p>
 */
public class RestDocsScanner implements ContractVerificationSource {

    private static final Map<String, HttpVerb> VERB_BUILDER_METHODS = Map.of(
            "get", HttpVerb.GET,
            "post", HttpVerb.POST,
            "put", HttpVerb.PUT,
            "delete", HttpVerb.DELETE,
            "patch", HttpVerb.PATCH);

    /** Creates a new {@code RestDocsScanner}. */
    public RestDocsScanner() {}

    /** {@inheritDoc} */
    @Override
    public List<Endpoint> scan(File rootDir) throws IOException {
        List<Endpoint> endpoints = new ArrayList<>();
        for (File javaFile : collectJavaFiles(rootDir)) {
            endpoints.addAll(scanFile(javaFile));
        }
        return endpoints;
    }

    private List<Endpoint> scanFile(File sourceFile) throws IOException {
        List<Endpoint> endpoints = new ArrayList<>();

        ParseResult<CompilationUnit> parseResult = new JavaParser(
                new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21))
                .parse(sourceFile);
        if (!parseResult.isSuccessful() || parseResult.getResult().isEmpty()) {
            return endpoints;
        }

        CompilationUnit cu = parseResult.getResult().get();
        String fileName = sourceFile.getName();

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
            String declaringClass = buildFqcn(cu, cls);
            cls.getMethods().forEach(method -> {
                Endpoint endpoint = endpointForMethod(method, declaringClass, fileName);
                if (endpoint != null) {
                    endpoints.add(endpoint);
                }
            });
        });

        return endpoints;
    }

    private Endpoint endpointForMethod(MethodDeclaration method, String declaringClass, String fileName) {
        List<MethodCallExpr> calls = method.findAll(MethodCallExpr.class);

        boolean documented = calls.stream().anyMatch(this::isAndDoDocument);
        if (!documented) {
            return null;
        }

        for (MethodCallExpr call : calls) {
            if (!isPerformArgument(call)) {
                continue;
            }
            VerbAndPath verbAndPath = asVerbAndPath(call);
            if (verbAndPath != null) {
                String signature = method.getNameAsString() + "()";
                int line = method.getBegin().map(p -> p.line).orElse(0);
                return new Endpoint(
                        verbAndPath.verb(), verbAndPath.path(), declaringClass, signature, fileName, line);
            }
        }
        return null;
    }

    private boolean isPerformArgument(MethodCallExpr call) {
        return call.getParentNode()
                .filter(MethodCallExpr.class::isInstance)
                .map(MethodCallExpr.class::cast)
                .filter(parent -> parent.getNameAsString().equals("perform"))
                .isPresent();
    }

    private boolean isAndDoDocument(MethodCallExpr call) {
        if (!call.getNameAsString().equals("andDo") || call.getArguments().isEmpty()) {
            return false;
        }
        Expression arg = call.getArgument(0);
        return arg.isMethodCallExpr() && arg.asMethodCallExpr().getNameAsString().equals("document");
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
