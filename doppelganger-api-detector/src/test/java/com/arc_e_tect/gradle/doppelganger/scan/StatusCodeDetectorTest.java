package com.arc_e_tect.gradle.doppelganger.scan;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StatusCodeDetector")
class StatusCodeDetectorTest {

    @Test
    @DisplayName("detects MockMvc status().isOk()")
    void detectsMockMvcIsOk() {
        assertThat(detect("mockMvc.perform(get(\"/x\")).andExpect(status().isOk());")).contains("200");
    }

    @Test
    @DisplayName("detects MockMvc status().isNotFound()")
    void detectsMockMvcIsNotFound() {
        assertThat(detect("mockMvc.perform(get(\"/x\")).andExpect(status().isNotFound());")).contains("404");
    }

    @Test
    @DisplayName("detects MockMvc status().is(NNN) numeric form")
    void detectsMockMvcNumericStatus() {
        assertThat(detect("mockMvc.perform(get(\"/x\")).andExpect(status().is(422));")).contains("422");
    }

    @Test
    @DisplayName("detects REST Assured .statusCode(NNN)")
    void detectsRestAssuredStatusCode() {
        assertThat(detect("given().when().get(\"/x\").then().statusCode(503);")).contains("503");
    }

    @Test
    @DisplayName("detects WebTestClient .expectStatus().isOk()")
    void detectsWebTestClientIsOk() {
        assertThat(detect("webTestClient.get().uri(\"/x\").exchange().expectStatus().isOk();")).contains("200");
    }

    @Test
    @DisplayName("detects WebTestClient .expectStatus().isEqualTo(NNN) numeric form")
    void detectsWebTestClientNumericStatus() {
        assertThat(detect("webTestClient.get().uri(\"/x\").exchange().expectStatus().isEqualTo(418);"))
                .contains("418");
    }

    @Test
    @DisplayName("returns empty when no recognised status assertion is present")
    void returnsEmptyWhenNoStatusAsserted() {
        assertThat(detect("given().when().get(\"/x\");")).isEmpty();
    }

    @Test
    @DisplayName("returns empty for a status assertion via a variable, not a literal")
    void returnsEmptyForNonLiteralStatus() {
        assertThat(detect("given().when().get(\"/x\").then().statusCode(expectedStatus);")).isEmpty();
    }

    @Test
    @DisplayName("returns empty for an empty call list")
    void returnsEmptyForEmptyCallList() {
        assertThat(StatusCodeDetector.detect(List.of())).isEmpty();
    }

    private Optional<String> detect(String statement) {
        BlockStmt block = StaticJavaParser.parseBlock("{ " + statement + " }");
        List<MethodCallExpr> calls = block.findAll(MethodCallExpr.class);
        return StatusCodeDetector.detect(calls);
    }
}
