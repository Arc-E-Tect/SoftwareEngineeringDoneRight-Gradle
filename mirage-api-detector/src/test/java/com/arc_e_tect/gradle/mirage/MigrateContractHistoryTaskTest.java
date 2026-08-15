package com.arc_e_tect.gradle.mirage;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.progress.ContractHistoryStore;
import com.arc_e_tect.gradle.detector.core.progress.ContractProgressRecord;
import com.arc_e_tect.gradle.detector.core.progress.EndpointFingerprint;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MigrateContractHistoryTask")
class MigrateContractHistoryTaskTest {

    private static final EndpointFingerprint FINGERPRINTER = new EndpointFingerprint();

    @TempDir
    Path tempDir;

    private Project project;
    private File controllerDir;
    private File stubDir;

    @BeforeEach
    void setUp() throws Exception {
        project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();

        controllerDir = new File(tempDir.toFile(), "src/main/java/com/example");
        Files.createDirectories(controllerDir.toPath());
        Files.writeString(controllerDir.toPath().resolve("RealOnlyController.java"), """
                package com.example;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class RealOnlyController {

                    @GetMapping("/real-only")
                    public String realOnly() { return ""; }

                    @GetMapping("/both")
                    public String both() { return ""; }
                }
                """);

        stubDir = new File(tempDir.toFile(), "src/test/resources/mappings");
        Files.createDirectories(stubDir.toPath());
        Files.writeString(stubDir.toPath().resolve("stubOnly.json"), """
                { "request": { "method": "GET", "urlPath": "/stub-only" }, "response": { "status": 200 } }
                """);
        Files.writeString(stubDir.toPath().resolve("both.json"), """
                { "request": { "method": "GET", "urlPath": "/both" }, "response": { "status": 200 } }
                """);
    }

