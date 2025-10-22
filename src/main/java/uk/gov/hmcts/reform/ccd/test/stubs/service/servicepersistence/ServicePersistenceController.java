package uk.gov.hmcts.reform.ccd.test.stubs.service.servicepersistence;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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

    private final ObjectMapper mapper;
    private final Cache<Long, CaseRecord> cases;
    private final AtomicLong auditSequence = new AtomicLong(1);

    public ServicePersistenceController(ObjectMapper mapper) {
        this.mapper = mapper;
        this.cases = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();
    }

    @PostMapping("/cases")
    public ResponseEntity<ObjectNode> submitEvent(
        @RequestBody ObjectNode payload,
        @RequestHeader(value = "Idempotency-Key") String idempotencyKey
    ) {
        requireHeader(idempotencyKey, "Idempotency-Key header is required");

        ObjectNode caseDetails = requireObjectNode(payload, CASE_DETAILS_FIELD);
        long reference = payload.path(CASE_DETAILS_FIELD).get("id").asLong();

        requireText(caseDetails, CASE_TYPE_ID_FIELD);
        requireText(caseDetails, "jurisdiction");
        ObjectNode caseDataNode = requireObjectNode(caseDetails, CASE_DATA_FIELD);

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

        CaseRecord existing = cases.getIfPresent(reference);
        final long revision = existing == null ? 1 : existing.revision + 1;

        caseDataNode.put(STUB_PROCESSOR_FIELD, STUB_PROCESSOR_VALUE);
        caseDetails.remove("data");
        caseDetails.put("version", revision);
        caseDetails.put("id", reference);

        applyTimestampsAndDefaults(caseDetails, existing);
        JsonNode resolvedTtl = payload.path("resolved_ttl");
        if (!resolvedTtl.isMissingNode() && !resolvedTtl.isNull()) {
            caseDetails.put("resolved_ttl", resolvedTtl.asText());
        }

        List<ObjectNode> history = existing == null
            ? new ArrayList<>()
            : new ArrayList<>(existing.history.stream().map(ObjectNode::deepCopy).toList());
        history.add(buildAuditEvent(reference, payload, caseDetails));

        CaseRecord caseRecord = new CaseRecord(caseDetails.deepCopy(), revision,
            history.stream().map(ObjectNode::deepCopy).toList());
        cases.put(reference, caseRecord);

        ObjectNode response = mapper.createObjectNode();
        response.set(CASE_DETAILS_FIELD, caseDetails);
        response.put("revision", revision);
        response.put("ignore_warning", true);

        boolean created = existing == null;
        String eventIdForLog = eventId.isBlank() ? "unknown" : eventId;
        LOG.info("ServicePersistenceStub processed event {} for case ref {} (idempotency={})",
            eventIdForLog,
            reference,
            idempotencyKey);

        HttpStatus status = created ? HttpStatus.CREATED : HttpStatus.OK;
        return new ResponseEntity<>(response, status);
    }

    @GetMapping("/cases")
    public List<ObjectNode> getCases(@RequestParam("case-refs") String rawCaseRefs) {
        List<Long> refs = parseCaseReferences(rawCaseRefs);
        return refs.stream()
            .map(ref -> {
                CaseRecord storedRecord = cases.getIfPresent(ref);
                if (storedRecord == null) {
                    throw notFound("No case stored for reference " + ref);
                }
                ObjectNode node = mapper.createObjectNode();
                node.set(CASE_DETAILS_FIELD, storedRecord.caseDetails.deepCopy());
                node.put("revision", storedRecord.revision);
                return node;
            })
            .toList();
    }

    @GetMapping("/cases/{caseRef}/history")
    public List<ObjectNode> getHistory(@PathVariable("caseRef") long caseReference) {
        CaseRecord caseRecord = cases.getIfPresent(caseReference);
        return caseRecord.history.stream()
            .map(ObjectNode::deepCopy)
            .toList();
    }

    @GetMapping("/cases/{caseRef}/history/{eventId}")
    public ResponseEntity<ObjectNode> getHistoryEvent(
        @PathVariable("caseRef") long caseReference,
        @PathVariable("eventId") long eventId
    ) {
        List<ObjectNode> history = getHistory(caseReference);

        return history.stream()
            .filter(event -> event.path("id").asLong() == eventId)
            .findFirst()
            .map(ResponseEntity::ok)
            .orElseThrow(() -> notFound("No event " + eventId + " for case " + caseReference));
    }

    @PostMapping("/cases/{caseRef}/supplementary-data")
    public ObjectNode updateSupplementaryData(
        @PathVariable("caseRef") long caseReference,
        @RequestBody ObjectNode request
    ) {
        if (cases.getIfPresent(caseReference) == null) {
            throw notFound("No case stored for reference " + caseReference);
        }
        LOG.info("ServicePersistenceStub received supplementary-data update for {}: {}", caseReference, request);
        ObjectNode updates = requireObjectNode(request, "supplementary_data_updates");
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

    private ObjectNode buildAuditEvent(long reference, ObjectNode payload, ObjectNode caseDetails) {
        ObjectNode eventDetails = payload.has(EVENT_DETAILS_FIELD) && payload.get(EVENT_DETAILS_FIELD).isObject()
            ? (ObjectNode) payload.get(EVENT_DETAILS_FIELD)
            : mapper.createObjectNode();
        ObjectNode event = mapper.createObjectNode();
        String eventTriggerId = eventDetails.path(EVENT_ID_FIELD).asText("event");

        event.put("id", eventTriggerId);
        event.put("event_name", eventDetails.path("event_name").asText(eventTriggerId));
        event.put("summary", eventDetails.path("summary").asText(null));
        event.put("description", eventDetails.path("description").asText(null));
        String caseType = eventDetails.path("case_type")
            .asText(caseDetails.path(CASE_TYPE_ID_FIELD).asText(null));
        event.put(CASE_TYPE_ID_FIELD, caseType);
        event.put(CREATED_DATE_FIELD, LocalDateTime.now(ZoneOffset.UTC).toString());
        event.put("state_id", caseDetails.path("state").asText(null));
        ObjectNode dataNode = requireObjectNode(caseDetails, CASE_DATA_FIELD);
        event.set("data", dataNode.deepCopy());
        event.put("case_type_version", caseDetails.path("version").asInt(1));
        event.put("security_classification", requireTextValue(caseDetails, "security_classification"));

        ObjectNode wrapper = mapper.createObjectNode();
        long nextId = auditSequence.getAndIncrement();
        wrapper.put("id", nextId);
        wrapper.put("case_reference", reference);
        wrapper.set("event", event);
        return wrapper;
    }

    private void applyTimestampsAndDefaults(ObjectNode caseDetails, CaseRecord existing) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String nowIso = now.toString();

        if (existing == null) {
            caseDetails.put(CREATED_DATE_FIELD, nowIso);
        } else if (!caseDetails.hasNonNull(CREATED_DATE_FIELD)) {
            caseDetails.put(CREATED_DATE_FIELD, existing.caseDetails.path(CREATED_DATE_FIELD).asText(nowIso));
        }

        caseDetails.put("last_modified", nowIso);
        caseDetails.put("last_state_modified_date", nowIso);

    }

    private ObjectNode requireObjectNode(ObjectNode parent, String fieldName) {
        JsonNode existing = parent.get(fieldName);
        if (existing == null || existing.isNull()) {
            throw badRequest("Missing required object field '%s'".formatted(fieldName));
        }
        if (!existing.isObject()) {
            throw badRequest("Field '%s' must be a JSON object".formatted(fieldName));
        }
        return (ObjectNode) existing;
    }

    private void requireText(ObjectNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw badRequest("Field '%s' must be present and non-empty".formatted(fieldName));
        }
    }

    private String requireTextValue(ObjectNode node, String fieldName) {
        requireText(node, fieldName);
        return node.get(fieldName).asText();
    }

    private void requireHeader(String headerValue, String message) {
        if (headerValue == null || headerValue.isBlank()) {
            throw badRequest(message);
        }
    }

    private List<Long> parseCaseReferences(String raw) {
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(token -> !token.isEmpty())
            .map(Long::valueOf)
            .toList();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private record CaseRecord(ObjectNode caseDetails,
                              long revision,
                              List<ObjectNode> history) {}
}
