package com.arc_e_tect.gradle.shadow;

import com.arc_e_tect.gradle.dslupdater.DslPropertyKind;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShadowApiDetectorDslSchema")
class ShadowApiDetectorDslSchemaTest {

    @Test
    @DisplayName("schemaShouldTargetTheShadowApiDetectorBlock")
    void schemaShouldTargetTheShadowApiDetectorBlock() {
        assertThat(ShadowApiDetectorDslSchema.SCHEMA.blockName()).isEqualTo(ShadowApiDetectorExtension.NAME);
    }

    @Test
    @DisplayName("schemaShouldListOnlyThePropertiesWithARealDefault")
    void schemaShouldListOnlyThePropertiesWithARealDefault() {
        List<String> names = ShadowApiDetectorDslSchema.SCHEMA.properties().stream()
                .map(DslPropertySpec::name)
                .collect(Collectors.toList());

        assertThat(names).containsExactly("failOnShadow", "reportDir", "reportFileName", "systemUnderTestVersion",
                "openApiDir", "trackContractHistory", "contractHistoryFile", "updateContractHistory",
                "excludePaths", "excludeWellKnown");
    }

    @Test
    @DisplayName("schemaShouldHaveNoContainerProperties")
    void schemaShouldHaveNoContainerProperties() {
        assertThat(ShadowApiDetectorDslSchema.SCHEMA.properties())
                .allMatch(property -> property.kind() == DslPropertyKind.SCALAR);
    }
}
