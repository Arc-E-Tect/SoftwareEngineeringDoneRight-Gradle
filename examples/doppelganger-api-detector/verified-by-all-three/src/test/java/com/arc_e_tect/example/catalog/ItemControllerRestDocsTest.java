package com.arc_e_tect.example.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
    void listItems() throws Exception {
        mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andDo(document("list-items"));
    }
}
