package com.arc_e_tect.gradle.trackerlens.contract;

import org.gradle.api.GradleException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Enforces every {@link ContractRule} against a generated dashboard file.
 *
 * <p>This validator never inspects text content - only the structural presence of the classes and
 * {@code data-*} attributes each {@link ContractRule} declares, per the "surrounding text is not
 * part of the contract" rule described on {@link ContractRule} itself.</p>
 */
public class LensContractValidator {

    /** Creates a new {@code LensContractValidator}. */
    public LensContractValidator() {}

    /**
     * Validates {@code html} against every {@link ContractRule}.
     *
     * @param html the dashboard HTML file to validate
     * @return every violation found, in {@link ContractRule} declaration order; empty when
     *         {@code html} conforms to the contract
     */
    public List<Violation> validate(File html) {
        Document document;
        try {
            document = Jsoup.parse(html, "UTF-8");
        } catch (IOException e) {
            throw new GradleException("trackerLens: failed to parse dashboard for contract validation: " + html, e);
        }

        Elements trackers = document.select(ContractRule.TRACKER_SECTION.selector());
        int trackerCount = trackers.size();

        List<Violation> violations = new ArrayList<>();
        for (ContractRule rule : ContractRule.values()) {
            checkRule(document, trackers, trackerCount, rule, violations);
        }
        return violations;
    }

    private void checkRule(Document document, Elements trackers, int trackerCount, ContractRule rule,
            List<Violation> violations) {
        Cardinality cardinality = rule.cardinality();
        if (cardinality instanceof Cardinality.SameCountAsRule sameCountAsRule) {
            checkSameCountAsRule(document, rule, sameCountAsRule.rule(), violations);
            return;
        }

        Cardinality.Fixed fixed = (Cardinality.Fixed) cardinality;
        switch (fixed) {
            case EXACTLY_ONE -> checkWholeDocumentCount(document, rule, violations, count -> count == 1,
                    "expected exactly one match, found %d");
            case AT_LEAST_ONE -> checkWholeDocumentCount(document, rule, violations, count -> count >= 1,
                    "expected at least one match, found %d");
            case AT_MOST_ONE -> checkWholeDocumentCount(document, rule, violations, count -> count <= 1,
                    "expected at most one match, found %d");
            case AT_LEAST_ONE_PER_TRACKER -> checkPerTracker(trackers, rule, violations, count -> count >= 1,
                    "expected at least one match, found %d");
            case AT_MOST_ONE_PER_TRACKER -> checkPerTracker(trackers, rule, violations, count -> count <= 1,
                    "expected at most one match, found %d");
            case SAME_COUNT_AS_TRACKERS -> checkSameCountAsTrackers(document, trackerCount, rule, violations);
        }
    }

    private void checkWholeDocumentCount(Document document, ContractRule rule, List<Violation> violations,
            java.util.function.IntPredicate satisfied, String messageFormat) {
        int count = document.select(rule.selector()).size();
        if (!satisfied.test(count)) {
            violations.add(new Violation(rule, String.format(messageFormat, count)));
        }
    }

    private void checkPerTracker(Elements trackers, ContractRule rule, List<Violation> violations,
            java.util.function.IntPredicate satisfied, String messageFormat) {
        for (Element tracker : trackers) {
            int count = tracker.select(rule.selector()).size();
            if (!satisfied.test(count)) {
                String trackerId = tracker.attr("data-tracker");
                violations.add(new Violation(rule,
                        "tracker '" + trackerId + "': " + String.format(messageFormat, count)));
            }
        }
    }

    private void checkSameCountAsTrackers(Document document, int trackerCount, ContractRule rule,
            List<Violation> violations) {
        int count = document.select(rule.selector()).size();
        if (count != trackerCount) {
            violations.add(new Violation(rule,
                    "expected " + trackerCount + " match(es) (one per tracker), found " + count));
        }
    }

    private void checkSameCountAsRule(Document document, ContractRule rule, ContractRule other,
            List<Violation> violations) {
        int count = document.select(rule.selector()).size();
        int otherCount = document.select(other.selector()).size();
        if (count != otherCount) {
            violations.add(new Violation(rule,
                    "expected " + otherCount + " match(es) (same as " + other.name() + "), found " + count));
        }
    }
}
