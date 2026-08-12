package com.arc_e_tect.example.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
@AutoConfigureRestDocs
class ItemControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getItem() throws Exception {
        mockMvc.perform(get("/items/{id}", 1))
                .andExpect(status().isOk())
                .andDo(document("get-item"));
    }
}
