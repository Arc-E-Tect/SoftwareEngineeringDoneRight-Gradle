package com.arc_e_tect.gradle.jacoco.report;

import com.arc_e_tect.gradle.jacoco.model.ExcludedElement;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Writes an HTML report styled after Gradle's built-in test report.
 *
 * <p>The report includes:</p>
 * <ul>
 *   <li>A summary bar (total exclusions, grouped by element type)</li>
 *   <li>A table grouped by package → class → member</li>
 * </ul>
 */
public class HtmlReportWriter {

    public void write(List<ExcludedElement> elements, String annotationName,
                      File outputDir) throws IOException {
        outputDir.mkdirs();
        File html = new File(outputDir, "index.html");

        try (PrintWriter w = new PrintWriter(html, StandardCharsets.UTF_8)) {
            w.println(buildPage(elements, annotationName));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private String buildPage(List<ExcludedElement> elements, String annotationName) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        long classes      = count(elements, ExcludedElement.ElementType.CLASS);
        long constructors = count(elements, ExcludedElement.ElementType.CONSTRUCTOR);
        long methods      = count(elements, ExcludedElement.ElementType.METHOD);
        long fields       = count(elements, ExcludedElement.ElementType.FIELD);

        // Group by package → FQCN → elements
        Map<String, Map<String, List<ExcludedElement>>> byPackage =
                elements.stream()
                        .collect(Collectors.groupingBy(
                                ExcludedElement::getPackageName,
                                TreeMap::new,
                                Collectors.groupingBy(
                                        ExcludedElement::getFqcn,
                                        TreeMap::new,
                                        Collectors.toList())));

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
          .append("<meta charset=\"UTF-8\">\n")
          .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
          .append("<title>JaCoCo Exclusion Report</title>\n")
          .append(STYLE)
          .append("</head>\n<body>\n");

        // ── Header ──────────────────────────────────────────────────────────
        sb.append("<header>\n")
          .append("  <h1>JaCoCo Exclusion Report</h1>\n")
          .append("  <p class=\"subtitle\">Annotation: <code>@").append(esc(annotationName))
          .append("</code></p>\n")
          .append("  <p class=\"timestamp\">Generated: ").append(timestamp).append("</p>\n")
          .append("</header>\n");

        // ── Summary bar ──────────────────────────────────────────────────────
        sb.append("<section class=\"summary\">\n")
          .append(summaryBox(elements.size(), "Total", "total"))
          .append(summaryBox(classes,          "Classes",      "class-col"))
          .append(summaryBox(constructors,     "Constructors", "ctor-col"))
          .append(summaryBox(methods,          "Methods",      "method-col"))
          .append(summaryBox(fields,           "Fields",       "field-col"))
          .append("</section>\n");

        // ── Content ──────────────────────────────────────────────────────────
        if (elements.isEmpty()) {
            sb.append("<p class=\"empty\">No exclusions found.</p>\n");
        } else {
            sb.append("<section class=\"content\">\n");
            byPackage.forEach((pkg, classes2) -> {
                String pkgLabel = pkg.isEmpty() ? "(default package)" : pkg;
                sb.append("<div class=\"package\">\n")
                  .append("  <h2>").append(esc(pkgLabel)).append("</h2>\n");

                classes2.forEach((fqcn, elems) -> {
                    String simpleCls = fqcn.contains(".")
                            ? fqcn.substring(fqcn.lastIndexOf('.') + 1) : fqcn;
                    sb.append("  <div class=\"class-block\">\n")
                      .append("    <h3>").append(esc(simpleCls)).append("</h3>\n")
                      .append("    <table>\n")
                      .append("      <thead><tr>")
                      .append("<th>Type</th><th>Member</th><th>Justification</th><th>Line</th><th>File</th>")
                      .append("</tr></thead>\n")
                      .append("      <tbody>\n");

                    elems.forEach(e -> {
                        String typeClass = e.getType().name().toLowerCase().replace("_", "-");
                        sb.append("        <tr class=\"row-").append(typeClass).append("\">\n")
                          .append("          <td><span class=\"badge badge-").append(typeClass)
                          .append("\">").append(esc(e.getType().label())).append("</span></td>\n")
                          .append("          <td class=\"mono\">")
                          .append(e.getMember().isEmpty() ? "<em>\u2014</em>" : esc(e.getMember()))
                          .append("</td>\n")
                          .append("          <td class=\"justification\">")
                          .append(e.getJustification().isEmpty() ? "<em>\u2014</em>" : esc(e.getJustification()))
                          .append("</td>\n")
                          .append("          <td class=\"line\">").append(e.getLineNumber())
                          .append("</td>\n")
                          .append("          <td class=\"mono\">").append(esc(e.getSourceFile()))
                          .append("</td>\n")
                          .append("        </tr>\n");
                    });

                    sb.append("      </tbody>\n    </table>\n  </div>\n");
                });

                sb.append("</div>\n");
            });
            sb.append("</section>\n");
        }

        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    private long count(List<ExcludedElement> elements, ExcludedElement.ElementType type) {
        return elements.stream().filter(e -> e.getType() == type).count();
    }

    private String summaryBox(long count, String label, String cssClass) {
        return "  <div class=\"summary-box " + cssClass + "\">\n"
               + "    <span class=\"count\">" + count + "</span>\n"
               + "    <span class=\"label\">" + esc(label) + "</span>\n"
               + "  </div>\n";
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ── Embedded CSS ─────────────────────────────────────────────────────────

    private static final String STYLE = """
            <style>
            *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
            body   { font-family: "Segoe UI", Roboto, Arial, sans-serif; font-size: 14px;
                     background: #f4f4f4; color: #333; }
            header { background: #1e2a3a; color: #fff; padding: 20px 30px; }
            header h1      { font-size: 22px; margin-bottom: 4px; }
            .subtitle      { font-size: 13px; opacity: 0.8; margin-bottom: 4px; }
            .timestamp     { font-size: 12px; opacity: 0.6; }
            code           { background: rgba(255,255,255,.15); padding: 1px 5px;
                             border-radius: 3px; }

            /* Summary bar */
            .summary { display: flex; gap: 12px; padding: 16px 30px;
                       background: #fff; border-bottom: 1px solid #ddd; flex-wrap: wrap; }
            .summary-box { display: flex; flex-direction: column; align-items: center;
                           padding: 10px 20px; border-radius: 6px; min-width: 90px; }
            .summary-box .count { font-size: 26px; font-weight: 700; }
            .summary-box .label { font-size: 11px; text-transform: uppercase;
                                  letter-spacing: .5px; margin-top: 2px; }
            .total       { background: #e8f0fe; color: #1a56bb; }
            .class-col   { background: #fce8ff; color: #7b1fa2; }
            .ctor-col    { background: #e8f5e9; color: #2e7d32; }
            .method-col  { background: #fff3e0; color: #e65100; }
            .field-col   { background: #e3f2fd; color: #0d47a1; }

            /* Content */
            .content { padding: 20px 30px; }
            .empty   { padding: 30px; text-align: center; color: #888; font-size: 15px; }
            .package { margin-bottom: 28px; }
            .package h2 { font-size: 16px; color: #555; border-bottom: 2px solid #ddd;
                          padding-bottom: 4px; margin-bottom: 14px; }
            .class-block { margin-bottom: 18px; }
            .class-block h3 { font-size: 14px; font-weight: 600; margin-bottom: 6px;
                              color: #1e2a3a; }
            table  { width: 100%; border-collapse: collapse; background: #fff;
                     box-shadow: 0 1px 3px rgba(0,0,0,.1); border-radius: 6px;
                     overflow: hidden; }
            thead tr { background: #1e2a3a; color: #fff; }
            th, td { padding: 9px 13px; text-align: left; }
            th { font-size: 12px; text-transform: uppercase; letter-spacing: .4px; }
            tbody tr:nth-child(even) { background: #f9f9f9; }
            tbody tr:hover           { background: #eef4ff; }
            .mono { font-family: "Fira Code", "Consolas", monospace; font-size: 12px; }
            .line { color: #888; font-size: 12px; text-align: right; width: 60px; }

            /* Type badges */
            .badge { display: inline-block; padding: 2px 8px; border-radius: 10px;
                     font-size: 11px; font-weight: 600; text-transform: uppercase; }
            .badge-class       { background: #ede7f6; color: #4527a0; }
            .badge-constructor { background: #e8f5e9; color: #1b5e20; }
            .badge-method      { background: #fff8e1; color: #f57f17; }
            .badge-field       { background: #e3f2fd; color: #0d47a1; }
            .justification     { font-style: italic; color: #555; max-width: 280px;
                                 white-space: normal; word-break: break-word; }
            </style>
            """;
}
