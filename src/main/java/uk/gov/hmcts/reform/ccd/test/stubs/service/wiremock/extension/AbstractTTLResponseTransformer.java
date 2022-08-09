package uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class AbstractTTLResponseTransformer extends AbstractDynamicResponseTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTTLResponseTransformer.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String CASE_DETAILS = "case_details";
    private static final String CASE_DATA = "case_data";
    private static final String DATA = "data";

    protected static final String TTL = "TTL";
    protected static final String TTL_SYSTEM = "SystemTTL";
    protected static final String TTL_OVERRIDE = "OverrideTTL";
    protected static final String TTL_SUSPENDED = "Suspended";

    @Override
    protected String dynamiseResponse(Request request, Response response, Parameters parameters) {

        try {
            ObjectNode requestNode = OBJECT_MAPPER.readValue(request.getBodyAsString(), ObjectNode.class);
            ObjectNode responseNode = OBJECT_MAPPER.readValue(response.getBodyAsString(), ObjectNode.class);

            ObjectNode requestDataNode = null;

            if (requestNode.hasNonNull(CASE_DETAILS)) {
                var requestCaseDetailsNode = requestNode.get(CASE_DETAILS);
                if (requestCaseDetailsNode.hasNonNull(CASE_DATA)) {
                    requestDataNode = (ObjectNode) requestCaseDetailsNode.get(CASE_DATA);

                    adjustTTLInCaseData(requestDataNode);

                    responseNode.set(DATA, requestDataNode);
                    return responseNode.toString();
                }
            }

        } catch (IOException e) {
            LOG.error("Exception deserialising response body: {}", e.getMessage());
        }

        return response.getBodyAsString();
    }

    protected abstract void adjustTTLInCaseData(ObjectNode responseCaseDataNode);

}
