package uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.annotations.VisibleForTesting;

/*
 * Manipulates the TTL field values.
 */
public class DynamicTTLUppercaseResponseTransformer extends AbstractTTLResponseTransformer {

    @VisibleForTesting
    static final String DYNAMIC_TTL_UPPERCASE_RESPONSE_TRANSFORMER = "dynamic-ttl-uppercase-response-transformer";

    @Override
    protected void adjustTTLInCaseData(ObjectNode responseCaseDataNode) {

        if (responseCaseDataNode.has(TTL)) {
            ObjectNode responseTTLNode = (ObjectNode) responseCaseDataNode.get(TTL);

            // copy strange behaviour of some service callbacks:
            //  * remove SystemTTL if null value
            if (responseTTLNode.has(TTL_SYSTEM) && responseTTLNode.get(TTL_SYSTEM).isNull()) {
                responseTTLNode.remove(TTL_SYSTEM);
            }

            // copy strange behaviour of some service callbacks:
            //  * remove OverrideTTL if null value
            if (responseTTLNode.has(TTL_OVERRIDE) && responseTTLNode.get(TTL_OVERRIDE).isNull()) {
                responseTTLNode.remove(TTL_OVERRIDE);
            }

            // copy strange behaviour of some service callbacks:
            //  * remove Suspended if null value
            if (responseTTLNode.has(TTL_SUSPENDED)) {
                if (responseTTLNode.get(TTL_SUSPENDED).isNull()) {
                    responseTTLNode.remove(TTL_SUSPENDED);
                } else {
                    responseTTLNode.set(
                            TTL_SUSPENDED,
                            new TextNode(responseTTLNode.get(TTL_SUSPENDED).asText().toUpperCase())
                    );
                }
            }
        }
    }

    @Override
    public String getName() {
        return DYNAMIC_TTL_UPPERCASE_RESPONSE_TRANSFORMER;
    }

}
