package uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class AbstractDataResponseTransformer extends AbstractDynamicResponseTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataResponseTransformer.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected String dynamiseResponse(Request request, Response response, Parameters parameters, String jsonNodeName) {
        try {
            LOG.info("Adding dynamic case data to response for request: {}", request.getUrl());
            // Adds case data from request to outgoing response
            ObjectNode requestNode = OBJECT_MAPPER.readValue(request.getBodyAsString(), ObjectNode.class);
            ObjectNode responseNode = OBJECT_MAPPER.readValue(response.getBodyAsString(), ObjectNode.class);
            if (responseNode.has(jsonNodeName) && requestNode.has(jsonNodeName)) {
                ObjectNode requestCaseDataNode = (ObjectNode) requestNode.get(jsonNodeName);
                ObjectNode responseCaseDataNode = (ObjectNode) responseNode.get(jsonNodeName);
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
}
