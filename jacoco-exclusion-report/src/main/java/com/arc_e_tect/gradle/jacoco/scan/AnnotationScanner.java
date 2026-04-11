package com.arc_e_tect.gradle.jacoco.scan;

import com.arc_e_tect.gradle.jacoco.model.ExcludedElement;
import com.arc_e_tect.gradle.jacoco.model.ExcludedElement.ElementType;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parses a single Java source file and collects every element that carries
 * the configured exclusion annotation.
 *
 * <p>Handles classes, constructors, methods, and fields, including in nested
 * classes.  Anonymous classes are ignored because they cannot bear annotations
 * on their declaration.</p>
 */
public class AnnotationScanner {

    private final String annotationSimpleName;

    public AnnotationScanner(String annotationSimpleName) {
        this.annotationSimpleName = annotationSimpleName;
    }

    /**
     * Scans one {@code .java} source file and returns every annotated element.
     *
     * @param sourceFile the file to parse
     * @return possibly-empty list of excluded elements, never {@code null}
     * @throws IOException if the file cannot be read
     */
    public List<ExcludedElement> scan(File sourceFile) throws IOException {
        List<ExcludedElement> results = new ArrayList<>();

        ParseResult<CompilationUnit> parseResult = new JavaParser().parse(sourceFile);
        if (!parseResult.isSuccessful() || parseResult.getResult().isEmpty()) {
            return results;
        }

        CompilationUnit cu    = parseResult.getResult().get();
        String          pkg   = cu.getPackageDeclaration()
                                  .map(pd -> pd.getNameAsString())
                                  .orElse("");
        String          fname = sourceFile.getName();

        // findAll traverses the entire AST, so we pick up nested classes too.
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
            String className = buildClassName(cls);

            // ── Class-level annotation ────────────────────────────────────
            if (hasAnnotation(cls, annotationSimpleName)) {
                results.add(new ExcludedElement(
                        ElementType.CLASS, pkg, className, "",
                        cls.getBegin().map(p -> p.line).orElse(0), fname,
                        extractJustification(cls, annotationSimpleName)));
            }

            // ── Constructor annotations ───────────────────────────────────
            cls.getConstructors().forEach(ctor -> {
                if (hasAnnotation(ctor, annotationSimpleName)) {
                    String sig = ctor.getNameAsString() + "("
                                 + buildParams(ctor.getParameters()) + ")";
                    results.add(new ExcludedElement(
                            ElementType.CONSTRUCTOR, pkg, className, sig,
                            ctor.getBegin().map(p -> p.line).orElse(0), fname,
                            extractJustification(ctor, annotationSimpleName)));
                }
            });

            // ── Method annotations ────────────────────────────────────────
            cls.getMethods().forEach(method -> {
                if (hasAnnotation(method, annotationSimpleName)) {
                    String sig = method.getNameAsString() + "("
                                 + buildParams(method.getParameters()) + ")";
                    results.add(new ExcludedElement(
                            ElementType.METHOD, pkg, className, sig,
                            method.getBegin().map(p -> p.line).orElse(0), fname,
                            extractJustification(method, annotationSimpleName)));
                }
            });

            // ── Field annotations ─────────────────────────────────────────
            cls.getFields().forEach(field -> {
                if (hasAnnotation(field, annotationSimpleName)) {
                    String just = extractJustification(field, annotationSimpleName);
                    field.getVariables().forEach(var -> results.add(new ExcludedElement(
                            ElementType.FIELD, pkg, className,
                            var.getNameAsString(),
                            field.getBegin().map(p -> p.line).orElse(0), fname, just)));
                }
            });
        });

        return results;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean hasAnnotation(NodeWithAnnotations<?> node, String simpleName) {
        return node.getAnnotations().stream()
                   .anyMatch(a -> {
                       String n = a.getNameAsString();
                       // Match by simple name or fully-qualified tail
                       return n.equals(simpleName) || n.endsWith("." + simpleName);
                   });
    }

    /**
     * Returns the {@code justification} attribute value of the exclusion
     * annotation on {@code node}, or an empty string when the annotation is a
     * plain marker or when the element is not annotated at all.
     */
    private String extractJustification(NodeWithAnnotations<?> node, String simpleName) {
        return node.getAnnotations().stream()
                .filter(a -> {
                    String n = a.getNameAsString();
                    return n.equals(simpleName) || n.endsWith("." + simpleName);
                })
                .findFirst()
                .map(a -> {
                    if (a instanceof NormalAnnotationExpr normal) {
                        return normal.getPairs().stream()
                                .filter(p -> p.getNameAsString().equals("justification"))
                                .findFirst()
                                .map(p -> {
                                    String raw = p.getValue().toString();
                                    // Strip surrounding quotes from string literals
                                    if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
                                        return raw.substring(1, raw.length() - 1);
                                    }
                                    return raw;
                                })
                                .orElse("");
                    }
                    return "";
                })
                .orElse("");
    }

    /**
     * Builds a nested-class name like {@code Outer.Inner} by walking up the AST.
     */
    private String buildClassName(ClassOrInterfaceDeclaration cls) {
        if (cls.getParentNode().isPresent()
                && cls.getParentNode().get() instanceof ClassOrInterfaceDeclaration parent) {
            return buildClassName(parent) + "." + cls.getNameAsString();
        }
        return cls.getNameAsString();
    }

    private String buildParams(NodeList<Parameter> params) {
        return params.stream()
                     .map(p -> p.getType().asString())
                     .collect(Collectors.joining(", "));
    }
}
