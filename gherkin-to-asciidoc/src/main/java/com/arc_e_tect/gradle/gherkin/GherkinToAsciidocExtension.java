package com.arc_e_tect.gradle.gherkin;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

public abstract class GherkinToAsciidocExtension {

    public GherkinToAsciidocExtension() {}

    public static final String NAME = "gherkinToAsciidoc";
    public static final String DEFAULT_SOURCE_DIR = "src/test/resources/features";
    public static final String DEFAULT_OUTPUT_FILE_NAME = "features.adoc";

    public abstract DirectoryProperty getSourceDir();

    public abstract RegularFileProperty getSourceFile();

    public abstract Property<Boolean> getIncludeSubDirs();

    public abstract DirectoryProperty getOutputDir();

    public abstract Property<String> getOutputFileName();
}
