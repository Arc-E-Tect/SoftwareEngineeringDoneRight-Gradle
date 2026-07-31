package com.arc_e_tect.gradle.shadow.openapi;

import com.arc_e_tect.gradle.shadow.model.HttpVerb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("OpenApiEndpointCollector")
class OpenApiEndpointCollectorTest {

    private final OpenApiEndpointCollector collector = new OpenApiEndpointCollector();

    @Test
    @DisplayName("collects every verb + path pair from a single-file document")
    void collectsFromSingleFile() {
        List<DescribedEndpoint> endpoints = collector.collect(resource("openapi/single-file/openapi.yaml"));

        assertThat(endpoints)
                .extracting(DescribedEndpoint::verb, DescribedEndpoint::path)
                .containsExactlyInAnyOrder(
                        tuple(HttpVerb.GET, "/users"),
                        tuple(HttpVerb.POST, "/users"),
                        tuple(HttpVerb.GET, "/users/{id}"),
                        tuple(HttpVerb.DELETE, "/users/{id}"));
    }

    @Test
    @DisplayName("follows a relative $ref to an external path-item document")
    void followsRelativeRef() {
        List<DescribedEndpoint> endpoints = collector.collect(resource("openapi/with-ref/openapi.yaml"));

        assertThat(endpoints)
                .extracting(DescribedEndpoint::verb, DescribedEndpoint::path)
                .containsExactlyInAnyOrder(
                        tuple(HttpVerb.GET, "/users"),
                        tuple(HttpVerb.POST, "/users"),
                        tuple(HttpVerb.GET, "/users/{id}"));
    }

    @Test
    @DisplayName("throws when the document cannot be parsed")
    void throwsForUnparsableDocument(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir)
            throws Exception {
        File garbage = new File(tempDir.toFile(), "not-openapi.yaml");
        java.nio.file.Files.writeString(garbage.toPath(), "not: [valid, openapi: {{{");

        assertThatThrownBy(() -> collector.collect(garbage))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("failed to parse OpenAPI document");
    }

    private static File resource(String name) {
        URL url = OpenApiEndpointCollectorTest.class.getClassLoader().getResource(name);
        if (url == null) {
            throw new IllegalStateException("Resource not found on classpath: " + name);
        }
        return new File(url.getFile());
    }
}
