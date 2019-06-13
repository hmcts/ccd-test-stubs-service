package uk.gov.hmcts.reform.ccd.stub.callback.controllers;

import java.util.Optional;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class StubResponseBuilder {

    private static final String CASE_DATA = "case_data";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonNode response = OBJECT_MAPPER.createObjectNode();

    private StubResponseBuilder() {
    }

    private StubResponseBuilder(JsonNode request) {
        Optional<JsonNode> caseData = ofNullable(request.get(CASE_DATA));
        caseData.ifPresent(data -> setCaseData((ObjectNode) data));
    }

    public static StubResponseBuilder create(JsonNode request) {
        return new StubResponseBuilder(request);
    }

    public static StubResponseBuilder create() {
        return new StubResponseBuilder();
    }

    public StubResponseBuilder withDataFields(JsonNode data) {
        setCaseData((ObjectNode) data);
        return this;
    }

    public JsonNode build() {
        return response;
    }

    private void setCaseData(ObjectNode caseData) {
        ((ObjectNode) this.response).setAll(caseData);
    }
}
