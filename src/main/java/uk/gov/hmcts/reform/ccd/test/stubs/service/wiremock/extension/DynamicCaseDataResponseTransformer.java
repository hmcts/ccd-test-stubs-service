package uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Merges request case data with response case data before returning the response
 */
public class DynamicCaseDataResponseTransformer extends AbstractDynamicResponseTransformer {

    @VisibleForTesting
    static final String DYNAMIC_CASE_DATA_RESPONSE_TRANSFORMER = "dynamic-case-data-response-transformer";

    private static final Logger LOG = LoggerFactory.getLogger(DynamicCaseDataResponseTransformer.class);
    private static final String CASE_DATA = "case_data";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected String dynamiseResponse(Request request, Response response, Parameters parameters) {
        try {
            LOG.info("Adding dynamic case data to response for request: {}", request.getUrl());
            // Adds case data from request to outgoing response
            ObjectNode requestNode = OBJECT_MAPPER.readValue(request.getBodyAsString(), ObjectNode.class);
            ObjectNode responseNode = OBJECT_MAPPER.readValue(response.getBodyAsString(), ObjectNode.class);
            if (responseNode.has(CASE_DATA) && requestNode.has(CASE_DATA)) {
                ObjectNode requestCaseDataNode = (ObjectNode) requestNode.get(CASE_DATA);
                ObjectNode responseCaseDataNode = (ObjectNode) responseNode.get(CASE_DATA);
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
        return DYNAMIC_CASE_DATA_RESPONSE_TRANSFORMER;
    }

}
