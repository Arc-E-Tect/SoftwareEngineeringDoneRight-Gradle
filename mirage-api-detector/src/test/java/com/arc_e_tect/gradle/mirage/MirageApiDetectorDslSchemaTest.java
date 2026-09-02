package com.arc_e_tect.gradle.mirage;

import com.arc_e_tect.gradle.dslupdater.DslPropertyKind;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MirageApiDetectorDslSchema")
class MirageApiDetectorDslSchemaTest {

    @Test
    @DisplayName("schemaShouldTargetTheMirageApiDetectorBlock")
    void schemaShouldTargetTheMirageApiDetectorBlock() {
        assertThat(MirageApiDetectorDslSchema.SCHEMA.blockName()).isEqualTo(MirageApiDetectorExtension.NAME);
    }

    @Test
    @DisplayName("schemaShouldListOnlyThePropertiesWithARealDefault")
    void schemaShouldListOnlyThePropertiesWithARealDefault() {
        List<String> names = MirageApiDetectorDslSchema.SCHEMA.properties().stream()
                .map(DslPropertySpec::name)
                .collect(Collectors.toList());

        assertThat(names).containsExactly("failOnMirage", "scanMocks", "reportDir", "reportFileName",
                "systemUnderTestVersion", "openApiDir", "trackContractHistory", "contractHistoryFile",
                "updateContractHistory", "excludePaths", "excludeWellKnown");
    }

    @Test
    @DisplayName("schemaShouldHaveNoContainerProperties")
    void schemaShouldHaveNoContainerProperties() {
        assertThat(MirageApiDetectorDslSchema.SCHEMA.properties())
                .allMatch(property -> property.kind() == DslPropertyKind.SCALAR);
    }
}
