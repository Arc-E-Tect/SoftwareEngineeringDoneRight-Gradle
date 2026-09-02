package com.arc_e_tect.gradle.suite;

import com.arc_e_tect.gradle.dslupdater.DslPropertyKind;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiOnlySuiteDslSchema")
class ApiOnlySuiteDslSchemaTest {

    @Test
    @DisplayName("schemaShouldTargetTheApiOnlySuiteBlock")
    void schemaShouldTargetTheApiOnlySuiteBlock() {
        assertThat(ApiOnlySuiteDslSchema.SCHEMA.blockName()).isEqualTo(ApiOnlySuiteExtension.NAME);
    }

    @Test
    @DisplayName("schemaShouldListOnlyThePropertiesWithARealDefault")
    void schemaShouldListOnlyThePropertiesWithARealDefault() {
        List<String> names = ApiOnlySuiteDslSchema.SCHEMA.properties().stream()
                .map(DslPropertySpec::name)
                .collect(Collectors.toList());

        assertThat(names).containsExactly("failOnDetection", "excludePaths", "excludeWellKnown");
    }

    @Test
    @DisplayName("schemaShouldHaveNoContainerProperties")
    void schemaShouldHaveNoContainerProperties() {
        assertThat(ApiOnlySuiteDslSchema.SCHEMA.properties())
                .allMatch(property -> property.kind() == DslPropertyKind.SCALAR);
    }
}
