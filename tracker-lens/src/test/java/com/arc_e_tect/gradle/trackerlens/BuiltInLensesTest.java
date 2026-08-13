package com.arc_e_tect.gradle.trackerlens;

import com.arc_e_tect.gradle.trackerlens.lens.Lens;
import com.arc_e_tect.gradle.trackerlens.lens.LensScanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards properties of this plugin's own bundled built-in lenses that aren't otherwise covered by
 * {@link com.arc_e_tect.gradle.trackerlens.lens.LensScannerTest} (which exercises the scanning
 * mechanism against fixtures, not the real bundled resources).
 */
@DisplayName("Built-in lenses")
class BuiltInLensesTest {

    private static final Pattern RED_OR_GREEN_KEYWORD = Pattern.compile("\\b(red|green)\\b", Pattern.CASE_INSENSITIVE);

    @Test
    @DisplayName("bundledResourcesShouldContainAllThreeBuiltInLenses")
    void bundledResourcesShouldContainAllThreeBuiltInLenses() throws URISyntaxException {
        List<Lens> lenses = new LensScanner().scan(builtInLensRoot());

        assertThat(lenses).extracting(Lens::id).containsExactlyInAnyOrder("light-lens", "dark-lens", "high-contrast-lens");
    }

    @Test
    @DisplayName("highContrastLensShouldNotUseRedOrGreenAnywhereSoColorBlindUsersCanDistinguishItsElements")
    void highContrastLensShouldNotUseRedOrGreenAnywhereSoColorBlindUsersCanDistinguishItsElements() throws URISyntaxException {
        Lens highContrastLens = new LensScanner().scan(builtInLensRoot()).stream()
                .filter(lens -> lens.id().equals("high-contrast-lens"))
                .findFirst()
                .orElseThrow();
        String css = new String(highContrastLens.content(), StandardCharsets.UTF_8);
        // Strip comments first: the file's own explanatory header comment necessarily discusses
        // "red" and "green" by name (it's explaining why neither is used) - only actual
        // declarations matter here.
        String declarationsOnly = css.replaceAll("(?s)/\\*.*?\\*/", "");

        assertThat(RED_OR_GREEN_KEYWORD.matcher(declarationsOnly).find()).isFalse();
    }

    private File builtInLensRoot() throws URISyntaxException {
        String marker = "META-INF/arc-e-tect/tracker-lens/lenses/light-lens.css";
        URL resource = getClass().getClassLoader().getResource(marker);
        assertThat(resource).as("bundled built-in lens resource must be on the test classpath").isNotNull();
        String urlString = resource.toString();
        return new File(new URI(urlString.substring(0, urlString.length() - marker.length())));
    }
}
