package com.arc_e_tect.example.users;

import com.arc_e_tect.example.endpoints.ApiEndpoints;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureRestDocs
class UserControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getUserByUsername() throws Exception {
        // The request path is not a string literal here - it is resolved via a static helper
        // method (ApiEndpoints.get(key)) backed by api-endpoints.properties. Configuring
        // propertyFiles + pathResolverHelperMethods in build.gradle lets the Doppelganger plugin
        // resolve this call the same way and recognise it as verification evidence.
        mockMvc.perform(get(ApiEndpoints.get("users.by-username"), "alice"))
                .andExpect(status().isOk())
                .andDo(document("get-user-by-username"));
    }
}
