package uk.gov.hmcts.reform.ccd.test.stubs.service.servicepersistence;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/ccd-persistence")
public class ServicePersistenceController {

    private static final Logger LOG = LoggerFactory.getLogger(ServicePersistenceController.class);

    private static final String STUB_PROCESSOR_FIELD = "_stubProcessedBy";
    private static final String STUB_PROCESSOR_VALUE = "ccd-test-stubs-service";
    private static final String CASE_DETAILS_FIELD = "case_details";
    private static final String CASE_DATA_FIELD = "case_data";
    private static final String CASE_TYPE_ID_FIELD = "case_type_id";
    private static final String EVENT_DETAILS_FIELD = "event_details";
    private static final String EVENT_ID_FIELD = "event_id";
    private static final String CREATED_DATE_FIELD = "created_date";
    private static final String CONFLICT_EVENT_ID = "simulateDecentralisedConflict";
    private static final String VALIDATION_ERROR_FLAG = "TriggerValidationError";
    private static final String VALIDATION_ERROR_MESSAGE = "Simulated decentralised validation error";
    private static final String VALIDATION_WARNING_MESSAGE = "Simulated decentralised validation warning";

    private static final String DEFAULT_CASE_TYPE_ID = "FT_Decentralisation";
    private static final String DEFAULT_JURISDICTION = "BEFTA_MASTER";
    private static final String DEFAULT_STATE = "CaseCreated";
    private static final String DEFAULT_SECURITY_CLASSIFICATION = "PUBLIC";
    private static final String DEFAULT_EVENT_ID = "createCase";
    private static final String DEFAULT_EVENT_NAME = "Create a case";
    private static final String DEFAULT_TEXT_VALUE = "Decentralised case";
    private static final int DEFAULT_VERSION = 1;
    private static final int DEFAULT_REVISION = 1;
    private static final long BASE_EPOCH_MILLIS = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();
    private static final long TIMESTAMP_SPREAD_MILLIS = Duration.ofDays(365).toMillis();

    private final ObjectMapper mapper;

    public ServicePersistenceController(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostMapping("/cases")
    public ResponseEntity<ObjectNode> submitEvent(
        @RequestBody ObjectNode payload,
        @RequestHeader(value = "Idempotency-Key") String idempotencyKey
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        }

        ObjectNode caseDetailsPayload = payload.with(CASE_DETAILS_FIELD);
        ObjectNode caseDataNode = caseDetailsPayload.with(CASE_DATA_FIELD);
        long reference = caseDetailsPayload.path("id").asLong();

        if (caseDataNode.hasNonNull(VALIDATION_ERROR_FLAG)
            && "yes".equalsIgnoreCase(caseDataNode.get(VALIDATION_ERROR_FLAG).asText())) {
            LOG.info("Simulating decentralised validation error for case ref {}", reference);
            ObjectNode response = mapper.createObjectNode();
            response.put("ignore_warning", false);
            response.set("errors", mapper.createArrayNode().add(VALIDATION_ERROR_MESSAGE));
            response.set("warnings", mapper.createArrayNode().add(VALIDATION_WARNING_MESSAGE));
            return ResponseEntity.ok(response);
        }

        String eventId = payload.path(EVENT_DETAILS_FIELD).path(EVENT_ID_FIELD).asText();
        if (CONFLICT_EVENT_ID.equals(eventId)) {
            LOG.info("Simulating decentralised concurrency conflict for case ref {} (eventId={})",
                reference,
                eventId);
            ObjectNode conflictResponse = mapper.createObjectNode();
            conflictResponse.put("message", "Simulated decentralised concurrency conflict");
            return new ResponseEntity<>(conflictResponse, HttpStatus.CONFLICT);
        }

        ObjectNode response = buildCaseEnvelope(reference, caseDetailsPayload, caseDataNode);

        String eventIdForLog = eventId.isBlank() ? DEFAULT_EVENT_ID : eventId;
        LOG.info("ServicePersistenceStub processed event {} for case ref {} (idempotency={})",
            eventIdForLog,
            reference,
            idempotencyKey);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/cases")
    public List<ObjectNode> getCases(@RequestParam("case-refs") String rawCaseRefs) {
        return parseCaseReferences(rawCaseRefs).stream()
            .map(ref -> buildCaseEnvelope(ref, null, null))
            .toList();
    }

    @GetMapping("/cases/{caseRef}/history")
    public List<ObjectNode> getHistory(@PathVariable("caseRef") long caseReference) {
        return List.of(buildHistoryEntry(caseReference));
    }

    @GetMapping("/cases/{caseRef}/history/{eventId}")
    public ResponseEntity<ObjectNode> getHistoryEvent(
        @PathVariable("caseRef") long caseReference,
        @PathVariable("eventId") long eventId
    ) {
        ObjectNode historyEntry = buildHistoryEntry(caseReference);
        if (historyEntry.path("id").asLong() != eventId) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No event " + eventId + " for case " + caseReference);
        }
        return ResponseEntity.ok(historyEntry);
    }

    @PostMapping("/cases/{caseRef}/supplementary-data")
    public ObjectNode updateSupplementaryData(
        @PathVariable("caseRef") long caseReference,
        @RequestBody ObjectNode request
    ) {
        LOG.info("ServicePersistenceStub received supplementary-data update for {}: {}", caseReference, request);
        ObjectNode updates = request.with("supplementary_data_updates");
        ObjectNode supplementary = mapper.createObjectNode();

        if (updates.has("$set") && updates.get("$set").isObject()) {
            ObjectNode setUpdates = (ObjectNode) updates.get("$set");
            setUpdates.fields().forEachRemaining(entry ->
                supplementary.set(entry.getKey(), entry.getValue().deepCopy())
            );
        }
        if (updates.has("$inc") && updates.get("$inc").isObject()) {
            ObjectNode incUpdates = (ObjectNode) updates.get("$inc");
            incUpdates.fields().forEachRemaining(entry -> {
                long value = entry.getValue().asLong();
                long current = supplementary.path(entry.getKey()).asLong(0L);
                supplementary.put(entry.getKey(), current + value);
            });
        }

        supplementary.put("test_value", 10);
        ObjectNode response = mapper.createObjectNode();
        response.set("supplementary_data", supplementary);
        LOG.info("ServicePersistenceStub returning supplementary-data for {}: {}", caseReference, response);
        return response;
    }

