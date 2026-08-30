package com.arc_e_tect.gradle.doppelganger;

import com.arc_e_tect.gradle.detector.core.scan.PropertyResolutionContext;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PropertyResolutionContextFactory")
class PropertyResolutionContextFactoryTest {

    @Test
    @DisplayName("returns PropertyResolutionContext.empty() when nothing is configured")
    void returnsEmptyWhenNothingConfigured(@TempDir Path tempDir) {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        ConfigurableFileCollection propertyFiles = project.files();
        ListProperty<String> helperMethods = project.getObjects().listProperty(String.class);
        helperMethods.set(List.of());

        PropertyResolutionContext context = PropertyResolutionContextFactory.create(propertyFiles, helperMethods);

        assertThat(context.properties()).isEmpty();
        assertThat(context.isHelperMethod("ApiEndpoints", "get")).isFalse();
    }

    @Test
    @DisplayName("loads a .properties file's keys as-is")
    void loadsPropertiesFile(@TempDir Path tempDir) throws Exception {
        Path propertiesFile = tempDir.resolve("api-endpoints.properties");
        Files.writeString(propertiesFile, "users.by-username=/v1/users/{username}\n");
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        ConfigurableFileCollection propertyFiles = project.files(propertiesFile.toFile());
        ListProperty<String> helperMethods = project.getObjects().listProperty(String.class);
        helperMethods.set(List.of("ApiEndpoints.get"));

        PropertyResolutionContext context = PropertyResolutionContextFactory.create(propertyFiles, helperMethods);

        assertThat(context.lookup("users.by-username")).contains("/v1/users/{username}");
        assertThat(context.isHelperMethod("ApiEndpoints", "get")).isTrue();
    }

    @Test
    @DisplayName("flattens a nested .yml document's mappings to dotted keys")
    void flattensYamlFile(@TempDir Path tempDir) throws Exception {
        Path yamlFile = tempDir.resolve("api-endpoints.yml");
        Files.writeString(yamlFile, "users:\n  by-username: /v1/users/{username}\n");
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        ConfigurableFileCollection propertyFiles = project.files(yamlFile.toFile());
        ListProperty<String> helperMethods = project.getObjects().listProperty(String.class);
        helperMethods.set(List.of());

        PropertyResolutionContext context = PropertyResolutionContextFactory.create(propertyFiles, helperMethods);

        assertThat(context.lookup("users.by-username")).contains("/v1/users/{username}");
    }

    @Test
    @DisplayName("later files take precedence over earlier ones on key collision")
    void laterFileWinsOnCollision(@TempDir Path tempDir) throws Exception {
        Path first = tempDir.resolve("first.properties");
        Path second = tempDir.resolve("second.properties");
        Files.writeString(first, "users.by-username=/v1/first\n");
        Files.writeString(second, "users.by-username=/v1/second\n");
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        ConfigurableFileCollection propertyFiles = project.files(first.toFile(), second.toFile());
        ListProperty<String> helperMethods = project.getObjects().listProperty(String.class);
        helperMethods.set(List.of());

        PropertyResolutionContext context = PropertyResolutionContextFactory.create(propertyFiles, helperMethods);

        assertThat(context.lookup("users.by-username")).contains("/v1/second");
    }

    @Test
    @DisplayName("silently skips a configured file that does not exist yet")
    void skipsMissingFile(@TempDir Path tempDir) {
        File missing = new File(tempDir.toFile(), "does-not-exist.properties");
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        ConfigurableFileCollection propertyFiles = project.files(missing);
        ListProperty<String> helperMethods = project.getObjects().listProperty(String.class);
        helperMethods.set(List.of());

        PropertyResolutionContext context = PropertyResolutionContextFactory.create(propertyFiles, helperMethods);

        assertThat(context.properties()).isEmpty();
    }
}
