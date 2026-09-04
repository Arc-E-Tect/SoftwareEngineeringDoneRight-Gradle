package com.arc_e_tect.gradle.jacoco.scan;

import com.arc_e_tect.gradle.jacoco.model.ExcludedElement;
import com.arc_e_tect.gradle.jacoco.model.ExcludedElement.ElementType;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Scans a compiled {@code .class} file for members that JaCoCo automatically
 * excludes from coverage because they carry an annotation whose simple name
 * is {@code Generated} — the same convention JaCoCo itself relies on since
 * version 0.8.2.
 *
 * <p>This covers code synthesised by another tool that stamps its output with
 * such an annotation, most notably Lombok's {@code @lombok.Generated} on
 * every getter, setter, constructor, {@code equals}/{@code hashCode}/{@code
 * toString}, and builder method it generates. These members never exist in
 * the {@code .java} source, so — unlike {@link AnnotationScanner} — this
 * scanner reads bytecode rather than parsing source.</p>
 */
public class GeneratedAnnotationScanner {

    private static final String MARKER_SIMPLE_NAME = "Generated";

    /** Creates a new scanner instance. */
    public GeneratedAnnotationScanner() {
    }

    /**
     * Scans one {@code .class} file and returns every member carrying a
     * {@code Generated}-named annotation.
     *
     * @param classFile the compiled class file to inspect
     * @return possibly-empty list of excluded elements, never {@code null}
     * @throws IOException if the file cannot be read or is not a valid class file
     */
    public List<ExcludedElement> scan(File classFile) throws IOException {
        List<ExcludedElement> results = new ArrayList<>();
        try (InputStream in = new FileInputStream(classFile)) {
            ClassReader reader = new ClassReader(in);
            reader.accept(new CollectingVisitor(results), ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
        } catch (IllegalArgumentException e) {
            // Not a well-formed class file (e.g. module-info.class on older ASM) — ignore.
            return List.of();
        }
        return results;
    }

    // ── Bytecode-level annotation matching ──────────────────────────────────

    private static boolean isGeneratedAnnotation(String descriptor) {
        return MARKER_SIMPLE_NAME.equals(simpleNameFromDescriptor(descriptor));
    }

    private static String simpleNameFromDescriptor(String descriptor) {
        String fqcn = fqcnFromDescriptor(descriptor);
        int dot = fqcn.lastIndexOf('.');
        return dot >= 0 ? fqcn.substring(dot + 1) : fqcn;
    }

    private static String fqcnFromDescriptor(String descriptor) {
        String body = descriptor.startsWith("L") && descriptor.endsWith(";")
                ? descriptor.substring(1, descriptor.length() - 1)
                : descriptor;
        return body.replace('/', '.');
    }

    // ── ASM visitors ─────────────────────────────────────────────────────────

    private static final class CollectingVisitor extends ClassVisitor {

        private final List<ExcludedElement> results;
        private String packageName = "";
        private String className   = "";
        private String sourceFile  = "";

        CollectingVisitor(List<ExcludedElement> results) {
            super(Opcodes.ASM9);
            this.results = results;
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            int slash = name.lastIndexOf('/');
            packageName = slash >= 0 ? name.substring(0, slash).replace('/', '.') : "";
            String binaryTail = slash >= 0 ? name.substring(slash + 1) : name;
            className = binaryTail.replace('$', '.');
            sourceFile = binaryTail.contains("$")
                    ? binaryTail.substring(0, binaryTail.indexOf('$')) + ".java"
                    : binaryTail + ".java";
        }

        @Override
        public void visitSource(String source, String debug) {
            if (source != null) {
                sourceFile = source;
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (isGeneratedAnnotation(descriptor)) {
                results.add(new ExcludedElement(ElementType.CLASS, packageName, className, "",
                        0, sourceFile, "@" + fqcnFromDescriptor(descriptor)));
            }
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            if ((access & (Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE)) != 0
                    || "<clinit>".equals(name)) {
                return null;
            }

            boolean isConstructor = "<init>".equals(name);
            ElementType type = isConstructor ? ElementType.CONSTRUCTOR : ElementType.METHOD;
            String simpleClassName = className.contains(".")
                    ? className.substring(className.lastIndexOf('.') + 1) : className;
            String member = (isConstructor ? simpleClassName : name)
                    + "(" + parameterTypeNames(descriptor) + ")";

            return new MethodVisitor(Opcodes.ASM9) {
                private boolean matched       = false;
                private String  justification = "";

                @Override
                public AnnotationVisitor visitAnnotation(String d, boolean visible) {
                    if (!matched && isGeneratedAnnotation(d)) {
                        matched       = true;
                        justification = "@" + fqcnFromDescriptor(d);
                    }
                    return null;
                }

                @Override
                public void visitEnd() {
                    if (matched) {
                        results.add(new ExcludedElement(type, packageName, className, member,
                                0, sourceFile, justification));
                    }
                }
            };
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
                return null;
            }

            return new FieldVisitor(Opcodes.ASM9) {
                private boolean matched       = false;
                private String  justification = "";

                @Override
                public AnnotationVisitor visitAnnotation(String d, boolean visible) {
                    if (!matched && isGeneratedAnnotation(d)) {
                        matched       = true;
                        justification = "@" + fqcnFromDescriptor(d);
                    }
                    return null;
                }

                @Override
                public void visitEnd() {
                    if (matched) {
                        results.add(new ExcludedElement(ElementType.FIELD, packageName, className,
                                name, 0, sourceFile, justification));
                    }
                }
            };
        }

        private String parameterTypeNames(String methodDescriptor) {
            Type[] argTypes = Type.getArgumentTypes(methodDescriptor);
            return Stream.of(argTypes)
                    .map(this::readableTypeName)
                    .collect(Collectors.joining(", "));
        }

        private String readableTypeName(Type type) {
            String name = type.getClassName();
            int dot = name.lastIndexOf('.');
            return dot >= 0 ? name.substring(dot + 1) : name;
        }
    }
}
