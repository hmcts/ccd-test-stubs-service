package uk.gov.hmcts.reform.ccd.test.stubs.service.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StubResponseControllerTest {

    @Autowired
    private transient MockMvc mockMvc;

    @DisplayName("Should return wiremock stub response with 200")
    @Test
    void forwardAllRequestEndpoint() throws Exception {
        mockMvc.perform(post("/case_type/aat/about_to_start").characterEncoding("UTF-8"))
            .andExpect(status().isOk());
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
}
