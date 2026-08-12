package com.arc_e_tect.gradle.doppelganger.detect;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A pluggable source of "this endpoint's contract is verified" evidence.
 *
 * <p>An endpoint is considered verified as soon as <strong>any one</strong> source reports
 * evidence for it, not all of them - teams may adopt Spring RestDocs, the Atlassian OpenAPI
 * request validator, and Spring Cloud Contract incrementally, in any combination, so the sources
 * are never required to agree.</p>
 *
 * <p>Each implementation owns its own recursive directory walk and file-type filtering, since the
 * three built-in sources scan different file types under different root directories.</p>
 */
public interface ContractVerificationSource {

    /**
     * Scans {@code rootDir} recursively and returns every endpoint this source found
     * verification evidence for.
     *
     * <p>The returned {@link Endpoint#declaringClass()} and {@link Endpoint#methodSignature()}
     * identify the test (or contract file) that supplied the evidence, not a production
     * {@code @RestController}.</p>
     *
     * @param rootDir the directory to scan recursively; scanning a non-existent or non-directory
     *                path returns an empty list rather than failing
     * @return possibly-empty list of verified endpoints, never {@code null}
     * @throws IOException if a file under {@code rootDir} cannot be read
     */
    List<Endpoint> scan(File rootDir) throws IOException;
}
