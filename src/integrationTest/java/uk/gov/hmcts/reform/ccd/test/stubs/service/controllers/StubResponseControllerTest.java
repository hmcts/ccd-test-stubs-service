package uk.gov.hmcts.reform.ccd.test.stubs.service.controllers;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class StubResponseControllerTest {

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

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
        mockMvc.perform(get("/login")
            .param("redirect_uri", "http://localhost:3451/oauth2"))
            .andExpect(status().is3xxRedirection());
    }

    @DisplayName("Should return random jw token with response code 200")
    @Test
    void testTokenEndpoint() throws Exception {
        mockMvc.perform(post("/oauth2/token").characterEncoding("UTF-8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expires_in").value("14400000"));
    }

    @DisplayName("Should return random jw token with response code 200")
    @Test
    void testOpenIdTokenEndpoint() throws Exception {
        mockMvc.perform(post("/o/token").characterEncoding("UTF-8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expires_in").value("14400000"));
    }

    @DisplayName("Should return random jw token with response code 200")
    @Test
    void testJwksEndpoint() throws Exception {
        mockMvc.perform(get("/o/jwks").characterEncoding("UTF-8"))
            .andExpect(status().isOk());
    }

    @DisplayName("Should return user info with response code 200")
    @Test
    void testUserInfoEndpoint() throws Exception {
        mockMvc.perform(get("/o/userinfo").characterEncoding("UTF-8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("auto.test.cnp@gmail.com"));
    }

    @DisplayName("Should be able to configure at runtime stubbed IDAM user info")
    @Test
    void testChangeStubbedUserInfo() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/o/userinfo").characterEncoding("UTF-8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("auto.test.cnp@gmail.com"))
            .andExpect(jsonPath("$.roles", not(hasItem("role1"))))
            .andReturn()
            .getResponse();

        String oldUserInfo = response.getContentAsString();

        String newEmail = "someemail@gmail.com";
        List<String> newRoles = Lists.newArrayList("role1");
        IdamUserInfo userInfo = createUserInfo(newEmail, newRoles);

        mockMvc.perform(post("/idam-user")
            .contentType(APPLICATION_JSON_VALUE)
            .content(asJson(userInfo)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/o/userinfo").characterEncoding("UTF-8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(newEmail))
            .andExpect(jsonPath("$.roles", hasItem("role1")));

        mockMvc.perform(post("/idam-user")
            .contentType(APPLICATION_JSON_VALUE)
            .content(oldUserInfo))
            .andExpect(status().isOk());
    }

    private String asJson(IdamUserInfo userInfo) throws JsonProcessingException {
        return mapper.writeValueAsString(userInfo);
    }

    private IdamUserInfo createUserInfo(String email, List<String> roles) {
        IdamUserInfo userInfo = new IdamUserInfo();
        userInfo.setUid("33");
        userInfo.setEmail(email);
        userInfo.setName("name");
        userInfo.setFamilyName("familyName");
        userInfo.setGivenName("givenName");
        userInfo.setSub("sub");
        userInfo.setRoles(roles);
        return userInfo;
    }
}
