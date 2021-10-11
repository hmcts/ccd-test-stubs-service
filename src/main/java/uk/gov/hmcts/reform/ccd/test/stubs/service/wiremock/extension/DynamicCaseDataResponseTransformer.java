package uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.google.common.annotations.VisibleForTesting;

/*
 * Merges request case data with response case data before returning the response
 */
public class DynamicCaseDataResponseTransformer extends AbstractDataResponseTransformer {

    @VisibleForTesting
    static final String DYNAMIC_CASE_DATA_RESPONSE_TRANSFORMER = "dynamic-case-data-response-transformer";

    private static final String CASE_DATA = "data";

    @Override
    protected String dynamiseResponse(Request request, Response response, Parameters parameters) {
        return dynamiseResponse(request, response, parameters, CASE_DATA);
    }

    @Override
    public String getName() {
        return DYNAMIC_CASE_DATA_RESPONSE_TRANSFORMER;
    }

}
