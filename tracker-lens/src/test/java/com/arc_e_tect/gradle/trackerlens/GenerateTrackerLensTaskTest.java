package com.arc_e_tect.gradle.trackerlens;

import com.arc_e_tect.gradle.trackerlens.contract.LensContractValidator;
import com.arc_e_tect.gradle.trackerlens.contract.Violation;
import org.gradle.api.GradleException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GenerateTrackerLensTask")
class GenerateTrackerLensTaskTest {

    @TempDir
    Path tempDir;

    private final Instant now = Instant.now();

    @Test
    @DisplayName("generateShouldProduceContractConformantDashboardWithBuiltInAndExternalLensesMerged")
    void generateShouldProduceContractConformantDashboardWithBuiltInAndExternalLensesMerged() throws Exception {
        GenerateTrackerLensTask task = newTask();
        writeGherkinHistory(tempDir.resolve("gherkin.ndjson"));
        writeApiContractHistory(tempDir.resolve("api.ndjson"));

        task.getTrackerSpecs().set(List.of(
                new TrackerSpec("bdd-scenarios", TrackerSourceKind.GHERKIN_SCENARIO, List.of(tempDir.resolve("gherkin.ndjson").toFile())),
                new TrackerSpec("api-contracts", TrackerSourceKind.API_CONTRACT, List.of(tempDir.resolve("api.ndjson").toFile()))));

        File jarFile = writeFixtureStylePackJar("sunrise-theme.jar");
        task.getLensStyleClasspath().from(jarFile);
        File outputDir = tempDir.resolve("out").toFile();
        task.getOutputDirectory().set(outputDir);

        task.generate();

        File dashboardFile = new File(outputDir, "dashboard.html");
        List<Violation> violations = new LensContractValidator().validate(dashboardFile);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("generateShouldWriteOneCssFilePerDiscoveredLensIncludingAutoNamespacedCollision")
    void generateShouldWriteOneCssFilePerDiscoveredLensIncludingAutoNamespacedCollision() throws Exception {
        GenerateTrackerLensTask task = newTask();
        writeGherkinHistory(tempDir.resolve("gherkin.ndjson"));
        task.getTrackerSpecs().set(List.of(
                new TrackerSpec("bdd-scenarios", TrackerSourceKind.GHERKIN_SCENARIO, List.of(tempDir.resolve("gherkin.ndjson").toFile()))));
        File jarFile = writeFixtureStylePackJar("sunrise-theme.jar");
        task.getLensStyleClasspath().from(jarFile);
        File outputDir = tempDir.resolve("out-css").toFile();
        task.getOutputDirectory().set(outputDir);

        task.generate();

        assertThat(outputDir.list()).contains(
                "light-lens.css", "dark-lens.css", "high-contrast-lens.css", "dark-lens-sunrise-theme.css", "sunrise.css");
    }

    @Test
    @DisplayName("generateShouldReportLensSwitcherCountMatchingAllDiscoveredLenses")
    void generateShouldReportLensSwitcherCountMatchingAllDiscoveredLenses() throws Exception {
        GenerateTrackerLensTask task = newTask();
        writeGherkinHistory(tempDir.resolve("gherkin.ndjson"));
        task.getTrackerSpecs().set(List.of(
                new TrackerSpec("bdd-scenarios", TrackerSourceKind.GHERKIN_SCENARIO, List.of(tempDir.resolve("gherkin.ndjson").toFile()))));
        File jarFile = writeFixtureStylePackJar("sunrise-theme.jar");
        task.getLensStyleClasspath().from(jarFile);
        File outputDir = tempDir.resolve("out-switcher").toFile();
        task.getOutputDirectory().set(outputDir);

        task.generate();

        String html = Files.readString(new File(outputDir, "dashboard.html").toPath());
        assertThat(html).contains("data-lens-count=\"5\"");
    }

    @Test
    @DisplayName("generateShouldSkipTrackerWithMissingHistoryFileWithoutFailing")
    void generateShouldSkipTrackerWithMissingHistoryFileWithoutFailing() {
        GenerateTrackerLensTask task = newTask();
        task.getTrackerSpecs().set(List.of(
                new TrackerSpec("bdd-scenarios", TrackerSourceKind.GHERKIN_SCENARIO, List.of(tempDir.resolve("missing.ndjson").toFile()))));
        File outputDir = tempDir.resolve("out-missing").toFile();
        task.getOutputDirectory().set(outputDir);

        task.generate();

        assertThat(new File(outputDir, "dashboard.html")).exists();
    }

    @Test
    @DisplayName("generateShouldGiveLensStylesheetHighestPrecedenceForCustomLensId")
    void generateShouldGiveLensStylesheetHighestPrecedenceForCustomLensId() throws Exception {
        GenerateTrackerLensTask task = newTask();
        writeGherkinHistory(tempDir.resolve("gherkin.ndjson"));
        task.getTrackerSpecs().set(List.of(
                new TrackerSpec("bdd-scenarios", TrackerSourceKind.GHERKIN_SCENARIO, List.of(tempDir.resolve("gherkin.ndjson").toFile()))));
        Path stylesheet = tempDir.resolve("my-theme.css");
        Files.writeString(stylesheet, ".dashboard { color: teal; }");
        task.getLensStylesheet().set(stylesheet.toFile());
        File outputDir = tempDir.resolve("out-custom").toFile();
        task.getOutputDirectory().set(outputDir);

        task.generate();

        assertThat(outputDir.list()).contains("custom-lens.css");
    }

    @Test
    @DisplayName("generateShouldRenderWithACustomTemplateWhenConfigured")
    void generateShouldRenderWithACustomTemplateWhenConfigured() throws Exception {
        GenerateTrackerLensTask task = newTask();
        writeGherkinHistory(tempDir.resolve("gherkin.ndjson"));
        task.getTrackerSpecs().set(List.of(
                new TrackerSpec("bdd-scenarios", TrackerSourceKind.GHERKIN_SCENARIO, List.of(tempDir.resolve("gherkin.ndjson").toFile()))));
        Path template = tempDir.resolve("dashboard-template.html");
        Files.writeString(template, """
                <!doctype html>
                <html><head><link rel="stylesheet" id="lens-stylesheet" href="{{defaultLensCssFile}}"></head>
                <body>
                <div class="dashboard">
                <h1>Tableau de bord Tracker Lens</h1>
                <div class="lens-switcher" data-lens-count="{{lensCount}}"><select></select></div>
                {{#trackers}}
                <section class="tracker" data-tracker="{{id}}">
                {{#metrics}}<div class="metric-card" data-stage="{{stage}}" style="--percent: {{percent}}"></div>{{/metrics}}
                <div class="chart"><canvas></canvas></div>
                </section>
                {{/trackers}}
                </div>
                {{{dashboardDataScript}}}
                </body></html>
                """);
        task.getTemplate().set(template.toFile());
        File outputDir = tempDir.resolve("out-template").toFile();
        task.getOutputDirectory().set(outputDir);

        task.generate();

        String html = Files.readString(new File(outputDir, "dashboard.html").toPath());
        assertThat(html).contains("Tableau de bord Tracker Lens");
    }

    @Test
    @DisplayName("generateShouldFailWhenLensStylesheetCannotBeRead")
    void generateShouldFailWhenLensStylesheetCannotBeRead() {
        GenerateTrackerLensTask task = newTask();
        task.getTrackerSpecs().set(List.of(
                new TrackerSpec("bdd-scenarios", TrackerSourceKind.GHERKIN_SCENARIO, List.of(tempDir.resolve("missing.ndjson").toFile()))));
        Path notAFile = tempDir.resolve("not-a-file-dir");
        notAFile.toFile().mkdirs();
        task.getLensStylesheet().set(notAFile.toFile());
        task.getOutputDirectory().set(tempDir.resolve("out-bad-stylesheet").toFile());

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("lensStylesheet");
    }

    @Test
    @DisplayName("generateShouldRestrictExternalLensesToPreferredLensPackWhenConfigured")
    void generateShouldRestrictExternalLensesToPreferredLensPackWhenConfigured() throws Exception {
        GenerateTrackerLensTask task = newTask();
        writeGherkinHistory(tempDir.resolve("gherkin.ndjson"));
        task.getTrackerSpecs().set(List.of(
                new TrackerSpec("bdd-scenarios", TrackerSourceKind.GHERKIN_SCENARIO, List.of(tempDir.resolve("gherkin.ndjson").toFile()))));
        File preferredJar = writeFixtureStylePackJar("sunrise-theme.jar");
        File otherJar = writeSecondStylePackJar("midnight-theme.jar");
        task.getLensStyleClasspath().from(preferredJar, otherJar);
        task.getPreferredLensPack().set("com.example:sunrise-theme");
        File outputDir = tempDir.resolve("out-preferred").toFile();
        task.getOutputDirectory().set(outputDir);

        task.generate();

        assertThat(outputDir.list()).doesNotContain("midnight.css");
    }

    @Test
    @DisplayName("resolveDefaultLensIdShouldFallBackToAlphabeticalFirstWhenLightLensNotDiscoveredAndUnset")
    void resolveDefaultLensIdShouldFallBackToAlphabeticalFirstWhenLightLensNotDiscoveredAndUnset() {
        GenerateTrackerLensTask task = newTask();
        List<com.arc_e_tect.gradle.trackerlens.lens.ResolvedLens> lenses = List.of(
                new com.arc_e_tect.gradle.trackerlens.lens.ResolvedLens("zeta", "built-in", new byte[0]),
                new com.arc_e_tect.gradle.trackerlens.lens.ResolvedLens("alpha", "built-in", new byte[0]));

        String defaultLensId = task.resolveDefaultLensId(lenses);

        assertThat(defaultLensId).isEqualTo("alpha");
    }

    @Test
    @DisplayName("resolveDefaultLensIdShouldFailWhenNoLensesWereDiscoveredAndUnset")
    void resolveDefaultLensIdShouldFailWhenNoLensesWereDiscoveredAndUnset() {
        GenerateTrackerLensTask task = newTask();

        assertThatThrownBy(() -> task.resolveDefaultLensId(List.of()))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("no lenses were discovered");
    }

    @Test
    @DisplayName("generateShouldMergeSameEndpointAcrossSeparateHistoryFilesInsteadOfDuplicatingIt")
    void generateShouldMergeSameEndpointAcrossSeparateHistoryFilesInsteadOfDuplicatingIt() throws Exception {
        GenerateTrackerLensTask task = newTask();
        Instant declared = now.minus(Duration.ofDays(10));
        Instant implemented = now.minus(Duration.ofDays(5));
        Path shadowHistory = tempDir.resolve("shadow-api-detector-contract-history.ndjson");
        Files.writeString(shadowHistory,
                "{\"fingerprint\":\"ep1\",\"verb\":\"GET\",\"path\":\"/orders\",\"declaringClass\":null,"
                + "\"declaredAt\":\"" + declared + "\",\"implementedAt\":null,\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"" + declared + "\",\"removedAt\":null}\n");
        Path mirageHistory = tempDir.resolve("mirage-api-detector-contract-history.ndjson");
        Files.writeString(mirageHistory,
                "{\"fingerprint\":\"ep1\",\"verb\":\"GET\",\"path\":\"/orders\",\"declaringClass\":\"com.example.OrderController\","
                + "\"declaredAt\":null,\"implementedAt\":\"" + implemented + "\",\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"" + implemented + "\",\"removedAt\":null}\n");

        task.getTrackerSpecs().set(List.of(new TrackerSpec(
                "api-contracts", TrackerSourceKind.API_CONTRACT,
                List.of(shadowHistory.toFile(), mirageHistory.toFile()))));
        File outputDir = tempDir.resolve("out-merge").toFile();
        task.getOutputDirectory().set(outputDir);

        task.generate();

        org.jsoup.nodes.Document document = org.jsoup.Jsoup.parse(new File(outputDir, "dashboard.html"), "UTF-8");
        assertThat(document.select(".metric-card[data-stage=\"declared\"] .metric-card__count").text())
                .isEqualTo("1 / 1");
    }

    @Test
    @DisplayName("generateShouldExpandDirectoryConfiguredAsHistoryFilesEntryToItsNdjsonChildren")
    void generateShouldExpandDirectoryConfiguredAsHistoryFilesEntryToItsNdjsonChildren() throws Exception {
        GenerateTrackerLensTask task = newTask();
        Path historyDir = tempDir.resolve("gherkin-histories");
        Files.createDirectories(historyDir);
        Files.writeString(historyDir.resolve("area-one.ndjson"),
                "{\"fingerprint\":\"g1\",\"scenarioName\":\"Place an order\",\"featureTitle\":\"Ordering\","
                + "\"listedAt\":\"" + now + "\",\"definedAt\":null,\"implementedAt\":null,"
                + "\"lastSeenAt\":\"" + now + "\",\"removedAt\":null}\n");
        Files.writeString(historyDir.resolve("area-two.ndjson"),
                "{\"fingerprint\":\"g2\",\"scenarioName\":\"Cancel an order\",\"featureTitle\":\"Ordering\","
                + "\"listedAt\":\"" + now + "\",\"definedAt\":null,\"implementedAt\":null,"
                + "\"lastSeenAt\":\"" + now + "\",\"removedAt\":null}\n");

        task.getTrackerSpecs().set(List.of(new TrackerSpec(
                "bdd-scenarios", TrackerSourceKind.GHERKIN_SCENARIO, List.of(historyDir.toFile()))));
        File outputDir = tempDir.resolve("out-dir").toFile();
        task.getOutputDirectory().set(outputDir);

        task.generate();

        org.jsoup.nodes.Document document = org.jsoup.Jsoup.parse(new File(outputDir, "dashboard.html"), "UTF-8");
        assertThat(document.select(".metric-card[data-stage=\"listed\"] .metric-card__count").text())
                .isEqualTo("2 / 2");
    }

    private GenerateTrackerLensTask newTask() {
        return ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("generateTrackerLensDashboard", GenerateTrackerLensTask.class);
    }

    private void writeGherkinHistory(Path file) throws Exception {
        Instant listed = now.minus(Duration.ofDays(20));
        Instant defined = now.minus(Duration.ofDays(15));
        Instant implemented = now.minus(Duration.ofDays(10));
        Files.writeString(file,
                "{\"fingerprint\":\"g1\",\"scenarioName\":\"Place an order\",\"featureTitle\":\"Ordering\","
                + "\"listedAt\":\"" + listed + "\",\"definedAt\":\"" + defined + "\",\"implementedAt\":\"" + implemented
                + "\",\"lastSeenAt\":\"" + now + "\",\"removedAt\":null}\n"
                + "{\"fingerprint\":\"g2\",\"scenarioName\":\"Cancel an order\",\"featureTitle\":\"Ordering\","
                + "\"listedAt\":\"" + listed + "\",\"definedAt\":null,\"implementedAt\":null,"
                + "\"lastSeenAt\":\"" + now + "\",\"removedAt\":null}\n");
    }

    private void writeApiContractHistory(Path file) throws Exception {
        Instant declared = now.minus(Duration.ofDays(5));
        Files.writeString(file,
                "{\"fingerprint\":\"a1\",\"verb\":\"GET\",\"path\":\"/orders\",\"declaringClass\":\"com.example.OrderController\","
                + "\"declaredAt\":\"" + declared + "\",\"implementedAt\":null,\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"" + now + "\",\"removedAt\":null}\n");
    }

    private File writeFixtureStylePackJar(String fileName) throws Exception {
        Path jarFile = tempDir.resolve(fileName);
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
            jar.putNextEntry(new JarEntry("META-INF/arc-e-tect/tracker-lens/lenses/dark-lens.css"));
            jar.write(".dashboard { color: purple; }".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("META-INF/arc-e-tect/tracker-lens/lenses/sunrise.css"));
            jar.write(".dashboard { color: orange; }".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return jarFile.toFile();
    }

    private File writeSecondStylePackJar(String fileName) throws Exception {
        Path jarFile = tempDir.resolve(fileName);
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
            jar.putNextEntry(new JarEntry("META-INF/arc-e-tect/tracker-lens/lenses/midnight.css"));
            jar.write(".dashboard { color: navy; }".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return jarFile.toFile();
    }
}
