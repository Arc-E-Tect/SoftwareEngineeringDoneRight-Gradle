package com.arc_e_tect.example.users;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    // The request path is not a string literal here - it is injected by Spring from
    // application.properties via @Value("${users.by-username}"). Configuring propertyFiles in
    // build.gradle lets the Doppelganger plugin resolve this field the same way and recognise it
    // as verification evidence.
    @Value("${users.by-username}")
    private String usersByUsernamePath;

    @Test
    void getUserByUsername() throws Exception {
        mockMvc.perform(get(usersByUsernamePath, "alice"))
                .andExpect(status().isOk())
                .andDo(document("get-user-by-username"));
    }
}
