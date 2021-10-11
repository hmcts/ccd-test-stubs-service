package uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/*
 * Merges request case data with response case data before returning the response
 */
public class DynamicCaseCaseDataResponseTransformer extends AbstractDynamicResponseTransformer {

    @VisibleForTesting
    static final String DYNAMIC_CASE_CASE_DATA_RESPONSE_TRANSFORMER = "dynamic-case-case-data-response-transformer";

    private static final Logger LOG = LoggerFactory.getLogger(DynamicCaseCaseDataResponseTransformer.class);
    private static final String CASE_DATA_JSON_NODE = "case_data";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected String dynamiseResponse(Request request, Response response, Parameters parameters) {
        try {
            LOG.info("Adding dynamic case data to response for request: {}", request.getUrl());
            // Adds case data from request to outgoing response
            ObjectNode requestNode = OBJECT_MAPPER.readValue(request.getBodyAsString(), ObjectNode.class);
            ObjectNode responseNode = OBJECT_MAPPER.readValue(response.getBodyAsString(), ObjectNode.class);
            if (responseNode.has(CASE_DATA_JSON_NODE) && requestNode.has(CASE_DATA_JSON_NODE)) {
                ObjectNode requestCaseDataNode = (ObjectNode) requestNode.get(CASE_DATA_JSON_NODE);
                ObjectNode responseCaseDataNode = (ObjectNode) responseNode.get(CASE_DATA_JSON_NODE);
                requestCaseDataNode.fields().forEachRemaining(entry -> {
                    if (!responseCaseDataNode.has(entry.getKey())) {
                        responseCaseDataNode.set(entry.getKey(), entry.getValue());
                    }
                });
                return responseNode.toString();
            }
        } catch (IOException e) {
            LOG.error("Exception deserialising response body: {}", e.getMessage());
        }

        return response.getBodyAsString();
    }

    @Override
    public String getName() {
        return DYNAMIC_CASE_CASE_DATA_RESPONSE_TRANSFORMER;
    }

}
