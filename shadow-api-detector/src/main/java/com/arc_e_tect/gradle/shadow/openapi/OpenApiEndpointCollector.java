package com.arc_e_tect.gradle.shadow.openapi;

import com.arc_e_tect.gradle.shadow.model.HttpVerb;
import com.arc_e_tect.gradle.shadow.model.PathTemplates;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a root OpenAPI document - resolving every {@code $ref} to other documents relative to
 * it - and collects every HTTP verb + path template pair it describes.
 */
public class OpenApiEndpointCollector {

    /** Creates a new {@code OpenApiEndpointCollector}. */
    public OpenApiEndpointCollector() {}

    /**
     * Parses {@code rootDocument} and every document it links to (relative {@code $ref}s are
     * resolved automatically), and returns the verb + path template pair described by every
     * operation found.
     *
     * @param rootDocument the root OpenAPI document (JSON or YAML)
     * @return possibly-empty list of described endpoints, never {@code null}
     * @throws IllegalStateException if the document cannot be parsed
     */
    public List<DescribedEndpoint> collect(File rootDocument) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        SwaggerParseResult result =
                new OpenAPIParser().readLocation(rootDocument.getAbsolutePath(), null, options);
        OpenAPI openApi = result.getOpenAPI();
        if (openApi == null) {
            String messages = result.getMessages() == null ? "" : String.join("; ", result.getMessages());
            throw new IllegalStateException(
                    "shadowApiDetector: failed to parse OpenAPI document " + rootDocument + ": " + messages);
        }

        List<DescribedEndpoint> endpoints = new ArrayList<>();
        Paths paths = openApi.getPaths();
        if (paths == null) {
            return endpoints;
        }
        paths.forEach((path, item) -> item.readOperationsMap().forEach((method, operation) ->
                endpoints.add(new DescribedEndpoint(
                        HttpVerb.valueOf(method.name()), PathTemplates.normalize(path)))));
        return endpoints;
    }
}
