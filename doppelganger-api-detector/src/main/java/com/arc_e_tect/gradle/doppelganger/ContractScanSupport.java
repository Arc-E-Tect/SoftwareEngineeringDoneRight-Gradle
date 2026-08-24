package com.arc_e_tect.gradle.doppelganger;

import com.arc_e_tect.gradle.detector.core.console.ScanProgressReporter;
import com.arc_e_tect.gradle.detector.core.exclude.ExclusionRule;
import com.arc_e_tect.gradle.detector.core.exclude.ExclusionRuleFile;
import com.arc_e_tect.gradle.detector.core.exclude.WellKnownExclusionSets;
import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.scan.ControllerScanner;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.ListProperty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrapping-gap handling and scanning building blocks shared by {@link DetectDoppelgangerApisTask}
 * and {@link ScanContractsTask} - both scan the same kind of {@code controllerDirs}, apply the same
 * exclusion rules, and need to treat a not-yet-existing configured directory as a warning rather
 * than a build failure. Deliberately package-private and stateless: every method takes exactly the
 * inputs it needs rather than a reference to either task, so it stays trivially reusable without
 * coupling the two task classes to each other.
 */
final class ContractScanSupport {

    private ContractScanSupport() {}

    /**
     * The result of checking a configured set of source directories: every {@code .java} file
     * found under an existing one, which configured entries don't exist yet, and whether at least
     * one configured entry exists at all.
     *
     * @param javaFiles    every {@code .java} file found recursively under an existing directory
     * @param missingDirs  configured directories that don't exist yet
     * @param anyDirExists whether at least one configured directory exists
     */
    record DirectoryScanResult(List<File> javaFiles, List<File> missingDirs, boolean anyDirExists) {

        /**
         * Whether every configured directory is missing - distinct from "zero directories were
         * configured at all", which is a valid, complete input rather than a bootstrapping gap.
         */
        boolean allConfiguredDirsMissing() {
            return !missingDirs.isEmpty() && !anyDirExists;
        }
    }

    /**
     * Scans {@code dirs} for {@code .java} files, recording which configured entries don't exist
     * yet.
     *
     * @param dirs the configured source directories
     * @return the scan result
     */
    static DirectoryScanResult scanJavaSourceDirs(Iterable<File> dirs) {
        List<File> missingDirs = new ArrayList<>();
        List<File> javaFiles = new ArrayList<>();
        boolean anyDirExists = false;
        for (File dir : dirs) {
            if (dir.isDirectory()) {
                anyDirExists = true;
                javaFiles.addAll(collectJavaFiles(dir));
            } else {
                missingDirs.add(dir);
            }
        }
        return new DirectoryScanResult(javaFiles, missingDirs, anyDirExists);
    }

    /**
     * Recursively collects every {@code .java} file under {@code dir}.
     *
     * @param dir the directory to search; a non-directory path yields an empty list
     * @return every {@code .java} file found, in no particular order
     */
    static List<File> collectJavaFiles(File dir) {
        List<File> files = new ArrayList<>();
        collectJavaFiles(dir, files);
        return files;
    }

    private static void collectJavaFiles(File dir, List<File> files) {
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

    /**
     * Scans every file in {@code controllerFiles} for {@code @RestController} endpoints, reporting
     * determinate progress to {@code logger} as it goes.
     *
     * @param controllerFiles the {@code .java} files to scan
     * @param logger          the task's logger, used for the progress banner
     * @return every endpoint found, across all scanned files
     * @throws GradleException if a file cannot be scanned
     */
    static List<Endpoint> scanControllerFiles(List<File> controllerFiles, Logger logger) {
        ControllerScanner controllerScanner = new ControllerScanner();
        List<Endpoint> implemented = new ArrayList<>();
        ScanProgressReporter progress =
                ScanProgressReporter.determinate(logger, "Scanning @RestController classes", controllerFiles.size());
        for (File javaFile : controllerFiles) {
            try {
                implemented.addAll(controllerScanner.scan(javaFile));
            } catch (IOException e) {
                throw new GradleException("doppelgangerApiDetector: failed to scan " + javaFile, e);
            }
            progress.step();
        }
        progress.complete();
        return implemented;
    }

    /**
     * Resolves every configured exclusion rule - {@code excludePaths}, {@code excludeFiles}, and
     * {@code excludeWellKnown} - into one combined list. A missing {@code excludeFiles} entry only
     * warns, the same way a missing source directory does; a malformed rule string or an
     * unrecognised well-known set name fails the build outright, since those are build-script/file
     * mistakes, not a "not built yet" bootstrapping gap.
     *
     * @param excludePaths      exclusion rule strings
     * @param excludeFiles      exclusion rule files
     * @param excludeWellKnown  bundled well-known exclusion set names
     * @param warnings          appended to for a missing {@code excludeFiles} entry
     * @return the combined, resolved exclusion rules
     * @throws GradleException if a rule string, file, or well-known set name is invalid
     */
    static List<ExclusionRule> resolveExclusionRules(
            ListProperty<String> excludePaths, ConfigurableFileCollection excludeFiles,
            ListProperty<String> excludeWellKnown, List<String> warnings) {
        List<ExclusionRule> rules = new ArrayList<>();
        for (String entry : excludePaths.get()) {
            try {
                rules.add(ExclusionRule.parse(entry));
            } catch (IllegalArgumentException e) {
                throw new GradleException(
                        "doppelgangerApiDetector: invalid `excludePaths` entry: " + e.getMessage(), e);
            }
        }
        for (File file : excludeFiles) {
            if (!file.isFile()) {
                warnings.add("Configured `excludeFiles` entry does not exist yet: `" + file + "`.");
                continue;
            }
            try {
                rules.addAll(ExclusionRuleFile.load(file));
            } catch (IOException e) {
                throw new GradleException("doppelgangerApiDetector: failed to read excludeFiles entry " + file, e);
            } catch (IllegalArgumentException e) {
                throw new GradleException("doppelgangerApiDetector: " + e.getMessage(), e);
            }
        }
        for (String name : excludeWellKnown.get()) {
            try {
                rules.addAll(WellKnownExclusionSets.resolve(name));
            } catch (IllegalArgumentException e) {
                throw new GradleException("doppelgangerApiDetector: " + e.getMessage(), e);
            }
        }
        return rules;
    }
}
