package com.arc_e_tect.gradle.doppelganger.scan;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.model.PathTemplates;
import com.arc_e_tect.gradle.detector.core.scan.LiteralPathResolver;
import com.arc_e_tect.gradle.detector.core.scan.PropertyResolutionContext;
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
 * {@link ContractVerificationSource} for Spring RestDocs, recognising three independent call-chain
 * shapes for the same underlying convention - a request-builder call paired with a
 * {@code document(...)} call somewhere in the same method body:
 *
 * <ul>
 *     <li>{@code spring-restdocs-mockmvc}: {@code mockMvc.perform(get(...)/post(...)/put(...)/
 *     delete(...)/patch(...))} together with {@code .andDo(document(...))}.</li>
 *     <li>{@code spring-restdocs-webtestclient}: {@code webTestClient.get()/post()/put()/
 *     delete()/patch().uri(...).exchange()...consumeWith(document(...))} - the HTTP verb comes
 *     from the request-builder method and the path from the {@code uri(...)} call.</li>
 *     <li>{@code spring-restdocs-restassured}: {@code given(...).filter(document(...))...
 *     when().get(...)/post(...)/put(...)/delete(...)/patch(...)} - the verb call directly scoped
 *     on a call named {@code when}.</li>
 * </ul>
 *
 * <p>Matches by simple method name only, the same way {@code ControllerScanner} matches mapping
 * annotations by simple name, so this scanner needs neither Spring MVC Test, Spring RestDocs, nor
 * REST Assured on its own classpath.</p>
 */
public class RestDocsScanner implements ContractVerificationSource {

    private static final Map<String, HttpVerb> VERB_BUILDER_METHODS = Map.of(
            "get", HttpVerb.GET,
            "post", HttpVerb.POST,
            "put", HttpVerb.PUT,
            "delete", HttpVerb.DELETE,
            "patch", HttpVerb.PATCH);

    private final String basePathToStrip;
    private final PropertyResolutionContext propertyResolutionContext;

    /** Creates a new {@code RestDocsScanner} that strips no base path from captured paths. */
    public RestDocsScanner() {
        this("");
    }

    /**
     * Creates a new {@code RestDocsScanner} that strips {@code basePathToStrip} - typically
     * resolved via {@link OpenApiServerBasePath#resolve(java.io.File)} - from the leading segments
     * of every path it captures, when present. A REST Assured request built against a running
     * server naturally includes this server-url path (e.g. a servlet context path) in the literal
     * path it captures, even though neither the OpenAPI documentation nor the
     * {@code @RestController} mapping it verifies ever declares it.
     *
     * @param basePathToStrip the base path to strip, e.g. {@code "/user-account-service"}; blank or
     *                         {@code null} disables stripping
     */
    public RestDocsScanner(String basePathToStrip) {
        this(basePathToStrip, PropertyResolutionContext.empty());
    }

    /**
     * Creates a new {@code RestDocsScanner} that additionally resolves a request-builder call's
     * path argument against {@code propertyResolutionContext} when it is a configured
     * helper-method call or an {@code @Value}-annotated field, in addition to the literal/
     * literal-initialized-constant shapes {@link LiteralPathResolver} always resolves.
     *
     * @param basePathToStrip           see {@link #RestDocsScanner(String)}
     * @param propertyResolutionContext out-of-band property knowledge; pass
     *                                  {@link PropertyResolutionContext#empty()} for none
     */
    public RestDocsScanner(String basePathToStrip, PropertyResolutionContext propertyResolutionContext) {
        this.basePathToStrip = basePathToStrip == null || basePathToStrip.isBlank()
                ? "" : PathTemplates.normalize(basePathToStrip);
        this.propertyResolutionContext = propertyResolutionContext == null
                ? PropertyResolutionContext.empty() : propertyResolutionContext;
    }

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

        boolean documented = calls.stream().anyMatch(call -> isAndDoDocument(call)
                || isConsumeWithDocument(call)
                || isFilterDocument(call));
        if (!documented) {
            return null;
        }

