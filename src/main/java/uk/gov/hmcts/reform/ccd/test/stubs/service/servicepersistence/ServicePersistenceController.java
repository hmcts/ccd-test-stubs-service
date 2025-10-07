package uk.gov.hmcts.reform.ccd.test.stubs.service.servicepersistence;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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

@RestController
@RequestMapping("/ccd-persistence")
public class ServicePersistenceController {

    private static final Logger LOG = LoggerFactory.getLogger(ServicePersistenceController.class);
    private static final String STUB_PROCESSOR_FIELD = "_stubProcessedBy";
    private static final String STUB_PROCESSOR_VALUE = "ccd-test-stubs-service";
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
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        ObjectNode caseDetails = (ObjectNode) Objects.requireNonNull(
            payload.get("case_details"),
            "case_details is required"
        );
        long reference = caseDetails.path("reference").asLong();
        CaseRecord existing = cases.getIfPresent(reference);

        long revision = existing == null ? 1 : existing.revision + 1;
        ObjectNode caseDataNode = caseDetails.has("data") && caseDetails.get("data").isObject()
            ? (ObjectNode) caseDetails.get("data")
            : caseDetails.putObject("data");
        caseDataNode.put(STUB_PROCESSOR_FIELD, STUB_PROCESSOR_VALUE);
        caseDetails.put("revision", revision);
        JsonNode resolvedTtl = payload.path("resolved_ttl");
        if (!resolvedTtl.isMissingNode() && !resolvedTtl.isNull()) {
            caseDetails.put("resolved_ttl", resolvedTtl.asText());
        }

        List<ObjectNode> history = existing == null ? new ArrayList<>() : new ArrayList<>(existing.history);
        history.add(buildAuditEvent(reference, payload, caseDetails));

        cases.put(reference, new CaseRecord(caseDetails, revision, history));

        ObjectNode response = mapper.createObjectNode();
        response.set("case_details", caseDetails);
        response.put("revision", revision);
        response.put("ignore_warning", true);

        boolean created = existing == null;
        LOG.info("ServicePersistenceStub processed event {} for case ref {}{}",
            payload.path("event_details").path("event_id").asText("unknown"),
            payload.path("case_details").path("reference").asText("?"),
            idempotencyKey != null ? " (idempotency=" + idempotencyKey + ")" : "");
        HttpStatus status = created ? HttpStatus.CREATED : HttpStatus.OK;
        return new ResponseEntity<>(response, status);
    }

    @GetMapping("/cases")
    public List<ObjectNode> getCases(
        @RequestParam("case-refs") String caseRefs
    ) {
        List<Long> refs = Arrays.stream(caseRefs.split(","))
            .map(String::trim)
            .filter(ref -> !ref.isEmpty())
            .map(Long::valueOf)
            .collect(Collectors.toList());
        return fetchCases(refs);
    }

    @GetMapping("/cases/{caseRef}/history")
    public List<ObjectNode> getHistory(@PathVariable("caseRef") long caseReference) {
        return fetchHistory(caseReference);
    }

    @GetMapping("/cases/{caseRef}/history/{eventId}")
    public ResponseEntity<ObjectNode> getHistoryEvent(
        @PathVariable("caseRef") long caseReference,
        @PathVariable("eventId") long eventId
    ) {
        Optional<ObjectNode> event = fetchHistoryEvent(caseReference, eventId);
        return event.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/cases/{caseRef}/supplementary-data")
    public ObjectNode updateSupplementaryData(
        @PathVariable("caseRef") long caseReference,
        @RequestBody ObjectNode request
    ) {
        ObjectNode response = mapper.createObjectNode();
        ObjectNode supplementary = (ObjectNode) Objects.requireNonNull(
            request.get("supplementary_data"),
            "supplementary_data is required"
        );
        response.set("supplementary_data", supplementary);
        return response;
    }

    private List<ObjectNode> fetchCases(List<Long> refs) {
        return refs.stream()
            .map(cases::getIfPresent)
            .filter(record -> record != null)
            .map(record -> {
                ObjectNode node = mapper.createObjectNode();
                node.set("case_details", record.caseDetails);
                node.put("revision", record.revision);
                return node;
            })
            .toList();
    }

    private List<ObjectNode> fetchHistory(long reference) {
        CaseRecord record = cases.getIfPresent(reference);
        return record == null ? List.of() : List.copyOf(record.history);
    }

    private Optional<ObjectNode> fetchHistoryEvent(long reference, long eventId) {
        return fetchHistory(reference).stream()
            .filter(event -> event.path("id").asLong() == eventId)
            .findFirst();
    }

    private ObjectNode buildAuditEvent(long reference, ObjectNode payload, ObjectNode caseDetails) {
        ObjectNode eventDetails = payload.has("event_details") && payload.get("event_details").isObject()
            ? (ObjectNode) payload.get("event_details")
            : mapper.createObjectNode();
        ObjectNode event = mapper.createObjectNode();
        long nextId = auditSequence.getAndIncrement();

        event.put("id", nextId);
        event.put("event_id", eventDetails.path("event_id").asText("event"));
        event.put("event_name", eventDetails.path("event_name").asText(null));
        event.put("summary", eventDetails.path("summary").asText(null));
        event.put("description", eventDetails.path("description").asText(null));
        event.put("case_type_id", eventDetails.path("case_type").asText(caseDetails.path("case_type_id").asText(null)));
        event.put("created_date", OffsetDateTime.now().toString());
        event.put("state_id", caseDetails.path("state").asText(null));
        ObjectNode dataNode = caseDetails.has("data") && caseDetails.get("data").isObject()
            ? (ObjectNode) caseDetails.get("data")
            : mapper.createObjectNode();
        event.set("data", dataNode);

        ObjectNode wrapper = mapper.createObjectNode();
        wrapper.put("id", nextId);
        wrapper.put("case_reference", reference);
        wrapper.set("event", event);
        return wrapper;
    }

    private record CaseRecord(ObjectNode caseDetails,
                              long revision,
                              List<ObjectNode> history) {}
}
