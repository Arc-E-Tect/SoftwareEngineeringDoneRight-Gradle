package com.arc_e_tect.gradle.mirage;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.progress.ContractHistoryStore;
import com.arc_e_tect.gradle.detector.core.progress.ContractProgressRecord;
import com.arc_e_tect.gradle.detector.core.progress.EndpointFingerprint;
import com.arc_e_tect.gradle.detector.core.scan.ControllerScanner;
import com.arc_e_tect.gradle.mirage.scan.WireMockStubScanner;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gradle task that migrates {@link #getContractHistoryFile()} from the pre-{@code stubbedAt}
 * 9-field NDJSON format to the current 10-field format, by re-scanning the currently configured
 * {@link #getControllerDirs()} and {@link #getStubDirs()} to work out, for every legacy record
 * that has {@code implementedAt} set, whether that evidence should now count as a real
 * implementation, a WireMock stub, both, or neither.
 *
 * <h2>CAUTION</h2>
 * <p>This migration is a best-effort reconstruction, not a lossless transformation. The legacy
 * format's {@code implementedAt} never distinguished a real {@code @RestController} match from a
 * WireMock stub match, so this task can only tell them apart by re-scanning the project's
 * <em>current</em> source and stub directories - it has no way to know what those directories
 * looked like back when each timestamp was first stamped. {@code contractHistoryFile} is backed
 * up alongside itself (as {@code <file>.bak}) before being overwritten, but review the migrated
 * file - or the version-controlled diff of it - before committing the result.</p>
 *
 * <p>Per legacy record with a non-null {@code implementedAt}:</p>
 * <ul>
 *   <li><b>Already marked {@code removedAt}</b> - both {@code implementedAt} and
 *       {@code stubbedAt} are cleared to {@code null}. The endpoint is already gone, so there is
 *       no current evidence left to re-derive either field from, and guessing would bake in a
 *       permanent, unverifiable assumption for a record that can never be re-scanned
 *       successfully.</li>
 *   <li><b>Matches a currently-scanned controller only</b> - {@code implementedAt} is kept as-is
 *       and {@code declaringClass} is refreshed from the match; {@code stubbedAt} stays
 *       {@code null}. No evidence a stub was ever involved.</li>
 *   <li><b>Matches a currently-scanned stub only</b> - {@code implementedAt} becomes {@code null}
 *       and {@code stubbedAt} takes the old {@code implementedAt} value; {@code declaringClass}
 *       becomes {@code null}. This is the case the whole migration exists to correct.</li>
 *   <li><b>Matches both</b> - both {@code implementedAt} and {@code stubbedAt} take the old
 *       {@code implementedAt} value, and {@code declaringClass} is refreshed from the controller
 *       match. Which of the two evidence types actually came first is lost, but the "first-seen"
 *       date itself is still accurate for whichever it turns out to be.</li>
 *   <li><b>Matches neither, and {@code removedAt} was not already set</b> - both
 *       {@code implementedAt} and {@code stubbedAt} take the old value (same reasoning as
 *       "matches both": preserve the ambiguous signal rather than discard it), and
 *       {@code removedAt} is stamped with this migration run's time, since the endpoint can no
 *       longer be found anywhere - the same outcome a normal {@code detectMirageApis} run would
 *       reach if it observed the same absence.</li>
 * </ul>
 *
 * <p>Registered automatically by {@link MirageApiDetectorPlugin} under the name
 * {@code migrateContractHistory}. Running it against a file that is already in the current
 * 10-field format fails - see {@link DetectMirageApisTask} for the exception guiding you here in
 * the first place.</p>
 */
@DisableCachingByDefault(because = "One-off, manually-invoked file migration; not part of the normal build graph")
public abstract class MigrateContractHistoryTask extends DefaultTask {

    /**
     * Directories to search recursively for {@code @RestController} classes, used to determine
     * which legacy records currently have real-implementation evidence.
     *
     * @return mutable file collection of controller source directories
     */
    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getControllerDirs();

    /**
     * Directories to search recursively for WireMock stub mapping files, used to determine which
     * legacy records currently have stub evidence.
     *
     * @return mutable file collection of WireMock stub directories
     */
    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getStubDirs();

    /**
     * The legacy-format contract history file to migrate in place. Deliberately not declared as
     * an {@code @InputFile}/{@code @OutputFile} - see {@link DetectMirageApisTask#getContractHistoryFile()}
     * for why this family of tasks reads and writes this file directly instead.
     *
     * @return mutable file property for the contract history file
     */
    @Internal
    public abstract RegularFileProperty getContractHistoryFile();

    /**
     * Creates the task. Instantiated by Gradle infrastructure via {@link javax.inject.Inject}.
     */
    @Inject
    public MigrateContractHistoryTask() {
        setGroup("verification");
        setDescription("CAUTION: migrates contractHistoryFile from the old 9-field format to the current "
                + "10-field format (adds stubbedAt), re-scanning controllerDirs/stubDirs to reconstruct "
                + "which legacy implementedAt values were really stub evidence. The original file is backed "
                + "up as <file>.bak, but review the result before committing it.");
    }

    /**
     * Task action: loads {@link #getContractHistoryFile()} as the legacy 9-field format, re-scans
     * {@link #getControllerDirs()} and {@link #getStubDirs()}, reconstructs every record per the
     * rules documented on this class, backs up the original file, and writes the migrated result
     * back to the same path in the current 10-field format.
     */
    @TaskAction
    public void migrate() {
        File historyFile = getContractHistoryFile().getAsFile().get();
        if (!historyFile.isFile()) {
            throw new GradleException("mirageApiDetector: " + historyFile + " does not exist - nothing to migrate.");
        }

        ContractHistoryStore store = new ContractHistoryStore();
        Map<String, ContractProgressRecord> legacy = store.loadLegacy(historyFile);
        if (legacy.isEmpty()) {
            getLogger().lifecycle("mirageApiDetector: {} has no records to migrate.", historyFile);
            return;
        }

        EndpointFingerprint fingerprinter = new EndpointFingerprint();
        Map<String, Endpoint> realByFingerprint = new HashMap<>();
        for (Endpoint endpoint : scanControllers()) {
            realByFingerprint.put(fingerprinter.fingerprint(endpoint), endpoint);
        }
        Set<String> stubFingerprints = new HashSet<>();
        for (Endpoint endpoint : scanStubs()) {
            stubFingerprints.add(fingerprinter.fingerprint(endpoint));
        }

        Instant now = Instant.now();
        List<ContractProgressRecord> migrated = new ArrayList<>();
        for (ContractProgressRecord record : legacy.values()) {
            migrated.add(migrateRecord(record, realByFingerprint, stubFingerprints, now));
        }

        File backup = new File(historyFile.getParentFile(), historyFile.getName() + ".bak");
        try {
            Files.copy(historyFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new GradleException("mirageApiDetector: failed to back up " + historyFile + " to " + backup, e);
        }

        store.save(historyFile, migrated);
        getLogger().lifecycle(
                "Mirage API Detector: migrated {} record(s) in {} to the current 10-field format. "
                        + "Original backed up to {}. Review the result before committing it.",
                migrated.size(), historyFile, backup);
    }

    private ContractProgressRecord migrateRecord(ContractProgressRecord record,
            Map<String, Endpoint> realByFingerprint, Set<String> stubFingerprints, Instant now) {
        if (record.implementedAt() == null) {
            return record;
        }

        if (record.removedAt() != null) {
            return new ContractProgressRecord(
                    record.fingerprint(), record.verb(), record.path(), record.declaringClass(),
                    record.declaredAt(), null, null, record.verifiedAt(),
                    record.lastSeenAt(), record.removedAt());
        }

        Endpoint realMatch = realByFingerprint.get(record.fingerprint());
        boolean stubMatch = stubFingerprints.contains(record.fingerprint());

        Instant implementedAt;
        Instant stubbedAt;
        Instant removedAt;
        String declaringClass;
        if (realMatch != null) {
            implementedAt = record.implementedAt();
            stubbedAt = stubMatch ? record.implementedAt() : null;
            removedAt = null;
            declaringClass = realMatch.declaringClass();
        } else if (stubMatch) {
            implementedAt = null;
            stubbedAt = record.implementedAt();
            removedAt = null;
            declaringClass = null;
        } else {
            implementedAt = record.implementedAt();
            stubbedAt = record.implementedAt();
            removedAt = now;
            declaringClass = null;
        }

        return new ContractProgressRecord(
                record.fingerprint(), record.verb(), record.path(), declaringClass,
                record.declaredAt(), implementedAt, stubbedAt, record.verifiedAt(),
                record.lastSeenAt(), removedAt);
    }

    private List<Endpoint> scanControllers() {
        List<File> controllerFiles = new ArrayList<>();
        for (File dir : getControllerDirs()) {
            collectJavaFiles(dir, controllerFiles);
        }

        ControllerScanner scanner = new ControllerScanner();
        List<Endpoint> endpoints = new ArrayList<>();
        for (File javaFile : controllerFiles) {
            try {
                endpoints.addAll(scanner.scan(javaFile));
            } catch (IOException e) {
                throw new GradleException("mirageApiDetector: failed to scan " + javaFile, e);
            }
        }
        return endpoints;
    }

    private List<Endpoint> scanStubs() {
        WireMockStubScanner scanner = new WireMockStubScanner();
        List<Endpoint> endpoints = new ArrayList<>();
        for (File dir : getStubDirs()) {
            try {
                endpoints.addAll(scanner.scan(dir));
            } catch (IOException e) {
                throw new GradleException("mirageApiDetector: failed to scan " + dir, e);
            }
        }
        return endpoints;
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
}