        for (MethodCallExpr call : calls) {
            if (!isPerformArgument(call) && !isWhenScoped(call) && !isWebTestClientUriScoped(call)) {
                continue;
            }
            VerbAndPath verbAndPath = asVerbAndPath(call);
            if (verbAndPath != null) {
                String signature = method.getNameAsString() + "()";
                int line = method.getBegin().map(p -> p.line).orElse(0);
                Endpoint endpoint = new Endpoint(verbAndPath.verb(), stripBasePath(verbAndPath.path()), declaringClass,
                        signature, fileName, line);
                return new VerifiedContractTest(endpoint, StatusCodeDetector.detect(calls).orElse(null));
            }
        }
        return null;
    }

    private String stripBasePath(String normalizedPath) {
        if (basePathToStrip.isEmpty()) {
            return normalizedPath;
        }
        if (normalizedPath.equals(basePathToStrip)) {
            return "/";
        }
        if (normalizedPath.startsWith(basePathToStrip + "/")) {
            return normalizedPath.substring(basePathToStrip.length());
        }
        return normalizedPath;
    }

    /** {@code mockMvc.perform(get(...)/post(...)/...)} - the verb call is {@code perform}'s argument. */
    private boolean isPerformArgument(MethodCallExpr call) {
        return call.getParentNode()
                .filter(MethodCallExpr.class::isInstance)
                .map(MethodCallExpr.class::cast)
                .filter(parent -> parent.getNameAsString().equals("perform"))
                .isPresent();
    }

    /** {@code ....when().get(...)/post(...)/...} - the verb call is scoped on a call named {@code when}. */
    private boolean isWhenScoped(MethodCallExpr call) {
        return call.getScope()
                .filter(MethodCallExpr.class::isInstance)
                .map(MethodCallExpr.class::cast)
                .filter(scope -> scope.getNameAsString().equals("when"))
                .isPresent();
    }

    private boolean isAndDoDocument(MethodCallExpr call) {
        if (!call.getNameAsString().equals("andDo") || call.getArguments().isEmpty()) {
            return false;
        }
        Expression arg = call.getArgument(0);
        return arg.isMethodCallExpr() && arg.asMethodCallExpr().getNameAsString().equals("document");
    }

    /** {@code .consumeWith(document(...))} - the {@code spring-restdocs-webtestclient} terminal call. */
    private boolean isConsumeWithDocument(MethodCallExpr call) {
        if (!call.getNameAsString().equals("consumeWith") || call.getArguments().isEmpty()) {
            return false;
        }
        Expression arg = call.getArgument(0);
        return arg.isMethodCallExpr() && arg.asMethodCallExpr().getNameAsString().equals("document");
    }

    /** {@code .filter(document(...))} - the {@code spring-restdocs-restassured} counterpart of {@code andDo}. */
    private boolean isFilterDocument(MethodCallExpr call) {
        if (!call.getNameAsString().equals("filter")) {
            return false;
        }
        return call.getArguments().stream()
                .anyMatch(arg -> arg.isMethodCallExpr() && arg.asMethodCallExpr().getNameAsString().equals("document"));
    }

    /** {@code webTestClient.get()/post()/...().uri(...)} - the path call is scoped on the verb call. */
    private boolean isWebTestClientUriScoped(MethodCallExpr call) {
        if (!call.getNameAsString().equals("uri") || call.getArguments().isEmpty()) {
            return false;
        }
        return call.getScope()
                .filter(MethodCallExpr.class::isInstance)
                .map(MethodCallExpr.class::cast)
                .filter(scope -> VERB_BUILDER_METHODS.containsKey(scope.getNameAsString()))
                .isPresent();
    }

    private VerbAndPath asVerbAndPath(MethodCallExpr call) {
        if (isWebTestClientUriScoped(call)) {
            MethodCallExpr scope = call.getScope().map(MethodCallExpr.class::cast).orElse(null);
            if (scope == null || !scope.getArguments().isEmpty()) {
                return null;
            }
            HttpVerb verb = VERB_BUILDER_METHODS.get(scope.getNameAsString());
            if (verb == null) {
                return null;
            }
            return LiteralPathResolver.resolve(call.getArgument(0), propertyResolutionContext)
                    .map(path -> new VerbAndPath(verb, PathTemplates.normalize(path)))
                    .orElse(null);
        }

        HttpVerb verb = VERB_BUILDER_METHODS.get(call.getNameAsString());
        if (verb == null || call.getArguments().isEmpty()) {
            return null;
        }
        return LiteralPathResolver.resolve(call.getArgument(0), propertyResolutionContext)
                .map(path -> new VerbAndPath(verb, PathTemplates.normalize(path)))
                .orElse(null);
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
