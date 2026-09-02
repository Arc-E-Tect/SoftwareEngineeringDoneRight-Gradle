package com.arc_e_tect.gradle.jacoco;

import com.arc_e_tect.gradle.dslupdater.DslPropertyKind;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JacocoExclusionReportDslSchema")
class JacocoExclusionReportDslSchemaTest {

    @Test
    @DisplayName("schemaShouldTargetTheJacocoExclusionReportBlock")
    void schemaShouldTargetTheJacocoExclusionReportBlock() {
        assertThat(JacocoExclusionReportDslSchema.SCHEMA.blockName()).isEqualTo(JacocoExclusionReportExtension.NAME);
    }

    @Test
    @DisplayName("schemaShouldListOnlyThePropertiesWithARealDefault")
    void schemaShouldListOnlyThePropertiesWithARealDefault() {
        List<String> names = JacocoExclusionReportDslSchema.SCHEMA.properties().stream()
                .map(DslPropertySpec::name)
                .collect(Collectors.toList());

        assertThat(names).containsExactly("annotationName", "reportDir", "includeConfiguredExclusions",
                "includeGeneratedAnnotationExclusions");
    }

    @Test
    @DisplayName("schemaShouldHaveNoContainerProperties")
    void schemaShouldHaveNoContainerProperties() {
        assertThat(JacocoExclusionReportDslSchema.SCHEMA.properties())
                .allMatch(property -> property.kind() == DslPropertyKind.SCALAR);
    }
}
