package uk.gov.hmcts.reform.ccd.test.stubs.service.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class StubResponseControllerTest {

    @Autowired
    private transient MockMvc mockMvc;

    @DisplayName("Should return wiremock stub response with 200")
    @Test
    void forwardAllRequestEndpoint() throws Exception {
        mockMvc.perform(post("/callback_about_to_start").characterEncoding("UTF-8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.CallbackText").value("test"));
    }

    @DisplayName("Should return http client error for invalid get operation")
    @Test
    void invalidGetOperation() throws Exception {
        mockMvc.perform(get("/case_type/aat/invalid_endpoint").characterEncoding("UTF-8"))
            .andExpect(status().is4xxClientError());
    }

    @DisplayName("Should return http client error for invalid put operation")
    @Test
    void invalidPutOperation() throws Exception {
        mockMvc.perform(put("/case_type/aat/invalid_endpoint").characterEncoding("UTF-8"))
            .andExpect(status().is4xxClientError());
    }

    @DisplayName("Should return http client error for invalid delete operation")
    @Test
    void invalidDeleteOperation() throws Exception {
        mockMvc.perform(delete("/case_type/aat/invalid_endpoint").characterEncoding("UTF-8"))
            .andExpect(status().is4xxClientError());
    }

    @DisplayName("Should redirect when calling login endpoint")
    @Test
    void testLoginRedirects() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().is3xxRedirection());
    }

    @DisplayName("Should return random jw token with response code 200")
    @Test
    void testTokenEndpoint() throws Exception {
        mockMvc.perform(post("/oauth2/token").characterEncoding("UTF-8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expires_in").value("28800"));
    }

    @DisplayName("Should return user info with response code 200")
    @Test
    void testUserInfoEndpoint() throws Exception {
        mockMvc.perform(get("/o/userinfo").characterEncoding("UTF-8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("auto.test.cnp@gmail.com"));
    }
}
