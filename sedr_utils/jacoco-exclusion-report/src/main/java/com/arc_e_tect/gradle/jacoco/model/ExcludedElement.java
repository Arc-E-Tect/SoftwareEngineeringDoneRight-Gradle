package com.arc_e_tect.gradle.jacoco.model;

/**
 * Represents a single Java element that carries the exclusion annotation.
 */
public final class ExcludedElement {

    /** Classifies the kind of Java element that carries the exclusion annotation. */
    public enum ElementType {
        /** Annotation is placed on a class or interface declaration. */
        CLASS,
        /** Annotation is placed on a constructor declaration. */
        CONSTRUCTOR,
        /** Annotation is placed on a method declaration. */
        METHOD,
        /** Annotation is placed on a field declaration. */
        FIELD;

        /**
         * Human-readable label used in reports.
         *
         * @return title-cased element type name (e.g. {@code "Class"}, {@code "Method"})
         */
        public String label() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    private final ElementType type;
    private final String      packageName;
    private final String      className;
    /** Empty string for class-level exclusions; qualified signature for members. */
    private final String      member;
    private final int         lineNumber;
    private final String      sourceFile;
    /** Optional human-readable justification for the exclusion. Empty string when none was provided. */
    private final String      justification;

    /**
     * Full constructor including justification.
     *
     * @param type          element type (class, constructor, method, or field)
     * @param packageName   package of the declaring class, or empty for the default package
     * @param className     simple or nested class name
     * @param member        member signature; empty for class-level exclusions
     * @param lineNumber    source line number where the annotation appears
     * @param sourceFile    simple file name (e.g. {@code MyClass.java})
     * @param justification optional human-readable justification; {@code null} is normalised to {@code ""}
     */
    public ExcludedElement(ElementType type, String packageName, String className,
                           String member, int lineNumber, String sourceFile,
                           String justification) {
        this.type          = type;
        this.packageName   = packageName;
        this.className     = className;
        this.member        = member;
        this.lineNumber    = lineNumber;
        this.sourceFile    = sourceFile;
        this.justification = justification == null ? "" : justification;
    }

    /**
     * Backward-compatible constructor without justification (defaults to empty string).
     *
     * @param type        element type (class, constructor, method, or field)
     * @param packageName package of the declaring class, or empty for the default package
     * @param className   simple or nested class name
     * @param member      member signature; empty for class-level exclusions
     * @param lineNumber  source line number where the annotation appears
     * @param sourceFile  simple file name (e.g. {@code MyClass.java})
     */
    public ExcludedElement(ElementType type, String packageName, String className,
                           String member, int lineNumber, String sourceFile) {
        this(type, packageName, className, member, lineNumber, sourceFile, "");
    }

    /**
     * Returns the element type.
     *
     * @return element type (class, constructor, method, or field)
     */
    public ElementType getType()          { return type; }

    /**
     * Returns the package name of the declaring class.
     *
     * @return package name, or an empty string for the default package
     */
    public String      getPackageName()   { return packageName; }

    /**
     * Returns the simple or nested class name of the declaring class.
     *
     * @return simple class name, or a dot-separated nested name such as {@code Outer.Inner}
     */
    public String      getClassName()     { return className; }

    /**
     * Returns the member signature.
     *
     * @return empty string for class-level exclusions; qualified member signature for
     *         constructor, method, and field exclusions
     */
    public String      getMember()        { return member; }

    /**
     * Returns the source line number where the exclusion annotation appears.
     *
     * @return 1-based line number, or {@code 0} when the parser could not determine a position
     */
    public int         getLineNumber()    { return lineNumber; }

    /**
     * Returns the simple file name of the source file.
     *
     * @return simple file name, e.g. {@code MyClass.java}
     */
    public String      getSourceFile()    { return sourceFile; }

    /**
     * Returns the human-readable justification for the exclusion.
     *
     * @return the justification string, or an empty string when none was provided
     */
    public String      getJustification() { return justification; }

    /**
     * Fully-qualified class name.
     *
     * @return fully-qualified class name, e.g. {@code com.example.MyClass}
     */
    public String getFqcn() {
        return packageName.isEmpty() ? className : packageName + "." + className;
    }

    /**
     * Display name used as the test-case {@code name} attribute in XML.
     *
     * @return a bracket-prefixed label such as {@code "[method] myMethod()"}
     */
    public String displayName() {
        return switch (type) {
            case CLASS       -> "[class] " + className;
            case CONSTRUCTOR -> "[constructor] " + member;
            case METHOD      -> "[method] " + member;
            case FIELD       -> "[field] " + member;
        };
    }

    @Override
    public String toString() {
        return "ExcludedElement{type=" + type + ", fqcn=" + getFqcn()
               + ", member='" + member + "', line=" + lineNumber + "}";
    }
}
