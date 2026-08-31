package com.arc_e_tect.gradle.doppelganger.scan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenApiServerBasePath")
class OpenApiServerBasePathTest {

    @Test
    @DisplayName("resolves the normalised path portion of the first server's url")
    void resolvesPathFromFirstServerUrl(@TempDir Path tempDir) throws Exception {
        File document = writeDocument(tempDir, """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 1.0.0
                servers:
                  - url: http://localhost:9011/user-account-service
                paths: {}
                """);

        assertThat(OpenApiServerBasePath.resolve(document)).isEqualTo("/user-account-service");
    }

    @Test
    @DisplayName("returns an empty string when the document declares no servers")
    void returnsEmptyStringWhenNoServers(@TempDir Path tempDir) throws Exception {
        File document = writeDocument(tempDir, """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 1.0.0
                paths: {}
                """);

        assertThat(OpenApiServerBasePath.resolve(document)).isEmpty();
    }

    @Test
    @DisplayName("returns an empty string when the first server's url has no path")
    void returnsEmptyStringWhenServerUrlHasNoPath(@TempDir Path tempDir) throws Exception {
        File document = writeDocument(tempDir, """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 1.0.0
                servers:
                  - url: http://localhost:9011
                paths: {}
                """);

        assertThat(OpenApiServerBasePath.resolve(document)).isEmpty();
    }

    @Test
    @DisplayName("returns an empty string when the first server's url is just the root path")
    void returnsEmptyStringWhenServerUrlIsRootPath(@TempDir Path tempDir) throws Exception {
        File document = writeDocument(tempDir, """
                openapi: 3.0.0
                info:
                  title: Test API
                  version: 1.0.0
                servers:
                  - url: /
                paths: {}
                """);

        assertThat(OpenApiServerBasePath.resolve(document)).isEmpty();
    }

    @Test
    @DisplayName("resolves the server base path from an OpenAPI 3.2 document, which swagger-parser can't read directly")
    void resolvesPathFromOpenApi32Document(@TempDir Path tempDir) throws Exception {
        File document = writeDocument(tempDir, """
                openapi: 3.2.0
                info:
                  title: Test API
                  version: 1.0.0
                servers:
                  - url: http://localhost:9011/user-account-service
                paths: {}
                """);

        assertThat(OpenApiServerBasePath.resolve(document)).isEqualTo("/user-account-service");
    }

    @Test
    @DisplayName("returns an empty string when the document cannot be parsed")
    void returnsEmptyStringWhenDocumentCannotBeParsed(@TempDir Path tempDir) throws Exception {
        File document = writeDocument(tempDir, "this is not { valid yaml or json ][");

        assertThat(OpenApiServerBasePath.resolve(document)).isEmpty();
    }

    private File writeDocument(Path tempDir, String content) throws Exception {
        File document = new File(tempDir.toFile(), "openapi.yaml");
        Files.writeString(document.toPath(), content);
        return document;
    }
}
