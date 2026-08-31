package com.arc_e_tect.gradle.doppelganger.scan;

import com.arc_e_tect.gradle.detector.core.model.PathTemplates;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the path portion of an OpenAPI root document's first {@code servers[].url} entry, e.g.
 * {@code servers: [{url: "http://localhost:9011/user-account-service"}]} resolves to
 * {@code "/user-account-service"}.
 *
 * <p>A verification source that captures the literal path used against a real, running server
 * (as opposed to one relative to a controller's own {@code @RequestMapping}, such as a REST
 * Assured request built with {@code RestAssured.basePath} cleared) naturally includes this
 * server-url path - typically a reverse-proxy or servlet context path - in what it captures, even
 * though neither the OpenAPI {@code paths:} keys nor the {@code @RestController} mappings
 * themselves ever declare it. {@link RestDocsScanner} strips it, when configured, before
 * comparing captured paths against declared-and-implemented endpoints.</p>
 */
public final class OpenApiServerBasePath {

    private static final Pattern OPENAPI_32_YAML_PATTERN =
            Pattern.compile("(?m)^\\s*openapi\\s*:\\s*['\"]?(3\\.2(?:\\.\\d+)?)['\"]?\\s*$");

    private static final Pattern OPENAPI_32_JSON_PATTERN =
            Pattern.compile("(\"openapi\"\\s*:\\s*\")3\\.2(?:\\.\\d+)?(\")");

    private OpenApiServerBasePath() {}

    /**
     * Resolves {@code rootDocument}'s first server URL's path component, normalised via
     * {@link PathTemplates#normalize(String)}.
     *
     * @param rootDocument the root OpenAPI document
     * @return the normalised base path (e.g. {@code "/user-account-service"}), or {@code ""} when the
     *         document declares no servers, the first server's URL has a blank or root path, or
     *         the document cannot be parsed
     */
    public static String resolve(File rootDocument) {
        OpenAPI openApi = parse(rootDocument);
        if (openApi == null) {
            return "";
        }
        List<Server> servers = openApi.getServers();
        if (servers == null || servers.isEmpty()) {
            return "";
        }
        String url = servers.get(0).getUrl();
        if (url == null || url.isBlank()) {
            return "";
        }
        String path = pathOf(url);
        if (path == null || path.isBlank() || path.equals("/")) {
            return "";
        }
        return PathTemplates.normalize(path);
    }

    /**
     * Parses {@code rootDocument}, falling back to a version-patched copy when the document
     * declares OpenAPI 3.2 - a version swagger-parser cannot read directly - the same
     * compatibility workaround {@code OpenApiEndpointCollector} applies for the same reason.
     * Returns {@code null}, rather than throwing, on any parse failure: an unparseable document is
     * already reported by the main endpoint-collection phase that runs first, so this method only
     * needs to degrade to "no base path to strip".
     */
    private static OpenAPI parse(File rootDocument) {
        ParseOptions options = new ParseOptions();
        options.setResolve(false);
        OpenAPI openApi = readLocation(rootDocument, options);
        if (openApi == null && declaresOpenApi32(rootDocument.toPath())) {
            openApi = parseWithOpenApi31Compatibility(rootDocument, options);
        }
        return openApi;
    }

    private static OpenAPI readLocation(File document, ParseOptions options) {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(document.getAbsolutePath(), null, options);
        return result.getOpenAPI();
    }

    private static OpenAPI parseWithOpenApi31Compatibility(File rootDocument, ParseOptions options) {
        Path rootPath = rootDocument.toPath();
        try {
            String original = Files.readString(rootPath);
            String normalized = normalizeOpenApi32To31(original);
            if (normalized.equals(original)) {
                return readLocation(rootDocument, options);
            }

            Path parent = rootPath.getParent();
            String prefix = rootDocument.getName() + ".doppelganger-server-base-path-";
            Path tempFile = Files.createTempFile(parent, prefix, ".yaml");
            try {
                Files.writeString(tempFile, normalized);
                return readLocation(tempFile.toFile(), options);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean declaresOpenApi32(Path rootDocument) {
        try {
            String content = Files.readString(rootDocument);
            return OPENAPI_32_YAML_PATTERN.matcher(content).find()
                    || OPENAPI_32_JSON_PATTERN.matcher(content).find();
        } catch (IOException e) {
            return false;
        }
    }

    private static String normalizeOpenApi32To31(String content) {
        Matcher yamlMatcher = OPENAPI_32_YAML_PATTERN.matcher(content);
        String rewritten = yamlMatcher.replaceFirst("openapi: 3.1.0");
        Matcher jsonMatcher = OPENAPI_32_JSON_PATTERN.matcher(rewritten);
        return jsonMatcher.replaceFirst("$13.1.0$2");
    }

    private static String pathOf(String url) {
        try {
            return new URI(url).getPath();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
