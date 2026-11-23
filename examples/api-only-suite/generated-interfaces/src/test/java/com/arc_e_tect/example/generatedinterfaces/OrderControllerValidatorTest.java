package com.arc_e_tect.example.generatedinterfaces;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static io.restassured.RestAssured.given;

/**
 * Calls both operations over real HTTP and validates each request and response against the
 * contract - the evidence Doppelganger API Detector looks for.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderControllerValidatorTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    void listOrders() {
        given()
                .filter(new OpenApiValidationFilter("src/main/resources/openapi/openapi.yaml"))
                .port(port)
                .when()
                .get("/v1/orders")
                .then()
                .statusCode(200);
    }

    @Test
    void getOrder() {
        given()
                .filter(new OpenApiValidationFilter("src/main/resources/openapi/openapi.yaml"))
                .port(port)
                .when()
                .get("/v1/orders/{id}", 1)
                .then()
                .statusCode(200);
    }
}
