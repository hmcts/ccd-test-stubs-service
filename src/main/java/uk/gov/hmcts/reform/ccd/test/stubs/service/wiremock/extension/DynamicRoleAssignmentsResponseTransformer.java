package uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.Response;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Retrieves userId from the url and replaces {{userId}} placeholder with its value
 */
public class DynamicRoleAssignmentsResponseTransformer extends AbstractDynamicResponseTransformer {

    @VisibleForTesting
    static final String DYNAMIC_ROLE_ASSIGNMENTS_RESPONSE_TRANSFORMER = "dynamic-role-assignments-response-transformer";

    private static final Logger LOG = LoggerFactory.getLogger(DynamicRoleAssignmentsResponseTransformer.class);
    private static final String USER_ID_PLACEHOLDER = "{{userId}}";
    public static final String GET_ROLE_ASSIGNMENTS_URL = "/am/role-assignments/actors/";

    @Override
    protected String dynamiseResponse(Request request, Response response, Parameters parameters) {
        if (request.getMethod().equals(RequestMethod.GET) && request.getUrl().contains(GET_ROLE_ASSIGNMENTS_URL)) {
            LOG.info("Adding dynamic userId to response for request: {}", request.getUrl());
            var userId = request.getUrl().substring(GET_ROLE_ASSIGNMENTS_URL.length());
            LOG.info("Extracted userId path parameter: {}", userId);
            return response.getBodyAsString().replace(USER_ID_PLACEHOLDER, userId);
        } else {
            return response.getBodyAsString();
        }
    }

    @Override
    public String getName() {
        return DYNAMIC_ROLE_ASSIGNMENTS_RESPONSE_TRANSFORMER;
    }

}
