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
}
