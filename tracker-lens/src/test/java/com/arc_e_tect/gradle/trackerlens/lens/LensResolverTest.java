package com.arc_e_tect.gradle.trackerlens.lens;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LensResolver")
class LensResolverTest {

    private final LensResolver resolver = new LensResolver();

    @Test
    @DisplayName("resolveShouldKeepCleanIdsWhenNoCollision")
    void resolveShouldKeepCleanIdsWhenNoCollision() {
        LensSource builtIn = new LensSource("built-in", List.of(
                new Lens("light-lens", bytes("light")), new Lens("dark-lens", bytes("dark"))));
        LensSource external = new LensSource("midnight-theme", List.of(new Lens("midnight", bytes("midnight"))));

        List<ResolvedLens> resolved = resolver.resolve(List.of(builtIn, external));

        assertThat(resolved).extracting(ResolvedLens::id)
                .containsExactly("light-lens", "dark-lens", "midnight");
    }

    @Test
    @DisplayName("resolveShouldAutoNamespaceExternalPackCollidingWithBuiltIn")
    void resolveShouldAutoNamespaceExternalPackCollidingWithBuiltIn() {
        LensSource builtIn = new LensSource("built-in", List.of(new Lens("dark-lens", bytes("built-in dark"))));
        LensSource external = new LensSource("midnight-theme", List.of(new Lens("dark-lens", bytes("external dark"))));

        List<ResolvedLens> resolved = resolver.resolve(List.of(builtIn, external));

        assertThat(resolved).extracting(ResolvedLens::id)
                .containsExactly("dark-lens", "dark-lens (midnight-theme)");
    }

    @Test
    @DisplayName("resolveShouldApplyTheSameAutoNamespacingBetweenTwoExternalPacksAsBetweenBuiltInAndExternal")
    void resolveShouldApplyTheSameAutoNamespacingBetweenTwoExternalPacksAsBetweenBuiltInAndExternal() {
        LensSource firstExternal = new LensSource("first-theme", List.of(new Lens("dark-lens", bytes("first"))));
        LensSource secondExternal = new LensSource("second-theme", List.of(new Lens("dark-lens", bytes("second"))));

        List<ResolvedLens> resolved = resolver.resolve(List.of(firstExternal, secondExternal));

        assertThat(resolved).extracting(ResolvedLens::id)
                .containsExactly("dark-lens", "dark-lens (second-theme)");
    }

    @Test
    @DisplayName("resolveShouldLetFirstListedSourceWinTheCleanIdRegardlessOfWhetherItIsBuiltIn")
    void resolveShouldLetFirstListedSourceWinTheCleanIdRegardlessOfWhetherItIsBuiltIn() {
        LensSource custom = new LensSource("custom", List.of(new Lens("custom-lens", bytes("custom"))));
        LensSource builtIn = new LensSource("built-in", List.of(new Lens("custom-lens", bytes("built-in"))));

        List<ResolvedLens> resolved = resolver.resolve(List.of(custom, builtIn));

        assertThat(resolved).extracting(ResolvedLens::id)
                .containsExactly("custom-lens", "custom-lens (built-in)");
    }

    private byte[] bytes(String value) {
        return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
