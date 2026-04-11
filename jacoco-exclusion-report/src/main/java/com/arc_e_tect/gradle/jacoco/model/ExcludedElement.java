package com.arc_e_tect.gradle.jacoco.model;

/**
 * Represents a single Java element that carries the exclusion annotation.
 */
public final class ExcludedElement {

    public enum ElementType {
        CLASS, CONSTRUCTOR, METHOD, FIELD;

        /** Human-readable label used in reports. */
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

    /** Full constructor including justification. */
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

    /** Backward-compatible constructor without justification (defaults to empty). */
    public ExcludedElement(ElementType type, String packageName, String className,
                           String member, int lineNumber, String sourceFile) {
        this(type, packageName, className, member, lineNumber, sourceFile, "");
    }

    public ElementType getType()          { return type; }
    public String      getPackageName()   { return packageName; }
    public String      getClassName()     { return className; }
    public String      getMember()        { return member; }
    public int         getLineNumber()    { return lineNumber; }
    public String      getSourceFile()    { return sourceFile; }
    public String      getJustification() { return justification; }

    /** Fully-qualified class name. */
    public String getFqcn() {
        return packageName.isEmpty() ? className : packageName + "." + className;
    }

    /** Display name used as the test-case {@code name} attribute in XML. */
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
