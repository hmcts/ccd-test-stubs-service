package uk.gov.hmcts.reform.ccd.test.stubs.service.controllers;

import java.nio.charset.StandardCharsets;
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

    public static final String AM_ROLE_ASSIGNMENTS_URL = "/am/role-assignments";
    public static final String OAUTH_AUTHORIZE_URL = "/oauth2/authorize";
    public static final String EVENT_TRIGGERS_CASE_TYPE_URL = "/case-types/FT_CRUD/event-triggers/createCase";

    @DisplayName("Should return wiremock stub response with 200")
    @Test
    void forwardAllRequestEndpoint() throws Exception {
        mockMvc.perform(post("/callback_about_to_start").characterEncoding(StandardCharsets.UTF_8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.CallbackText").value("test"));
    }

    @DisplayName("Should return http client error for invalid get operation")
    @Test
    void invalidGetOperation() throws Exception {
        mockMvc.perform(get("/case_type/aat/invalid_endpoint").characterEncoding(StandardCharsets.UTF_8))
            .andExpect(status().is4xxClientError());
    }

    @DisplayName("Should return http client error for invalid put operation")
    @Test
    void invalidPutOperation() throws Exception {
        mockMvc.perform(put("/case_type/aat/invalid_endpoint").characterEncoding(StandardCharsets.UTF_8))
            .andExpect(status().is4xxClientError());
    }

    @DisplayName("Should return http client error for invalid delete operation")
    @Test
    void invalidDeleteOperation() throws Exception {
        mockMvc.perform(delete("/case_type/aat/invalid_endpoint").characterEncoding(StandardCharsets.UTF_8))
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
        mockMvc.perform(post("/oauth2/token").characterEncoding(StandardCharsets.UTF_8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expires_in").value("14400000"));
    }

    @DisplayName("Should return random jw token with response code 200")
    @Test
    void testOpenIdTokenEndpoint() throws Exception {
        mockMvc.perform(post("/o/token").characterEncoding(StandardCharsets.UTF_8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expires_in").value("14400000"));
    }

    @DisplayName("Should return random jw token with response code 200")
    @Test
    void testJwksEndpoint() throws Exception {
        mockMvc.perform(get("/o/jwks").characterEncoding(StandardCharsets.UTF_8))
            .andExpect(status().isOk());
    }

    @DisplayName("Should return user info with response code 200")
    @Test
    void testUserInfoEndpoint() throws Exception {
        mockMvc.perform(get("/o/userinfo").characterEncoding(StandardCharsets.UTF_8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("auto.test.cnp@gmail.com"));
    }

    @DisplayName("Should be able to configure at runtime stubbed IDAM user info")
    @Test
    void testChangeStubbedUserInfo() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/o/userinfo").characterEncoding(StandardCharsets.UTF_8))
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

        mockMvc.perform(get("/o/userinfo").characterEncoding(StandardCharsets.UTF_8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(newEmail))
            .andExpect(jsonPath("$.roles", hasItem("role1")));

        mockMvc.perform(post("/idam-user")
            .contentType(APPLICATION_JSON_VALUE)
            .content(oldUserInfo))
            .andExpect(status().isOk());
    }

    @DisplayName("postRoleAssignments should return 200 for valid requests")
    @Test
    void postRoleAssignmentsShouldReturn200ForValidRequests() throws Exception {
        mockMvc.perform(post(AM_ROLE_ASSIGNMENTS_URL)
                .contentType(APPLICATION_JSON_VALUE)
                .content("{\"actorId\":[\"3c0b4d4e-32af-4998-890a-e82850360de4\"],\"roleType\":[\"ORGANISATION\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleAssignmentResponse[0].roleName").value("hearing-manager"));
    }

    @DisplayName("postOauthAuthorize should return 200 for valid requests")
    @Test
    void postOauthAuthorizeShouldReturn200ForValidRequests() throws Exception {
        mockMvc.perform(post(OAUTH_AUTHORIZE_URL)
                .contentType(APPLICATION_JSON_VALUE)
                .content("{\"code\":[\"eyJ0eXAiOiJKV1QiLCJ6aXAiOiJOT05FIiwia2lkIjoiYi9PNk92VnYxK3krV2dySDVVaTlXVGlvTHQwPSIsImFsZyI6IlJTMjU2In0."
                    + "eyJzdWIiOiJtYXN0ZXIuY2FzZXdvcmtlckBnbWFpbC5jb20iLCJhdXRoX2xldmVsIjowLCJhdWRpdFRyYWNraW5nSWQiOiI4MWY3NGI0ZS00YjFkLTQy"
                    + "ZGYtYWFhNC0xZmU5ZGZiOGFhMDEiLCJpc3MiOiJodHRwOi8vZnItYW06ODA4MC9vcGVuYW0vb2F1dGgyL2htY3RzIiwidG9rZW5OYW1lIjoiYWNjZXNz"
                    + "X3Rva2VuIiwidG9rZW5fdHlwZSI6IkJlYXJlciIsImF1dGhHcmFudElkIjoiYTgzOTdlYjctYWE2ZS00ZDYxLWIyNWYtMzQ1MDNiMjU4OGQ5IiwiYXVk"
                    + "IjoiY2NkX2dhdGV3YXkiLCJuYmYiOjE3NDE3OTMwNjAsImdyYW50X3R5cGUiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJzY29wZSI6WyJvcGVuaWQiLCJw"
                    + "cm9maWxlIiwicm9sZXMiXSwiYXV0aF90aW1lIjoxNzQxNzkzMDU4MDAwLCJyZWFsbSI6Ii9obWN0cyIsImV4cCI6MTc0MTgyMTg2MCwiaWF0IjoxNzQx"
                    + "NzkzMDYwLCJleHBpcmVzX2luIjoyODgwMCwianRpIjoiNTUzNzE5ZGMtZjU4Ni00MTgzLTk4NTAtNjg5YzhjY2FkM2ZlIn0.D0PL9b2a3z9x7kxuco820"
                    + "_bjDxTIWK4ZYzJzv39IyX_iRqjLwIF_sINuXiLBhaFUDsnJ9wg-kYF4E1RAvqGbKzA17SHc60HV5T4PxdVNIfZD3xd0sd8Si24GRF2eCmq9GhNptvnGg"
                    + "1BSF8aelj4UoIijt9GL0lRSUCIYmiC0jdFOjTKPXVTg7zFa5z0oxhmd6JvuWFyvTm0JZKZtGLSXabaEpWwxbIXDnaOy0MEazhFC3yruwgAd7ptVXxRTu"
                    + "9u3uRY0f9qWVksf9xJxy53bfkdEjSQLzaXwZdKGFyWp0ueG0qdPX4COJK4NPL17O7beARJIcljF-z5OTtM5OWjCpA\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").exists());
    }

    @DisplayName("get Create Case should return 200 for valid requests")
    @Test
    void getCreateCaseShouldReturn200ForValidRequests() throws Exception {
        mockMvc.perform(get(EVENT_TRIGGERS_CASE_TYPE_URL)
                .contentType(APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.event_id").exists());
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
