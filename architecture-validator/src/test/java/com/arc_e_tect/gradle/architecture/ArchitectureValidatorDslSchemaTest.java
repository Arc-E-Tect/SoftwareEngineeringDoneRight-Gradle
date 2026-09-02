package com.arc_e_tect.gradle.architecture;

import com.arc_e_tect.gradle.dslupdater.DslPropertyKind;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArchitectureValidatorDslSchema")
class ArchitectureValidatorDslSchemaTest {

    @Test
    @DisplayName("schemaShouldTargetTheArchitectureValidatorBlock")
    void schemaShouldTargetTheArchitectureValidatorBlock() {
        assertThat(ArchitectureValidatorDslSchema.SCHEMA.blockName()).isEqualTo(ArchitectureValidatorExtension.NAME);
    }

    @Test
    @DisplayName("schemaShouldListEveryTopLevelPropertyPlusTheNestedHexagonalArchitectureContainer")
    void schemaShouldListEveryTopLevelPropertyPlusTheNestedHexagonalArchitectureContainer() {
        List<String> names = ArchitectureValidatorDslSchema.SCHEMA.properties().stream()
                .map(DslPropertySpec::name)
                .collect(Collectors.toList());

        assertThat(names).containsExactly("testDirectory", "basePackage", "failOnViolation", "maxAllowedViolations",
                "ignoreFailures", "failOnDuplicateRules", "useBuiltInHexagonalRulePack",
                "rulesDisabled", "hexagonalArchitecture");
    }

    @Test
    @DisplayName("hexagonalArchitectureShouldBeTheOnlyContainerProperty")
    void hexagonalArchitectureShouldBeTheOnlyContainerProperty() {
        List<String> containerNames = ArchitectureValidatorDslSchema.SCHEMA.properties().stream()
                .filter(property -> property.kind() == DslPropertyKind.CONTAINER)
                .map(DslPropertySpec::name)
                .collect(Collectors.toList());

        assertThat(containerNames).containsExactly("hexagonalArchitecture");
    }

    @Test
    @DisplayName("hexagonalArchitectureStubShouldContainRealDefaultValuesNotCommentedOutExamples")
    void hexagonalArchitectureStubShouldContainRealDefaultValuesNotCommentedOutExamples() {
        DslPropertySpec hexagonal = ArchitectureValidatorDslSchema.SCHEMA.properties().stream()
                .filter(property -> property.name().equals("hexagonalArchitecture"))
                .findFirst()
                .orElseThrow();

        assertThat(hexagonal.containerStub()).doesNotContain("//");
        assertThat(hexagonal.containerStub()).contains("namingConventionsEnabled = false");
    }
}
