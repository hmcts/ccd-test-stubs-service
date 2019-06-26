package uk.gov.hmcts.reform.ccd.test.stubs.service.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class StubResponseControllerTest {

    @Autowired
    private transient MockMvc mockMvc;

    @DisplayName("Should return wiremock stub response with 200")
    @Test
    public void forwardAllRequestEndpoint() throws Exception {
        mockMvc.perform(post("/aat/about_to_start").characterEncoding("UTF-8"))
            .andExpect(status().isOk());
    }

}
