package com.arc_e_tect.gradle.gherkin.indexing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Decides which project in a Gradle build a given {@code .feature} file belongs to, by matching the
 * file's path against every project's directory.
 *
 * <p>This is the single notion of "owning project" the plugin scopes work by. {@link FeatureIndexer}
 * uses it to number each project's features as its own 1-based sequence, and the task uses it to tell
 * whether the files one invocation collected actually span more than one project - which is the only
 * situation in which that scoping changes anything.</p>
 *
 * <p>A file that isn't under any of the configured project directories (which shouldn't normally
 * happen, since callers supply every project directory in the build) is attributed to itself rather
 * than being silently folded into an unrelated project.</p>
 */
public final class ProjectAttribution {

    private final List<Candidate> mostSpecificFirst;

    /**
     * Creates an attribution over {@code projectDirectories}.
     *
     * @param projectDirectories every project directory in the build; an empty list attributes every
     *                           file to itself, since there are then no boundaries to match against
     */
    public ProjectAttribution(List<File> projectDirectories) {
        // Deepest path first, so the most specific enclosing project wins: every sub-project
        // directory is itself inside the root project's directory, and the sub-project is the
        // meaningful owner of a file under it.
        //
        // Depth is measured on the canonical path, which is also what files are matched against.
        // Ranking raw paths instead would let a symlinked project directory outrank a genuinely
        // deeper one just for spelling its prefix at greater length - on macOS, a root project at
        // /private/var/folders/.../build (canonical) reads as longer than its own sub-project at
        // /var/folders/.../build/catalog (as configured), and would wrongly claim that
        // sub-project's files. Canonicalizing once here also keeps it off the per-file path.
        List<Candidate> candidates = new ArrayList<>();
        for (File projectDirectory : projectDirectories) {
            candidates.add(new Candidate(projectDirectory, canonicalPath(projectDirectory)));
        }
        candidates.sort(Comparator.comparingInt((Candidate candidate) -> candidate.canonicalPath.getNameCount())
                .reversed());
        this.mostSpecificFirst = List.copyOf(candidates);
    }

    /**
     * The most specific project directory that is an ancestor of {@code featureFile}, or
     * {@code featureFile} itself when none is.
     *
     * @param featureFile the feature file to attribute
     * @return the owning project's directory, never {@code null}
     */
    public File owningProjectDirectory(File featureFile) {
        Path filePath = canonicalPath(featureFile);
        for (Candidate candidate : mostSpecificFirst) {
            if (filePath.startsWith(candidate.canonicalPath)) {
                return candidate.projectDirectory;
            }
        }
        return featureFile;
    }

    /**
     * Every distinct project {@code featureFiles} is spread across, in the order each owner was first
     * encountered.
     *
     * @param featureFiles the feature files one run collected
     * @return the distinct owning project directories; empty when {@code featureFiles} is empty
     */
    public Set<File> owningProjectDirectories(List<File> featureFiles) {
        Set<File> owners = new LinkedHashSet<>();
        for (File featureFile : featureFiles) {
            owners.add(owningProjectDirectory(featureFile));
        }
        return owners;
    }

    /**
     * {@code file}'s canonical path - symlinks resolved, so e.g. macOS's {@code /tmp} -&gt;
     * {@code /private/tmp} doesn't make a feature file look like it lives outside every candidate
     * project directory just because one side of the comparison went through the symlink and the
     * other didn't. Falls back to the plain absolute, normalized path on the rare I/O failure (e.g.
     * the file was deleted mid-run) rather than letting {@link #owningProjectDirectory} throw.
     */
    private Path canonicalPath(File file) {
        try {
            return file.getCanonicalFile().toPath();
        } catch (IOException e) {
            return file.getAbsoluteFile().toPath().normalize();
        }
    }

    /** A project directory, paired with the canonical path every file is matched against. */
    private record Candidate(File projectDirectory, Path canonicalPath) {}
}
