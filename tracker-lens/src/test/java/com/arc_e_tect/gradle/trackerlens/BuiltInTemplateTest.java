package com.arc_e_tect.gradle.trackerlens;

import com.arc_e_tect.gradle.trackerlens.lens.Template;
import com.arc_e_tect.gradle.trackerlens.lens.TemplateScanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards properties of this plugin's own bundled built-in dashboard template that aren't otherwise
 * covered by {@link com.arc_e_tect.gradle.trackerlens.lens.TemplateScannerTest} (which exercises
 * the scanning mechanism against fixtures, not the real bundled resource) or
 * {@link com.arc_e_tect.gradle.trackerlens.dashboard.DashboardHtmlWriterTest} (which exercises
 * rendering, not discoverability).
 */
@DisplayName("Built-in template")
class BuiltInTemplateTest {

    @Test
    @DisplayName("bundledResourcesShouldContainTheBuiltInTemplateDiscoverableAsDefault")
    void bundledResourcesShouldContainTheBuiltInTemplateDiscoverableAsDefault() throws URISyntaxException {
        List<Template> templates = new TemplateScanner().scan(builtInTemplateRoot());

        assertThat(templates).extracting(Template::id).containsExactly("default");
    }

    private File builtInTemplateRoot() throws URISyntaxException {
        String marker = "META-INF/arc-e-tect/tracker-lens/templates/default.html";
        URL resource = getClass().getClassLoader().getResource(marker);
        assertThat(resource).as("bundled built-in template resource must be on the test classpath").isNotNull();
        String urlString = resource.toString();
        return new File(new URI(urlString.substring(0, urlString.length() - marker.length())));
    }
}
