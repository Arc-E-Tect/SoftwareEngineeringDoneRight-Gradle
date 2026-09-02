package com.arc_e_tect.gradle.trackerlens;

import com.arc_e_tect.gradle.dslupdater.DslPropertyKind;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TrackerLensDslSchema")
class TrackerLensDslSchemaTest {

    @Test
    @DisplayName("schemaShouldTargetTheTrackerLensBlock")
    void schemaShouldTargetTheTrackerLensBlock() {
        assertThat(TrackerLensDslSchema.SCHEMA.blockName()).isEqualTo(TrackerLensExtension.NAME);
    }

    @Test
    @DisplayName("schemaShouldListOnlyThePropertiesWithARealDefault")
    void schemaShouldListOnlyThePropertiesWithARealDefault() {
        List<String> names = TrackerLensDslSchema.SCHEMA.properties().stream()
                .map(DslPropertySpec::name)
                .collect(Collectors.toList());

        assertThat(names).containsExactly("outputDir", "dashboardName", "version", "trackers");
    }

    @Test
    @DisplayName("trackersShouldBeTheOnlyContainerProperty")
    void trackersShouldBeTheOnlyContainerProperty() {
        List<String> containerNames = TrackerLensDslSchema.SCHEMA.properties().stream()
                .filter(property -> property.kind() == DslPropertyKind.CONTAINER)
                .map(DslPropertySpec::name)
                .collect(Collectors.toList());

        assertThat(containerNames).containsExactly("trackers");
    }
}
