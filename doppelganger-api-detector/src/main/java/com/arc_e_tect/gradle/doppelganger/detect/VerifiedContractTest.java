package com.arc_e_tect.gradle.doppelganger.detect;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;

/**
 * A single piece of contract verification evidence, together with the HTTP status code it was
 * detected to assert - the richer counterpart of a plain {@link Endpoint} produced by
 * {@link ContractVerificationSource#scanWithStatusCodes(java.io.File)}.
 *
 * @param endpoint    the verified endpoint, identifying both what it verifies (verb + path) and,
 *                    via {@link Endpoint#declaringClass()}/{@link Endpoint#methodSignature()}, the
 *                    test (or contract file) that supplied the evidence
 * @param statusCode  the HTTP status code this test was detected to assert, e.g. {@code "404"}, or
 *                    {@code null} when the verification source found the test but could not
 *                    determine what status it asserts
 */
public record VerifiedContractTest(Endpoint endpoint, String statusCode) {
}
