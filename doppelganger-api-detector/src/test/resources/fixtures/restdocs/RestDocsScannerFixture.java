package com.example.fixture;

public class RestDocsScannerFixture {

    void getOrder() throws Exception {
        mockMvc.perform(get("/orders/{id}", 1))
                .andExpect(status().isOk())
                .andDo(document("get-order"));
    }

    void createOrder() throws Exception {
        mockMvc.perform(post("/orders"))
                .andExpect(status().isCreated())
                .andDo(document("create-order"));
    }

    void getItemWebTestClient() {
        webTestClient.get()
                .uri("/items/{id}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(document("get-item-web-test-client"));
    }

    void createItemWebTestClient() {
        webTestClient.post()
                .uri("/items")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .consumeWith(document("create-item-web-test-client"));
    }

    void listItemsUndocumentedWebTestClient() {
        webTestClient.get()
                .uri("/items")
                .exchange()
                .expectStatus().isOk();
    }

    void deleteItemDynamicPathWebTestClient() {
        webTestClient.delete()
                .uri(buildPath())
                .exchange()
                .expectStatus().isNoContent()
                .expectBody()
                .consumeWith(document("delete-item-web-test-client"));
    }

    void listOrdersUndocumented() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk());
    }

    void deleteOrderDynamicPath() throws Exception {
        mockMvc.perform(delete(buildPath()))
                .andDo(document("delete-order"));
    }

    void getCallOutsidePerform() throws Exception {
        String path = get("/not/a/request/builder/call");
        andDo(document("irrelevant"));
    }

    void getItemRestAssured() {
        given(documentationSpec)
                .filter(document("get-item-rest-assured"))
                .when()
                .get("/items/{id}");
    }

    void createItemRestAssured() {
        given(documentationSpec)
                .filter(document("create-item-rest-assured"))
                .when()
                .post("/items");
    }

    void listItemsUndocumentedRestAssured() {
        given(documentationSpec)
                .when()
                .get("/items");
    }

    void deleteItemDynamicPathRestAssured() {
        given(documentationSpec)
                .filter(document("delete-item-rest-assured"))
                .when()
                .delete(buildPath());
    }

    void getCallOutsideWhenRestAssured() {
        String path = get("/not/a/request/builder/call");
        filter(document("irrelevant"));
    }

    void getItemWithServerBasePathRestAssured() {
        given(documentationSpec)
                .filter(document("get-item-base-path-rest-assured"))
                .when()
                .get("crm-service/items/{id}");
    }
}
