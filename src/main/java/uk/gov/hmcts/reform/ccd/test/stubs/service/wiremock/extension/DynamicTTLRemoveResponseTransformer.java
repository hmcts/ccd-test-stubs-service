package uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;

/*
 * Removes the TTL field values.
 */
public class DynamicTTLRemoveResponseTransformer extends AbstractTTLResponseTransformer {

    @VisibleForTesting
    static final String DYNAMIC_TTL_REMOVE_RESPONSE_TRANSFORMER = "dynamic-ttl-remove-response-transformer";

    @Override
    protected void adjustTTLInCaseData(ObjectNode responseCaseDataNode) {

        if (responseCaseDataNode.has(TTL)) {
            responseCaseDataNode.remove(TTL);
        }
    }

    @Override
    public String getName() {
        return DYNAMIC_TTL_REMOVE_RESPONSE_TRANSFORMER;
    }

}
