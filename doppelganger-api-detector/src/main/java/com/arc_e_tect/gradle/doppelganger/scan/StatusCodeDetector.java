package com.arc_e_tect.gradle.doppelganger.scan;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Best-effort detection of the HTTP status code a test method asserts, from the same
 * {@code List<MethodCallExpr>} a {@link ContractVerificationSource} AST scanner already collects
 * for that method - not exhaustive, since a test can assert a response's status in ways this
 * cannot recognise (a variable, a helper method, a custom matcher); such tests still count towards
 * an endpoint's overall contract test count, they simply contribute no evidence to any specific
 * response code's count.
 *
 * <p>Recognises three independent shapes, matched by simple method name only, the same way the
 * scanners that use this class already match request-builder calls:</p>
 * <ul>
 *     <li>MockMvc: {@code status().isOk()} / {@code status().isNotFound()} / ... (a fixed set of
 *     well-known {@code org.springframework.test.web.servlet.result.StatusResultMatchers} method
 *     names), or the numeric {@code status().is(404)}.</li>
 *     <li>REST Assured: {@code .statusCode(404)}.</li>
 *     <li>WebTestClient: {@code .expectStatus().isNotFound()}, or the numeric
 *     {@code .expectStatus().isEqualTo(404)}.</li>
 * </ul>
 */
public final class StatusCodeDetector {

    private static final Map<String, String> MOCKMVC_STATUS_METHODS = Map.ofEntries(
            Map.entry("isOk", "200"),
            Map.entry("isCreated", "201"),
            Map.entry("isAccepted", "202"),
            Map.entry("isNoContent", "204"),
            Map.entry("isMovedPermanently", "301"),
            Map.entry("isFound", "302"),
            Map.entry("isNotModified", "304"),
            Map.entry("isBadRequest", "400"),
            Map.entry("isUnauthorized", "401"),
            Map.entry("isForbidden", "403"),
            Map.entry("isNotFound", "404"),
            Map.entry("isMethodNotAllowed", "405"),
            Map.entry("isConflict", "409"),
            Map.entry("isGone", "410"),
            Map.entry("isUnprocessableEntity", "422"),
            Map.entry("isTooManyRequests", "429"),
            Map.entry("isInternalServerError", "500"),
            Map.entry("isNotImplemented", "501"),
            Map.entry("isBadGateway", "502"),
            Map.entry("isServiceUnavailable", "503"));

    private StatusCodeDetector() {}

    /**
     * Detects the status code asserted anywhere among {@code calls}, or {@link Optional#empty()}
     * when none of the recognised shapes are present.
     *
     * @param calls every method call expression found in a test method's body
     * @return the detected status code, e.g. {@code "404"}, or {@link Optional#empty()}
     */
    public static Optional<String> detect(List<MethodCallExpr> calls) {
        for (MethodCallExpr call : calls) {
            Optional<String> fromMockMvcOrWebTestClient = fromStatusScopedCall(call);
            if (fromMockMvcOrWebTestClient.isPresent()) {
                return fromMockMvcOrWebTestClient;
            }
            Optional<String> fromRestAssured = fromStatusCodeCall(call);
            if (fromRestAssured.isPresent()) {
                return fromRestAssured;
            }
        }
        return Optional.empty();
    }

    /**
     * {@code status().isXxx()} / {@code status().is(NNN)} (MockMvc) or
     * {@code expectStatus().isXxx()} / {@code expectStatus().isEqualTo(NNN)} (WebTestClient) - the
     * call is scoped on a no-argument call named {@code status} or {@code expectStatus}.
     */
    private static Optional<String> fromStatusScopedCall(MethodCallExpr call) {
        boolean statusScoped = call.getScope()
                .filter(MethodCallExpr.class::isInstance)
                .map(MethodCallExpr.class::cast)
                .filter(scope -> scope.getArguments().isEmpty()
                        && (scope.getNameAsString().equals("status") || scope.getNameAsString().equals("expectStatus")))
                .isPresent();
        if (!statusScoped) {
            return Optional.empty();
        }
        String name = call.getNameAsString();
        if (MOCKMVC_STATUS_METHODS.containsKey(name)) {
            return Optional.of(MOCKMVC_STATUS_METHODS.get(name));
        }
        if ((name.equals("is") || name.equals("isEqualTo")) && !call.getArguments().isEmpty()) {
            return numericLiteral(call.getArgument(0));
        }
        return Optional.empty();
    }

    /** {@code .statusCode(NNN)} (REST Assured). */
    private static Optional<String> fromStatusCodeCall(MethodCallExpr call) {
        if (!call.getNameAsString().equals("statusCode") || call.getArguments().isEmpty()) {
            return Optional.empty();
        }
        return numericLiteral(call.getArgument(0));
    }

    private static Optional<String> numericLiteral(Expression argument) {
        return argument.isIntegerLiteralExpr()
                ? Optional.of(argument.asIntegerLiteralExpr().asNumber().toString())
                : Optional.empty();
    }
}
