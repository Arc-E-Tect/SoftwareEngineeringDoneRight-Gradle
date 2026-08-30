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

    private static final String ITEMS_PATH = "/items";

    void listItemsWithConstantPath() {
        given()
                .filter(new OpenApiValidationFilter(spec))
                .when()
                .get(ITEMS_PATH)
                .then()
                .statusCode(200);
    }

    void listItemsWithDynamicPath() {
        given()
                .filter(new OpenApiValidationFilter(spec))
                .when()
                .get(buildPath())
                .then()
                .statusCode(200);
    }

    private static String buildPath() {
        return "/dynamic";
    }

    void getUserByUsernamePropertyHelperMethod() {
        given()
                .filter(new OpenApiValidationFilter(spec))
                .when()
                .get(ApiEndpoints.path("users.by-username"))
                .then()
                .statusCode(200);
    }

    @Value("${users.by-username}")
    private String usersByUsernamePath;

    void getUserByUsernameValueAnnotation() {
        given()
                .filter(new OpenApiValidationFilter(spec))
                .when()
                .get(usersByUsernamePath)
                .then()
                .statusCode(200);
    }

    static class ApiEndpoints {
        static String path(String key) {
            return null;
        }
    }

    @interface Value {
        String value();
    }
}
