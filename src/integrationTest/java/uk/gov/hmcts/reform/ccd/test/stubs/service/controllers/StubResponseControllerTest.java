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

    public static final String AM_ROLE_ASSIGNMENTS_URL = "/am/role-assignments/actors/";
    public static final String ID_HEARING_VIEWER = "18187443-7b29-47c1-bcda-6f099705e4bc";
    public static final String ID_HEARING_MANAGER = "fe051b3d-7751-4b44-ba97-b00b0e508b2e";
    public static final String ID_HMC_SUPERUSER = "3c0b4d4e-32af-4998-890a-e82850360de4";
    public static final String ID_LISTED_HEARING_VIEWER = "a8ae0153-8c4b-4f09-bd8d-2a93db2a0520";
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

    @DisplayName("getRoleAssignments for hearing superuser should return 200 for valid requests")
    @Test
    void getRoleAssignmentsForHearingSuperuserShouldReturn200ForValidRequests() throws Exception {
        mockMvc.perform(get(AM_ROLE_ASSIGNMENTS_URL + ID_HMC_SUPERUSER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleAssignmentResponse[0].roleName")
                .value("hearing-superuser"))
            .andExpect(jsonPath("$.roleAssignmentResponse[0].actorId")
                .value(ID_HMC_SUPERUSER))
            .andExpect(jsonPath("$.roleAssignmentResponse[3].roleName")
                .value("listed-hearing-viewer"))
            .andExpect(jsonPath("$.roleAssignmentResponse[3].actorId")
                .value(ID_HMC_SUPERUSER))
            .andExpect(jsonPath("$.roleAssignmentResponse[2].roleName")
                .value("hearing-viewer"))
            .andExpect(jsonPath("$.roleAssignmentResponse[2].actorId")
                .value(ID_HMC_SUPERUSER))
            .andExpect(jsonPath("$.roleAssignmentResponse[1].roleName")
                .value("hearing-manager"))
            .andExpect(jsonPath("$.roleAssignmentResponse[1].actorId")
                .value(ID_HMC_SUPERUSER));
    }

    @DisplayName("getRoleAssignments for hearing manager should return 200 for valid requests")
    @Test
    void getRoleAssignmentsForHearingManagerShouldReturn200ForValidRequests() throws Exception {
        mockMvc.perform(get(AM_ROLE_ASSIGNMENTS_URL + ID_HEARING_MANAGER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleAssignmentResponse[0].roleName")
                .value("hearing-manager"))
            .andExpect(jsonPath("$.roleAssignmentResponse[0].actorId")
                .value(ID_HEARING_MANAGER));
    }

    @DisplayName("getRoleAssignments for hearing viewer should return 200 for valid requests")
    @Test
    void getRoleAssignmentsForHearingViewerShouldReturn200ForValidRequests() throws Exception {
        mockMvc.perform(get(AM_ROLE_ASSIGNMENTS_URL + ID_HEARING_VIEWER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleAssignmentResponse[0].roleName")
                .value("hearing-viewer"))
            .andExpect(jsonPath("$.roleAssignmentResponse[0].actorId")
            .value(ID_HEARING_VIEWER));
    }

    @DisplayName("getRoleAssignments for listed hearing viewr should return 200 for valid requests")
    @Test
    void getRoleAssignmentsForListedHearingViewerShouldReturn200ForValidRequests() throws Exception {
        mockMvc.perform(get(AM_ROLE_ASSIGNMENTS_URL  + ID_LISTED_HEARING_VIEWER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleAssignmentResponse[0].roleName")
                .value("listed-hearing-viewer"))
            .andExpect(jsonPath("$.roleAssignmentResponse[0].actorId")
                .value(ID_LISTED_HEARING_VIEWER))
        ;
    }

    @DisplayName("get Create Case should return 200 for valid requests")
    @Test
    void getCreateCaseShouldReturn200ForValidRequests() throws Exception {
        mockMvc.perform(get(EVENT_TRIGGERS_CASE_TYPE_URL)
                .contentType(APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.event_id").exists());
    }

    @DisplayName("post Role Assignments should return 200")
    @Test
    void postRoleAssignmentsHmcSuperUser1() throws Exception {
        mockMvc.perform(post("/am/role-assignments")
                .characterEncoding(StandardCharsets.UTF_8)
                .content("\"actorId\":\"cc80b61d-02b8-4bf8-b0d2-5fda88649ab4\",\"roleType\":\"ORGANISATION\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleAssignmentResponse.roleRequest.id").value("0c6f56f5-4457-485e-a0de-828e6dfa1e22"));
    }

    @DisplayName("post Role Assignments should return 200")
    @Test
    void postRoleAssignmentsHearingManager() throws Exception {
        mockMvc.perform(post("/am/role-assignments")
                .characterEncoding(StandardCharsets.UTF_8)
                .content("\"actorId\":\"59a73933-9c23-4180-80fa-76ee07fa20cd\",\"roleType\":\"ORGANISATION\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleAssignmentResponse.roleRequest.id").value("0c6f56f5-4457-485e-a0de-828e6dfa1e11"));
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
