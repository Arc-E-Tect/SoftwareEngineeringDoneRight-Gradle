package com.arc_e_tect.gradle.mirage.scan;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.model.PathTemplates;
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
import java.util.Optional;

/**
 * Scans Java source files for WireMock's fluent Java stub-registration DSL, e.g.:
 *
 * <pre>{@code
 * stubFor(get(urlEqualTo("/orders/1")).willReturn(aResponse().withStatus(200)));
 * }</pre>
 *
 * <p>Unlike {@link WireMockStubScanner}, which reads static {@code *.json} stub mapping files,
 * this scanner finds stubs registered programmatically at test run time - evidence
 * {@code WireMockStubScanner} cannot "see" since no mapping file for it exists on disk. Matches
 * by simple method name only, the same way {@code ControllerScanner} and
 * {@code RestDocsScanner} match by simple annotation/method name, so this scanner needs neither
 * WireMock nor Spring on its own classpath.</p>
 *
 * <p>Recognises any call to a method named {@code stubFor(...)} with exactly one argument -
 * regardless of its scope, so both the statically-imported {@code stubFor(...)} and an instance
 * call like {@code wireMockServer.stubFor(...)} are matched - whose argument is a fluent chain
 * built from one of WireMock's verb builders ({@code get}/{@code post}/{@code put}/
 * {@code delete}/{@code patch}/{@code head}/{@code options}/{@code trace}) applied to one of its
 * URL matchers ({@code urlEqualTo}/{@code urlPathEqualTo}/{@code urlMatching}/
 * {@code urlPathMatching}/{@code urlPathTemplate}) with a literal string argument. Any other
 * builder calls chained in between - {@code .willReturn(...)}, {@code .atPriority(...)},
 * {@code .inScenario(...)}, etc. - are walked through transparently, since they are chained
 * directly onto the verb builder call.</p>
 *
 * <p>A {@code stubFor(...)} call whose argument isn't shaped this way - e.g. the mapping builder
 * was assembled in a local variable first, the URL matcher argument is not a string literal, or
 * the matcher is a dynamic one like {@code anyUrl()} - is silently skipped, the same way
 * {@code WireMockStubScanner} skips a stub file missing a recognised method/URL field.</p>
 */
public class WireMockJavaDslScanner {

    private static final Map<String, HttpVerb> VERB_METHODS = Map.of(
            "get", HttpVerb.GET,
            "post", HttpVerb.POST,
            "put", HttpVerb.PUT,
            "delete", HttpVerb.DELETE,
            "patch", HttpVerb.PATCH,
            "head", HttpVerb.HEAD,
            "options", HttpVerb.OPTIONS,
            "trace", HttpVerb.TRACE);

    private static final List<String> URL_MATCHER_METHODS = List.of(
            "urlEqualTo", "urlPathEqualTo", "urlMatching", "urlPathMatching", "urlPathTemplate");

    /** Creates a new {@code WireMockJavaDslScanner}. */
    public WireMockJavaDslScanner() {}

    /**
     * Scans every {@code .java} file under {@code rootDir}, recursively, for {@code stubFor(...)}
     * calls shaped as described in the class documentation.
     *
     * @param rootDir directory to search; may not exist, in which case an empty list is returned
     * @return one {@link Endpoint} per recognised {@code stubFor(...)} call found
     * @throws IOException if a source file cannot be read
     */
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
            cls.getMethods().forEach(method -> endpoints.addAll(stubsInMethod(method, declaringClass, fileName)));
        });

        return endpoints;
    }

    private List<Endpoint> stubsInMethod(MethodDeclaration method, String declaringClass, String fileName) {
        List<Endpoint> endpoints = new ArrayList<>();
        String signature = method.getNameAsString() + "()";
        for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
            if (!call.getNameAsString().equals("stubFor") || call.getArguments().size() != 1) {
                continue;
            }
            VerbAndPath verbAndPath = asVerbAndPath(call.getArgument(0));
            if (verbAndPath == null) {
                continue;
            }
            int line = call.getBegin().map(p -> p.line).orElse(0);
            endpoints.add(new Endpoint(
                    verbAndPath.verb(), verbAndPath.path(), declaringClass, signature, fileName, line));
        }
        return endpoints;
    }

    /**
     * Walks down the fluent {@code MappingBuilder} chain from {@code stubFor(...)}'s argument -
     * through any number of chained builder calls such as {@code .willReturn(...)} - to the verb
     * builder call at its root, e.g. {@code get(urlEqualTo("/orders/1"))}.
     */
    private VerbAndPath asVerbAndPath(Expression stubForArgument) {
        Expression current = stubForArgument;
        while (current.isMethodCallExpr()) {
            MethodCallExpr call = current.asMethodCallExpr();
            HttpVerb verb = VERB_METHODS.get(call.getNameAsString());
            if (verb != null && call.getArguments().size() == 1) {
                String path = extractPath(call.getArgument(0));
                return path == null ? null : new VerbAndPath(verb, path);
            }
            Optional<Expression> scope = call.getScope();
            if (scope.isEmpty()) {
                return null;
            }
            current = scope.get();
        }
        return null;
    }

    private String extractPath(Expression urlMatcherArgument) {
        if (!urlMatcherArgument.isMethodCallExpr()) {
            return null;
        }
        MethodCallExpr call = urlMatcherArgument.asMethodCallExpr();
        if (!URL_MATCHER_METHODS.contains(call.getNameAsString()) || call.getArguments().isEmpty()) {
            return null;
        }
        Expression first = call.getArgument(0);
        if (!first.isStringLiteralExpr()) {
            return null;
        }
        return PathTemplates.normalize(first.asStringLiteralExpr().asString());
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

    /** HTTP verb and path template resolved from a WireMock verb-builder call. */
    private record VerbAndPath(HttpVerb verb, String path) {}
}
