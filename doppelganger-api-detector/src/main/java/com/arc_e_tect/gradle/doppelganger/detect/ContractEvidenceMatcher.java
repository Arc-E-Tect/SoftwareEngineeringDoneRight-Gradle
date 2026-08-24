package com.arc_e_tect.gradle.doppelganger.detect;

import com.arc_e_tect.gradle.detector.core.Described;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.model.PathMatcher;

/**
 * Whether one piece of verification evidence counts as verifying a candidate endpoint - shared by
 * {@link DoppelgangerApiFinder} and {@link ResponseCoverageAnalyzer} so both apply exactly the same
 * matching rule.
 *
 * <p>Deliberately not {@code api-detector-core}'s {@code ContractSetOperations}: see
 * {@link DoppelgangerApiFinder}'s own javadoc for why a concrete, resolved verified path (e.g. from
 * Spring Cloud Contract) needs {@link PathMatcher#matchesConcrete(String, String)} rather than the
 * symmetric, template-vs-template matching {@code ContractSetOperations} performs.</p>
 */
final class ContractEvidenceMatcher {

    private ContractEvidenceMatcher() {}

    /**
     * Returns whether {@code verifiedEntry} counts as verification evidence for {@code candidate}:
     * same verb - or either side carries {@link HttpVerb#ANY} - and {@code verifiedEntry}'s path is
     * a valid instance of {@code candidate}'s path template.
     *
     * @param verifiedEntry the verb + path of a piece of verification evidence
     * @param candidate     the verb + path template of the candidate endpoint being checked
     * @return {@code true} when {@code verifiedEntry} verifies {@code candidate}
     */
    static boolean verifies(Described verifiedEntry, Described candidate) {
        boolean verbMatches = candidate.verb() == HttpVerb.ANY
                || verifiedEntry.verb() == HttpVerb.ANY
                || candidate.verb() == verifiedEntry.verb();
        return verbMatches && PathMatcher.matchesConcrete(verifiedEntry.path(), candidate.path());
    }
}
