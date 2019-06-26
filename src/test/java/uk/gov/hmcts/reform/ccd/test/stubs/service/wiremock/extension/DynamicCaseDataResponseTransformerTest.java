package uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension.DynamicCaseDataResponseTransformer.DYNAMIC_CASE_DATA_RESPONSE_TRANSFORMER;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DynamicCaseDataResponseTransformerTest {

    private final DynamicCaseDataResponseTransformer transformer = new DynamicCaseDataResponseTransformer();

    @Test
    @DisplayName("Should add request case data to response")
    void shouldAddRequestDataToResponse() {
        Request request = mock(Request.class);
        when(request.getBodyAsString()).thenReturn("{\"case_data\":{\"lastName\":\"Gates\"}}");
        Response response = Response.response().body("{\"case_data\":{\"firstName\":\"Bill\"}}").build();

        Response result = transformer.transform(request, response, null, null);
        assertThat(result.getBodyAsString(), is("{\"case_data\":{\"firstName\":\"Bill\",\"lastName\":\"Gates\"}}"));
    }

    @Test
    @DisplayName("Should not apply the transformer globally")
    void shouldNotApplyTransformerGlobally() {
        assertThat(transformer.applyGlobally(), is(false));
    }

    @Test
    @DisplayName("Should return response as is on error")
    void shouldReturnResponseAsIsOnError() {
        Request request = mock(Request.class);
        when(request.getBodyAsString()).thenReturn("{\"case_data\":{}");
        Response response = Response.response().body("{\"case_data\":{\"firstName\":\"Bill\"}}").build();

        Response result = transformer.transform(request, response, null, null);
        assertThat(result.getBodyAsString(), is(response.getBodyAsString()));
    }

    @Test
    @DisplayName("Should return name of the transformer")
    void shouldReturnName() {
        assertThat(transformer.getName(), is(DYNAMIC_CASE_DATA_RESPONSE_TRANSFORMER));
    }

}
