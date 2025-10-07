package uk.gov.hmcts.reform.ccd.test.stubs.service.servicepersistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServicePersistenceController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ServicePersistenceControllerWebMvcTest {

    private static final String STUB_MARKER_FIELD = "_stubProcessedBy";
    private static final String STUB_MARKER_VALUE = "ccd-test-stubs-service";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void shouldPersistCaseAndExposeHistory() throws Exception {
        long caseReference = 111_222_333_444_555L;
        String ttl = "2025-01-01T00:00:00Z";

        ObjectNode firstPayload = buildPayload(caseReference, "createCase");
        firstPayload.put("resolved_ttl", ttl);

        mockMvc.perform(post("/ccd-persistence/cases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstPayload.toString()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.revision").value(1))
            .andExpect(jsonPath("$.case_details.revision").value(1))
            .andExpect(jsonPath("$.case_details.resolved_ttl").value(ttl))
            .andExpect(jsonPath("$.case_details.case_data." + STUB_MARKER_FIELD).value(STUB_MARKER_VALUE));

        String casesJson = mockMvc.perform(get("/ccd-persistence/cases")
                .param("case-refs", String.valueOf(caseReference)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode cases = mapper.readTree(casesJson);
        assertThat(cases).hasSize(1);
        JsonNode caseDetails = cases.get(0).path("case_details");
        assertThat(caseDetails.path("reference").asLong()).isEqualTo(caseReference);
        assertThat(caseDetails.path("revision").asInt()).isEqualTo(1);
        assertThat(caseDetails.path("case_data").path(STUB_MARKER_FIELD).asText()).isEqualTo(STUB_MARKER_VALUE);

        String historyJson = mockMvc.perform(get("/ccd-persistence/cases/{caseRef}/history", caseReference))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode history = mapper.readTree(historyJson);
        assertThat(history).hasSize(1);
        JsonNode firstEvent = history.get(0);
        assertThat(firstEvent.path("case_reference").asLong()).isEqualTo(caseReference);
        assertThat(firstEvent.path("event").path("event_id").asText()).isEqualTo("createCase");
        assertThat(firstEvent.path("id").asLong()).isEqualTo(1L);
        assertThat(firstEvent.path("event").path("data").path(STUB_MARKER_FIELD).asText())
            .isEqualTo(STUB_MARKER_VALUE);
    }

    @Test
    void shouldIncrementRevisionForExistingCase() throws Exception {
        long caseReference = 200_300_400_500_600L;

        mockMvc.perform(post("/ccd-persistence/cases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildPayload(caseReference, "create").toString()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.revision").value(1))
            .andExpect(jsonPath("$.case_details.case_data." + STUB_MARKER_FIELD).value(STUB_MARKER_VALUE));

        mockMvc.perform(post("/ccd-persistence/cases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildPayload(caseReference, "update").toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.revision").value(2))
            .andExpect(jsonPath("$.case_details.revision").value(2))
            .andExpect(jsonPath("$.case_details.case_data." + STUB_MARKER_FIELD).value(STUB_MARKER_VALUE));

        String historyJson = mockMvc.perform(get("/ccd-persistence/cases/{caseRef}/history", caseReference))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode history = mapper.readTree(historyJson);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).path("id").asLong()).isEqualTo(1L);
        assertThat(history.get(1).path("id").asLong()).isEqualTo(2L);
        assertThat(history.get(1).path("event").path("event_id").asText()).isEqualTo("update");
        assertThat(history.get(1).path("event").path("data").path(STUB_MARKER_FIELD).asText())
            .isEqualTo(STUB_MARKER_VALUE);
    }

    @Test
    void shouldReturnNotFoundWhenHistoryEventMissing() throws Exception {
        long caseReference = 314_159_265_358_979L;

        mockMvc.perform(post("/ccd-persistence/cases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildPayload(caseReference, "create").toString()))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/ccd-persistence/cases/{caseRef}/history/{eventId}", caseReference, 999L))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldEchoSupplementaryDataPayload() throws Exception {
        long caseReference = 789_101_112_131_415L;
        ObjectNode supplementary = mapper.createObjectNode();
        supplementary.put("key", "value");
        ObjectNode request = mapper.createObjectNode();
        request.set("supplementary_data", supplementary);

        mockMvc.perform(post("/ccd-persistence/cases/{caseRef}/supplementary-data", caseReference)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.supplementary_data.key").value("value"));
    }

    private ObjectNode buildPayload(long caseReference, String eventId) {
        ObjectNode caseDetails = mapper.createObjectNode();
        caseDetails.put("reference", caseReference);
        caseDetails.put("case_type_id", "TestCaseType");
        caseDetails.put("state", "Created");
        caseDetails.set("data", mapper.createObjectNode().put("field", "value"));

        ObjectNode eventDetails = mapper.createObjectNode();
        eventDetails.put("event_id", eventId);
        eventDetails.put("event_name", eventId + "Name");

        ObjectNode payload = mapper.createObjectNode();
        payload.set("case_details", caseDetails);
        payload.set("event_details", eventDetails);
        return payload;
    }
}
