package com.arc_e_tect.gradle.trackerlens.contract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against {@code DASHBOARD-THEMING.adoc} drifting out of sync with {@link ContractRule}: the
 * documentation is prose, hand-maintained, but every selector it lists as part of the contract must
 * come straight from the enum, and every rule in the enum must be documented.
 */
@DisplayName("DASHBOARD-THEMING.adoc consistency with ContractRule")
class ContractDocumentationConsistencyTest {

    // Matches an AsciiDoc table cell that is a lone backtick-quoted selector on its own line, e.g.
    // "|`.dashboard`" - i.e. the Selector column of the Rules table, and nothing else in the
    // document (the Cardinality/Meaning columns never start with a backtick immediately after the
    // cell delimiter, and prose elsewhere in the file never starts a line with "|`" at all).
    private static final Pattern TABLE_SELECTOR_CELL = Pattern.compile("(?m)^\\|`([^`]+)`\\s*$");

    @Test
    @DisplayName("everyContractRuleSelectorShouldAppearInTheDocumentedRulesTable")
    void everyContractRuleSelectorShouldAppearInTheDocumentedRulesTable() throws IOException {
        String documentation = readDocumentation();

        for (ContractRule rule : ContractRule.values()) {
            assertThat(documentation).contains("`" + rule.selector() + "`");
        }
    }

    @Test
    @DisplayName("documentedRulesTableShouldNotListASelectorNotBackedByAContractRule")
    void documentedRulesTableShouldNotListASelectorNotBackedByAContractRule() throws IOException {
        String documentation = readDocumentation();

        Set<String> knownSelectors = Arrays.stream(ContractRule.values())
                .map(ContractRule::selector)
                .collect(Collectors.toSet());

        List<String> documentedSelectors = extractDocumentedSelectors(documentation);

        // Guards the extraction itself: a documentedSelectors list that came back empty means the
        // pattern above has stopped matching the table's actual markup (e.g. after a future
        // reformat), which would make the allMatch(...) assertion below vacuously true and useless.
        assertThat(documentedSelectors).isNotEmpty();
        assertThat(documentedSelectors).allMatch(knownSelectors::contains);
    }

    private List<String> extractDocumentedSelectors(String documentation) {
        Matcher matcher = TABLE_SELECTOR_CELL.matcher(documentation);
        List<String> documentedSelectors = new ArrayList<>();
        while (matcher.find()) {
            documentedSelectors.add(matcher.group(1));
        }
        return documentedSelectors;
    }

    private String readDocumentation() throws IOException {
        File file = new File("DASHBOARD-THEMING.adoc");
        assertThat(file).as("DASHBOARD-THEMING.adoc must exist at the module root").exists();
        return Files.readString(file.toPath());
    }
}
