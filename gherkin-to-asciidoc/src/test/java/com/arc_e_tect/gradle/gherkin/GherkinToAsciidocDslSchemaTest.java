package com.arc_e_tect.gradle.gherkin;

import com.arc_e_tect.gradle.dslupdater.DslPropertyKind;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GherkinToAsciidocDslSchema")
class GherkinToAsciidocDslSchemaTest {

    @Test
    @DisplayName("schemaShouldTargetTheGherkinToAsciidocBlock")
    void schemaShouldTargetTheGherkinToAsciidocBlock() {
        assertThat(GherkinToAsciidocDslSchema.SCHEMA.blockName()).isEqualTo(GherkinToAsciidocExtension.NAME);
    }

    @Test
    @DisplayName("schemaShouldListOnlyThePropertiesSafeRegardlessOfRootProjectInheritance")
    void schemaShouldListOnlyThePropertiesSafeRegardlessOfRootProjectInheritance() {
        List<String> names = GherkinToAsciidocDslSchema.SCHEMA.properties().stream()
                .map(DslPropertySpec::name)
                .collect(Collectors.toList());

        assertThat(names).containsExactly("outputDir", "snippetDir", "progressHistoryFile", "updateProgressHistory");
    }

    @Test
    @DisplayName("schemaShouldHaveNoContainerProperties")
    void schemaShouldHaveNoContainerProperties() {
        assertThat(GherkinToAsciidocDslSchema.SCHEMA.properties())
                .allMatch(property -> property.kind() == DslPropertyKind.SCALAR);
    }
}
