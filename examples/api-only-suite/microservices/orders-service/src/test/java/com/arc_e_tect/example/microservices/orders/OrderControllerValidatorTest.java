package com.arc_e_tect.example.microservices.orders;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderControllerValidatorTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    void getOrder() {
        given()
                .filter(new OpenApiValidationFilter("src/main/resources/openapi/openapi.yaml"))
                .port(port)
                .when()
                .get("/orders/{id}", 1)
                .then()
                .statusCode(200);
    }
}
