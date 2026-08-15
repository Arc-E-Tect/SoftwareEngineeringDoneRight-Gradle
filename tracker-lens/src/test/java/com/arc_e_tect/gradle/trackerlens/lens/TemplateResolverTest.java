package com.arc_e_tect.gradle.trackerlens.lens;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TemplateResolver")
class TemplateResolverTest {

    private final TemplateResolver resolver = new TemplateResolver();

    @Test
    @DisplayName("resolveShouldKeepCleanIdsWhenNoCollision")
    void resolveShouldKeepCleanIdsWhenNoCollision() {
        TemplateSource builtIn = new TemplateSource("built-in", List.of(new Template("default", bytes("default"))));
        TemplateSource external = new TemplateSource("venn-view-pack",
                List.of(new Template("venn-diagram-view", bytes("venn"))));

        List<ResolvedTemplate> resolved = resolver.resolve(List.of(builtIn, external));

        assertThat(resolved).extracting(ResolvedTemplate::id)
                .containsExactly("default", "venn-diagram-view");
    }

    @Test
    @DisplayName("resolveShouldAutoNamespaceExternalPackCollidingWithBuiltIn")
    void resolveShouldAutoNamespaceExternalPackCollidingWithBuiltIn() {
        TemplateSource builtIn = new TemplateSource("built-in", List.of(new Template("default", bytes("built-in"))));
        TemplateSource external = new TemplateSource("venn-view-pack",
                List.of(new Template("default", bytes("external"))));

        List<ResolvedTemplate> resolved = resolver.resolve(List.of(builtIn, external));

        assertThat(resolved).extracting(ResolvedTemplate::id)
                .containsExactly("default", "default (venn-view-pack)");
    }

    @Test
    @DisplayName("resolveShouldApplyTheSameAutoNamespacingBetweenTwoExternalPacksAsBetweenBuiltInAndExternal")
    void resolveShouldApplyTheSameAutoNamespacingBetweenTwoExternalPacksAsBetweenBuiltInAndExternal() {
        TemplateSource first = new TemplateSource("first-pack", List.of(new Template("story-view", bytes("first"))));
        TemplateSource second = new TemplateSource("second-pack", List.of(new Template("story-view", bytes("second"))));

        List<ResolvedTemplate> resolved = resolver.resolve(List.of(first, second));

        assertThat(resolved).extracting(ResolvedTemplate::id)
                .containsExactly("story-view", "story-view (second-pack)");
    }

    private byte[] bytes(String value) {
        return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
