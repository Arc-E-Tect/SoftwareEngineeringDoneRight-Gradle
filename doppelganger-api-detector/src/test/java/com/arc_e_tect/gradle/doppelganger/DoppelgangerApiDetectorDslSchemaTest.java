package com.arc_e_tect.gradle.doppelganger;

import com.arc_e_tect.gradle.dslupdater.DslPropertyKind;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DoppelgangerApiDetectorDslSchema")
class DoppelgangerApiDetectorDslSchemaTest {

    @Test
    @DisplayName("schemaShouldTargetTheDoppelgangerApiDetectorBlock")
    void schemaShouldTargetTheDoppelgangerApiDetectorBlock() {
        assertThat(DoppelgangerApiDetectorDslSchema.SCHEMA.blockName())
                .isEqualTo(DoppelgangerApiDetectorExtension.NAME);
    }

    @Test
    @DisplayName("schemaShouldListOnlyThePropertiesWithARealDefault")
    void schemaShouldListOnlyThePropertiesWithARealDefault() {
        List<String> names = DoppelgangerApiDetectorDslSchema.SCHEMA.properties().stream()
                .map(DslPropertySpec::name)
                .collect(Collectors.toList());

        assertThat(names).containsExactly("failOnDoppelganger", "useRestDocs", "useOpenApiRequestValidator",
                "useSpringCloudContract", "reportDir", "reportFileName", "systemUnderTestVersion", "openApiDir",
                "trackContractHistory", "contractHistoryFile", "updateContractHistory", "excludePaths",
                "excludeWellKnown", "pathResolverHelperMethods", "includeResponseCoverage", "ignore5xx",
                "scanContractsReportFileName", "trackResponseCoverageHistory", "responseCoverageHistoryFile",
                "updateResponseCoverageHistory");
    }

    @Test
    @DisplayName("schemaShouldHaveNoContainerProperties")
    void schemaShouldHaveNoContainerProperties() {
        assertThat(DoppelgangerApiDetectorDslSchema.SCHEMA.properties())
                .allMatch(property -> property.kind() == DslPropertyKind.SCALAR);
    }
}
