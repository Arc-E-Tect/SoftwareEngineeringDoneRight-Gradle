package com.arc_e_tect.gradle.zombie.openapi;

import com.arc_e_tect.gradle.zombie.model.HttpVerb;

/**
 * A single HTTP verb + path template pair described by an OpenAPI operation.
 *
 * @param verb the HTTP verb the OpenAPI operation is documented under; never {@link HttpVerb#ANY}
 * @param path the OpenAPI path template, e.g. {@code "/users/{id}"}
 */
public record DescribedEndpoint(HttpVerb verb, String path) {
}