    private ObjectNode buildCaseEnvelope(
        long reference,
        ObjectNode caseDetailsPayload,
        ObjectNode requestCaseData
    ) {
        ObjectNode response = mapper.createObjectNode();
        response.set(CASE_DETAILS_FIELD, buildCaseDetails(reference, caseDetailsPayload, requestCaseData));
        response.put("revision", DEFAULT_REVISION);
        response.put("ignore_warning", true);
        return response;
    }

    private ObjectNode buildCaseDetails(
        long reference,
        ObjectNode caseDetailsPayload,
        ObjectNode requestCaseData
    ) {
        ObjectNode caseDetails = mapper.createObjectNode();
        caseDetails.put("id", reference);
        caseDetails.put("version", DEFAULT_VERSION);
        caseDetails.put(CASE_TYPE_ID_FIELD, determineCaseType(caseDetailsPayload));
        caseDetails.put("jurisdiction", determineJurisdiction(caseDetailsPayload));
        caseDetails.put("state", determineState(caseDetailsPayload));
        caseDetails.put("security_classification", determineSecurity(caseDetailsPayload));

        String timestamp = deterministicTimestamp(reference);
        caseDetails.put(CREATED_DATE_FIELD, timestamp);
        caseDetails.put("last_modified", timestamp);
        caseDetails.put("last_state_modified_date", timestamp);

        if (caseDetailsPayload != null && caseDetailsPayload.hasNonNull("resolved_ttl")) {
            caseDetails.put("resolved_ttl", caseDetailsPayload.get("resolved_ttl").asText());
        }

        caseDetails.set(CASE_DATA_FIELD, buildCaseData(reference, requestCaseData));
        caseDetails.set("data_classification", mapper.createObjectNode());

        return caseDetails;
    }

    private ObjectNode buildCaseData(long reference, ObjectNode sourceCaseData) {
        ObjectNode caseData = mapper.createObjectNode();
        if (sourceCaseData != null) {
            sourceCaseData.fields().forEachRemaining(entry ->
                caseData.set(entry.getKey(), entry.getValue().deepCopy())
            );
        } else {
            caseData.put("TextField", DEFAULT_TEXT_VALUE);
        }
        caseData.put(STUB_PROCESSOR_FIELD, STUB_PROCESSOR_VALUE);
        return caseData;
    }

    private ObjectNode buildHistoryEntry(long reference) {
        ObjectNode wrapper = mapper.createObjectNode();
        long auditId = deterministicAuditId(reference);
        wrapper.put("id", auditId);
        wrapper.put("case_reference", reference);
        wrapper.set("event", buildEvent(reference));
        return wrapper;
    }

    private ObjectNode buildEvent(long reference) {
        ObjectNode event = mapper.createObjectNode();
        event.put("id", DEFAULT_EVENT_ID);
        event.put("event_name", DEFAULT_EVENT_NAME);
        event.put("summary", (String) null);
        event.put("description", (String) null);
        event.put(CASE_TYPE_ID_FIELD, DEFAULT_CASE_TYPE_ID);
        event.put(CREATED_DATE_FIELD, deterministicTimestamp(reference));
        event.put("state_id", DEFAULT_STATE);
        event.set("data", buildCaseData(reference, null));
        event.put("case_type_version", DEFAULT_VERSION);
        event.put("security_classification", DEFAULT_SECURITY_CLASSIFICATION);
        return event;
    }

    private String determineCaseType(ObjectNode caseDetailsPayload) {
        if (caseDetailsPayload != null && caseDetailsPayload.hasNonNull(CASE_TYPE_ID_FIELD)) {
            return caseDetailsPayload.get(CASE_TYPE_ID_FIELD).asText();
        }
        return DEFAULT_CASE_TYPE_ID;
    }

    private String determineJurisdiction(ObjectNode caseDetailsPayload) {
        if (caseDetailsPayload != null && caseDetailsPayload.hasNonNull("jurisdiction")) {
            return caseDetailsPayload.get("jurisdiction").asText();
        }
        return DEFAULT_JURISDICTION;
    }

    private String determineState(ObjectNode caseDetailsPayload) {
        if (caseDetailsPayload != null && caseDetailsPayload.hasNonNull("state")) {
            return caseDetailsPayload.get("state").asText();
        }
        return DEFAULT_STATE;
    }

    private String determineSecurity(ObjectNode caseDetailsPayload) {
        if (caseDetailsPayload != null && caseDetailsPayload.hasNonNull("security_classification")) {
            return caseDetailsPayload.get("security_classification").asText();
        }
        return DEFAULT_SECURITY_CLASSIFICATION;
    }

    private String deterministicTimestamp(long reference) {
        long offset = Math.floorMod(reference, TIMESTAMP_SPREAD_MILLIS);
        return Instant.ofEpochMilli(BASE_EPOCH_MILLIS + offset).atOffset(ZoneOffset.UTC).toString();
    }

    private long deterministicAuditId(long reference) {
        return Math.abs(reference % 1_000L) + 1;
    }

    private List<Long> parseCaseReferences(String raw) {
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(token -> !token.isEmpty())
            .map(Long::valueOf)
            .toList();
    }
}
