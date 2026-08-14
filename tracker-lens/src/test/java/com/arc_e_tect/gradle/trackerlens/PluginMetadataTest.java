package com.arc_e_tect.gradle.trackerlens;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PluginMetadata")
class PluginMetadataTest {

    @Test
    @DisplayName("pluginVersionShouldReadTheStampedVersionFromTheBundledResource")
    void pluginVersionShouldReadTheStampedVersionFromTheBundledResource() {
        String version = PluginMetadata.pluginVersion();

        // Not asserting an exact value - it's whatever gradle.properties currently declares (or
        // whatever the release workflow stamped it to) - only that processResources actually
        // expanded the ${pluginVersion} placeholder rather than leaving it un-substituted.
        assertThat(version).isNotEqualTo("${pluginVersion}");
    }
}
