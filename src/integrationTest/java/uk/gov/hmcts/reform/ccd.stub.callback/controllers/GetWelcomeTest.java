package uk.gov.hmcts.reform.ccd.stub.callback.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
public class GetWelcomeTest {

    @Autowired
    private transient MockMvc mockMvc;

    @DisplayName("Should welcome upon root request with 200 response code")
    @Test
    public void welcomeRootEndpoint() throws Exception {
        //TODO

        //MvcResult response = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();

        //assertThat(response.getResponse().getContentAsString()).startsWith("Welcome");
    }
}