    @Test
    @DisplayName("throws when contractHistoryFile does not exist")
    void throwsWhenHistoryFileDoesNotExist() {
        MigrateContractHistoryTask task = newTask(new File(tempDir.toFile(), "missing.ndjson"));

        assertThatThrownBy(task::migrate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    @DisplayName("a record matching only a real controller keeps implementedAt and gets no stubbedAt")
    void realOnlyMatchKeepsImplementedAtNoStubbedAt() throws Exception {
        Instant oldImplementedAt = Instant.parse("2026-01-10T00:00:00Z");
        File historyFile = writeLegacyHistory(legacyLine("/real-only", oldImplementedAt, null));

        MigrateContractHistoryTask task = newTask(historyFile);
        task.migrate();

        ContractProgressRecord migrated = loadMigrated(historyFile).get(fingerprint("/real-only"));
        assertThat(migrated.implementedAt()).isEqualTo(oldImplementedAt);
        assertThat(migrated.stubbedAt()).isNull();
        assertThat(migrated.declaringClass()).isEqualTo("com.example.RealOnlyController");
        assertThat(migrated.removedAt()).isNull();
    }

    @Test
    @DisplayName("a record matching only a stub gets stubbedAt and loses implementedAt")
    void stubOnlyMatchGetsStubbedAtLosesImplementedAt() throws Exception {
        Instant oldImplementedAt = Instant.parse("2026-01-10T00:00:00Z");
        File historyFile = writeLegacyHistory(legacyLine("/stub-only", oldImplementedAt, "(mappings)"));

        MigrateContractHistoryTask task = newTask(historyFile);
        task.migrate();

        ContractProgressRecord migrated = loadMigrated(historyFile).get(fingerprint("/stub-only"));
        assertThat(migrated.implementedAt()).isNull();
        assertThat(migrated.stubbedAt()).isEqualTo(oldImplementedAt);
        assertThat(migrated.declaringClass()).isNull();
        assertThat(migrated.removedAt()).isNull();
    }

    @Test
    @DisplayName("a record matching both a real controller and a stub gets both timestamps")
    void bothMatchGetsBothTimestamps() throws Exception {
        Instant oldImplementedAt = Instant.parse("2026-01-10T00:00:00Z");
        File historyFile = writeLegacyHistory(legacyLine("/both", oldImplementedAt, null));

        MigrateContractHistoryTask task = newTask(historyFile);
        task.migrate();

        ContractProgressRecord migrated = loadMigrated(historyFile).get(fingerprint("/both"));
        assertThat(migrated.implementedAt()).isEqualTo(oldImplementedAt);
        assertThat(migrated.stubbedAt()).isEqualTo(oldImplementedAt);
        assertThat(migrated.declaringClass()).isEqualTo("com.example.RealOnlyController");
        assertThat(migrated.removedAt()).isNull();
    }

    @Test
    @DisplayName("a record matching neither, not already removed, gets both timestamps and removedAt stamped now")
    void neitherMatchNotAlreadyRemovedGetsBothTimestampsAndRemovedAtNow() throws Exception {
        Instant oldImplementedAt = Instant.parse("2026-01-10T00:00:00Z");
        File historyFile = writeLegacyHistory(legacyLine("/gone-but-not-marked", oldImplementedAt, null));
        Instant before = Instant.now();

        MigrateContractHistoryTask task = newTask(historyFile);
        task.migrate();

        ContractProgressRecord migrated = loadMigrated(historyFile).get(fingerprint("/gone-but-not-marked"));
        assertThat(migrated.implementedAt()).isEqualTo(oldImplementedAt);
        assertThat(migrated.stubbedAt()).isEqualTo(oldImplementedAt);
        assertThat(migrated.declaringClass()).isNull();
        assertThat(migrated.removedAt()).isNotNull().isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("a record already marked removed has both timestamps cleared instead of guessed")
    void alreadyRemovedRecordHasBothTimestampsCleared() throws Exception {
        Instant oldImplementedAt = Instant.parse("2026-01-10T00:00:00Z");
        Instant oldRemovedAt = Instant.parse("2026-02-01T00:00:00Z");
        String legacyLine = "{\"fingerprint\":\"" + fingerprint("/real-only") + "\",\"verb\":\"GET\","
                + "\"path\":\"/real-only\",\"declaringClass\":\"com.example.RealOnlyController\","
                + "\"declaredAt\":\"2026-01-01T00:00:00Z\",\"implementedAt\":\"" + oldImplementedAt + "\","
                + "\"verifiedAt\":null,\"lastSeenAt\":\"2026-01-10T00:00:00Z\","
                + "\"removedAt\":\"" + oldRemovedAt + "\"}";
        File historyFile = writeLegacyHistory(legacyLine);

        MigrateContractHistoryTask task = newTask(historyFile);
        task.migrate();

        ContractProgressRecord migrated = loadMigrated(historyFile).get(fingerprint("/real-only"));
        assertThat(migrated.implementedAt()).isNull();
        assertThat(migrated.stubbedAt()).isNull();
        assertThat(migrated.removedAt()).isEqualTo(oldRemovedAt);
    }

    @Test
    @DisplayName("a declared-only legacy record (no implementedAt) is carried over unchanged")
    void declaredOnlyRecordIsCarriedOverUnchanged() throws Exception {
        String legacyLine = "{\"fingerprint\":\"" + fingerprint("/declared-only") + "\",\"verb\":\"GET\","
                + "\"path\":\"/declared-only\",\"declaringClass\":null,"
                + "\"declaredAt\":\"2026-01-01T00:00:00Z\",\"implementedAt\":null,"
                + "\"verifiedAt\":null,\"lastSeenAt\":\"2026-01-01T00:00:00Z\",\"removedAt\":null}";
        File historyFile = writeLegacyHistory(legacyLine);

        MigrateContractHistoryTask task = newTask(historyFile);
        task.migrate();

        ContractProgressRecord migrated = loadMigrated(historyFile).get(fingerprint("/declared-only"));
        assertThat(migrated.implementedAt()).isNull();
        assertThat(migrated.stubbedAt()).isNull();
        assertThat(migrated.declaredAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    @DisplayName("backs up the original file to <file>.bak before overwriting")
    void backsUpOriginalFileBeforeOverwriting() throws Exception {
        String legacyLine = legacyLine("/real-only", Instant.parse("2026-01-10T00:00:00Z"), null);
        File historyFile = writeLegacyHistory(legacyLine);
        String originalContent = Files.readString(historyFile.toPath());

        MigrateContractHistoryTask task = newTask(historyFile);
        task.migrate();

        File backup = new File(historyFile.getParentFile(), historyFile.getName() + ".bak");
        assertThat(backup).exists();
        assertThat(Files.readString(backup.toPath())).isEqualTo(originalContent);
    }

    @Test
    @DisplayName("writes the migrated file in the current 10-field format, loadable by the regular store")
    void writesCurrentFormatLoadableByRegularStore() throws Exception {
        File historyFile = writeLegacyHistory(legacyLine("/real-only", Instant.parse("2026-01-10T00:00:00Z"), null));

        MigrateContractHistoryTask task = newTask(historyFile);
        task.migrate();

        Map<String, ContractProgressRecord> loaded = new ContractHistoryStore().load(historyFile);
        assertThat(loaded).containsKey(fingerprint("/real-only"));
    }

    private String legacyLine(String path, Instant implementedAt, String declaringClass) {
        return "{\"fingerprint\":\"" + fingerprint(path) + "\",\"verb\":\"GET\","
                + "\"path\":\"" + path + "\","
                + "\"declaringClass\":" + (declaringClass == null ? "null" : "\"" + declaringClass + "\"") + ","
                + "\"declaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"implementedAt\":" + (implementedAt == null ? "null" : "\"" + implementedAt + "\"") + ","
                + "\"verifiedAt\":null,\"lastSeenAt\":\"2026-01-10T00:00:00Z\",\"removedAt\":null}";
    }

    private String fingerprint(String path) {
        Endpoint endpoint = new Endpoint(HttpVerb.GET, path, "irrelevant", "irrelevant()", "Irrelevant.java", 1);
        return FINGERPRINTER.fingerprint(endpoint);
    }

    private File writeLegacyHistory(String... lines) throws Exception {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        Files.writeString(historyFile.toPath(), String.join("\n", lines) + "\n");
        return historyFile;
    }

    private Map<String, ContractProgressRecord> loadMigrated(File historyFile) {
        return new ContractHistoryStore().load(historyFile);
    }

    private MigrateContractHistoryTask newTask(File historyFile) {
        MigrateContractHistoryTask task = project.getTasks()
                .create("migrateContractHistoryUnderTest", MigrateContractHistoryTask.class);
        task.getControllerDirs().from(controllerDir);
        task.getStubDirs().from(stubDir);
        task.getContractHistoryFile().set(historyFile);
        return task;
    }
}
