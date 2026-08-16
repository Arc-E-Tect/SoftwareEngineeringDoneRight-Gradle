package com.arc_e_tect.example.microservices.users;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerValidatorTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    void getUser() {
        given()
                .filter(new OpenApiValidationFilter("src/main/resources/openapi/openapi.yaml"))
                .port(port)
                .when()
                .get("/users/{id}", 1)
                .then()
                .statusCode(200);
    }
}
