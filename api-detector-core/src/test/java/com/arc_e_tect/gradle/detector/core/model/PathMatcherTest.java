package com.arc_e_tect.gradle.detector.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PathMatcher")
class PathMatcherTest {

    @Test
    @DisplayName("matches identical literal paths")
    void matchesIdenticalLiteralPaths() {
        assertThat(PathMatcher.matches("/api/users", "/api/users")).isTrue();
    }

    @Test
    @DisplayName("does not match different literal paths")
    void doesNotMatchDifferentLiteralPaths() {
        assertThat(PathMatcher.matches("/api/users", "/api/accounts")).isFalse();
    }

    @Test
    @DisplayName("matches placeholders regardless of variable name")
    void matchesPlaceholdersRegardlessOfName() {
        assertThat(PathMatcher.matches("/users/{id}", "/users/{userId}")).isTrue();
    }

    @Test
    @DisplayName("does not match a placeholder against a literal segment")
    void doesNotMatchPlaceholderAgainstLiteral() {
        assertThat(PathMatcher.matches("/users/{id}", "/users/123")).isFalse();
    }

    @Test
    @DisplayName("does not match paths with a different number of segments")
    void doesNotMatchDifferentSegmentCounts() {
        assertThat(PathMatcher.matches("/users/{id}", "/users/{id}/profile")).isFalse();
    }

    @Test
    @DisplayName("matches the root path against itself")
    void matchesRootPath() {
        assertThat(PathMatcher.matches("/", "/")).isTrue();
    }
}
