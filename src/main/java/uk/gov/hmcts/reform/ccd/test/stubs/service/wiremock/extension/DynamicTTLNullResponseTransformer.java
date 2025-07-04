package uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;

public class DynamicTTLNullResponseTransformer extends AbstractTTLResponseTransformer {

    @VisibleForTesting
    static final String DYNAMIC_TTL_NULL_RESPONSE_TRANSFORMER = "dynamic-ttl-null-response-transformer";

    @Override
    protected void adjustTTLInCaseData(ObjectNode responseCaseDataNode) {
        responseCaseDataNode.set(TTL, null);
    }

    @Override
    public String getName() {
        return DYNAMIC_TTL_NULL_RESPONSE_TRANSFORMER;
    }

}
