package com.example.fixture;

public class OpenApiRequestValidatorScannerFixture {

    void getOrderWithFilter() {
        given()
                .filter(new OpenApiValidationFilter(spec))
                .when()
                .get("/orders/{id}", 1)
                .then()
                .statusCode(200);
    }

    void createOrderWithDirectValidation() {
        Response response = given().when().post("/orders").thenReturn();
        validator.validateRequest(response.request());
        validator.validateResponse(response);
    }

    void listOrdersWithoutValidation() {
        given().when().get("/orders").then().statusCode(200);
    }

    void deleteOrderNotRestAssuredStyle() {
        validator.validateRequest(someRequest);
        client.delete("/orders/{id}", 1);
    }
}
