package com.arc_e_tect.gradle.jacoco.report;

import com.arc_e_tect.gradle.jacoco.model.ExcludedElement;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Writes a Surefire-compatible XML report so CI tools (Jenkins, GitLab, GitHub
 * Actions test-reporter) can display exclusions alongside normal test results.
 *
 * <p>Each excluded element becomes one {@code <testcase>} with {@code time="0"}.
 * The suite never has failures — its purpose is documentation, not assertion.</p>
 */
public class XmlReportWriter {

    public void write(List<ExcludedElement> elements, String annotationName,
                      File outputDir) throws IOException {
        outputDir.mkdirs();
        File xml = new File(outputDir, "jacoco-exclusions.xml");

        // Group per package so we can emit one <testsuite> per package
        Map<String, List<ExcludedElement>> byPackage = elements.stream()
                .collect(Collectors.groupingBy(
                        ExcludedElement::getPackageName, TreeMap::new, Collectors.toList()));

        try (PrintWriter w = new PrintWriter(xml, StandardCharsets.UTF_8)) {
            w.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            w.println("<!-- JaCoCo Exclusion Report – annotation: @" + annotationName + " -->");
            w.println("<testsuites name=\"JacocoExclusions\" tests=\"" + elements.size()
                      + "\" failures=\"0\" errors=\"0\" skipped=\"0\">");

            if (byPackage.isEmpty()) {
                w.println("  <testsuite name=\"(no exclusions)\" tests=\"0\"/>");
            } else {
                byPackage.forEach((pkg, elems) -> {
                    String suiteName = pkg.isEmpty() ? "(default)" : pkg;
                    w.println("  <testsuite name=\"" + esc(suiteName) + "\" tests=\""
                              + elems.size() + "\" failures=\"0\" errors=\"0\" skipped=\"0\""
                              + " timestamp=\"" + Instant.now() + "\">");
                    elems.forEach(e -> {
                        String justAttr = e.getJustification().isEmpty() ? "" :
                                " justification=\"" + esc(e.getJustification()) + "\"";
                        w.println("    <testcase classname=\"" + esc(e.getFqcn())
                                  + "\" name=\"" + esc(e.displayName())
                                  + " \u2013 line " + e.getLineNumber()
                                  + "\" time=\"0\"" + justAttr + "/>");
                    });
                    w.println("  </testsuite>");
                });
            }

            w.println("</testsuites>");
        }
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
