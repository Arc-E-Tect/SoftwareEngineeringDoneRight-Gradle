// Intentionally in the default package (no package declaration).
// Used by AnnotationScannerTest to exercise the n.endsWith("." + simpleName) branch
// in both hasAnnotation() and extractJustification().
public class FqAnnotationFixture {

    @com.example.ExcludeFromJacocoGeneratedCodeCoverage(justification = "FQ annotation match")
    public void annotatedWithFqName() {
    }
}
