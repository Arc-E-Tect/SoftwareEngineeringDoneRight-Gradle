package com.arc_e_tect.gradle.gherkin;

import com.arc_e_tect.gradle.dslupdater.DslPropertyKind;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;
import com.arc_e_tect.gradle.dslupdater.DslUpdater;
import com.arc_e_tect.gradle.dslupdater.UpdateDslOptions;
import com.arc_e_tect.gradle.dslupdater.UpdateDslResult;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adds any {@code gherkinToAsciidoc} DSL properties missing from this project's build file, set to
 * their default values - see {@code com.arc_e_tect.gradle.dslupdater.DslUpdater} and
 * {@link GherkinToAsciidocDslSchema}. Setting an added property explicitly afterwards is a no-op,
 * so running this task after upgrading the plugin is always safe.
 *
 * <p>Groovy DSL ({@code build.gradle}) only - see {@code dsl-updater-core}'s own documentation for
 * why.</p>
 */
@DisableCachingByDefault(because = "Rewrites a source file in place; a cache restore would bypass that entirely")
public abstract class UpdateGherkinToAsciidocDslTask extends DefaultTask {

    /** Creates a new task instance. Instantiated by Gradle infrastructure. */
    @Inject
    public UpdateGherkinToAsciidocDslTask() {
        setGroup("gherkin to asciidoc");
        setDescription(
                "Adds any gherkinToAsciidoc DSL properties missing from the build file, set to their defaults.");
        getGenerateDsl().convention(false);
        getCleanupDsl().convention(false);
    }

    /**
     * The build file to update - the project's own {@code build.gradle}.
     *
     * @return read-only file property for the build file
     */
    @Internal
    public abstract RegularFileProperty getBuildFile();

    /**
     * When the {@code gherkinToAsciidoc} block is entirely absent, synthesize a full new one from
     * the schema and append it to the end of the build file; when {@code false} (the default), a
     * missing block is left alone.
     *
     * @return mutable property for the generate-DSL flag
     */
    @Internal
    public abstract Property<Boolean> getGenerateDsl();

    /**
     * Sets {@link #getGenerateDsl()} for this run only.
     *
     * @param value the value to set {@link #getGenerateDsl()} to
     */
    @Option(option = "generateGherkinToAsciidocDSL",
            description = "When the gherkinToAsciidoc block is entirely absent, generates a full new one from the "
                    + "schema.")
    public void applyGenerateDsl(boolean value) {
        getGenerateDsl().set(value);
    }

    /**
     * Strips every comment line from inside the managed block - both pre-existing ones and any
     * doc comment this run would otherwise add - leaving only properties.
     *
     * @return mutable property for the cleanup-DSL flag
     */
    @Internal
    public abstract Property<Boolean> getCleanupDsl();

    /**
     * Sets {@link #getCleanupDsl()} for this run only.
     *
     * @param value the value to set {@link #getCleanupDsl()} to
     */
    @Option(option = "cleanupGherkinToAsciidocDSL",
            description = "Strips every comment from inside the gherkinToAsciidoc block.")
    public void applyCleanupDsl(boolean value) {
        getCleanupDsl().set(value);
    }

    /**
     * Reads the build file, adds any missing DSL properties, and writes it back if anything
     * changed - after first copying the original, untouched file alongside it as
     * {@code build.gradle.bak}, so there's always a plain-file fallback even for a project not
     * using version control (or one that just hasn't committed the file yet).
     */
    @TaskAction
    public void updateDsl() {
        Path buildFile = getBuildFile().get().getAsFile().toPath();
        if (!Files.exists(buildFile)) {
            throw new GradleException("gherkinToAsciidoc: build file not found: " + buildFile);
        }

        String original;
        try {
            original = Files.readString(buildFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GradleException("gherkinToAsciidoc: failed to read " + buildFile, e);
        }

        UpdateDslOptions options = new UpdateDslOptions(getGenerateDsl().get(), getCleanupDsl().get());
        DslUpdater.Outcome outcome = DslUpdater.update(original, GherkinToAsciidocDslSchema.SCHEMA, options);
        UpdateDslResult result = outcome.result();

        if (!result.changed()) {
            if (!result.blockFoundBefore()) {
                getLogger().lifecycle(
                        "gherkinToAsciidoc: updateGherkinToAsciidocDSL found no gherkinToAsciidoc block in {} "
                                + "- rerun with --generateGherkinToAsciidocDSL to add one.",
                        buildFile);
            } else {
                getLogger().lifecycle(
                        "gherkinToAsciidoc: updateGherkinToAsciidocDSL found the gherkinToAsciidoc block already "
                                + "up to date in {}",
                        buildFile);
            }
            return;
        }

        Path backup = buildFile.resolveSibling(buildFile.getFileName() + ".bak");
        try {
            // Copied from the file on disk, not written from the in-memory `original` string, so
            // the backup is a byte-for-byte copy of what was there - not a UTF-8 re-encoding of it.
            Files.copy(buildFile, backup, StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(buildFile, outcome.source(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GradleException("gherkinToAsciidoc: failed to write " + buildFile, e);
        }
        getLogger().lifecycle("gherkinToAsciidoc: updateGherkinToAsciidocDSL backed up the original file to {}",
                backup);

        if (result.blockGenerated()) {
            getLogger().lifecycle("gherkinToAsciidoc: updateGherkinToAsciidocDSL generated a new "
                    + "gherkinToAsciidoc block in {}", buildFile);
        } else if (!result.addedProperties().isEmpty()) {
            String propertyWord = result.addedProperties().size() == 1 ? "property" : "properties";
            getLogger().lifecycle("gherkinToAsciidoc: updateGherkinToAsciidocDSL added {} missing {} to the "
                    + "gherkinToAsciidoc block in {}", result.addedProperties().size(), propertyWord, buildFile);
        }
        if (result.cleaned()) {
            getLogger().lifecycle("gherkinToAsciidoc: updateGherkinToAsciidocDSL removed comments from the "
                    + "gherkinToAsciidoc block in {}", buildFile);
        }

        Map<String, String> defaultsByName = GherkinToAsciidocDslSchema.SCHEMA.properties().stream()
                .filter(property -> property.kind() == DslPropertyKind.SCALAR)
                .collect(Collectors.toMap(DslPropertySpec::name, DslPropertySpec::defaultLiteral));
        for (String name : result.addedProperties()) {
            getLogger().info("gherkinToAsciidoc: updateGherkinToAsciidocDSL added {} = {}", name,
                    defaultsByName.get(name));
        }
    }
}
