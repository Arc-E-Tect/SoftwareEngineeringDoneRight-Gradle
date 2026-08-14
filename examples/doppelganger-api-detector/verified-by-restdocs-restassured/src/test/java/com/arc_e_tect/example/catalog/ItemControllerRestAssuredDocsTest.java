package com.arc_e_tect.example.catalog;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;

import static io.restassured.RestAssured.given;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.document;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.documentationConfiguration;

@ExtendWith(RestDocumentationExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ItemControllerRestAssuredDocsTest {

    @LocalServerPort
    private int port;

    private RequestSpecification spec;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        RestAssured.port = port;
        spec = new RequestSpecBuilder()
                .addFilter(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void getItem() {
        // No leading "/" and no basePath configured anywhere in spec - this hits a real, running
        // server (webEnvironment = RANDOM_PORT) whose servlet context path is "catalog-service"
        // (see application.yml), the same shape spring-restdocs-restassured tests take in
        // production: the path literal includes a server-url prefix ItemController's own mapping
        // (and openapi.yaml's paths: keys) never declare.
        given(spec)
                .filter(document("get-item"))
                .basePath("")
                .when()
                .get("catalog-service/items/{id}", 1)
                .then()
                .statusCode(200);
    }
}
