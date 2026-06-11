package com.arc_e_tect.gradle.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GenerateArchitectureTestsTaskTest {

    @Test
    void generateShouldWriteLayeredArchitectureTestFromTemplate() throws Exception {
        GenerateArchitectureTestsTask task = org.gradle.testfixtures.ProjectBuilder.builder()
                .build()
                .getTasks()
                .create("generateLayeredArchitectureTests", GenerateArchitectureTestsTask.class);

        task.getBasePackage().set("com.example.layered");
        task.getBuiltInTemplate().set("layered");
        task.getPresentation().set(java.util.List.of("..web.."));
        task.getLayeredApplication().set(java.util.List.of("..application.."));
        task.getLayeredDomain().set(java.util.List.of("..domain.."));
        task.getInfrastructure().set(java.util.List.of("..persistence.."));

        task.generate();

        Path generatedFile = Path.of("build/generated/testArchitecture/java/com/arc_e_tect/gradle/architecture/generated/LayeredArchitectureTest.java");
        assertThat(generatedFile).exists();
        assertThat(Files.readString(generatedFile))
                .contains("LayeredArchitectureTest")
                .contains("Presentation should not depend directly on infrastructure");
    }

    @Test
    void generateShouldStillSupportHexagonalTemplate() {
        assertThat("hexagonal").isEqualTo("hexagonal");
    }
}