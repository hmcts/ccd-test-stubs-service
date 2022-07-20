package uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension.DynamicTTLRemoveResponseTransformer.DYNAMIC_TTL_REMOVE_RESPONSE_TRANSFORMER;

class DynamicTTLRemoveResponseTransformerTest {

    private final DynamicTTLRemoveResponseTransformer transformer = new DynamicTTLRemoveResponseTransformer();

    @ParameterizedTest(name = "Should remove TTL from response")
    @ValueSource(
        strings = {
            // with TTL
            "{\"case_details\":{\"case_data\":{\"TTL\":{},\"OtherField\":\"value\"}}}",

            // without TTL
            "{\"case_details\":{\"case_data\":{\"OtherField\":\"value\"}}}"
        }
    )
    void shouldRemoveTTLFromResponse(String requestJson) {

        // GIVEN
        Request request = mock(Request.class);
        when(request.getBodyAsString()).thenReturn(requestJson);
        Response response = Response.response().body("{\"data\":{}}").build();

        // WHEN
        Response result = transformer.transform(request, response, null, null);

        // THEN
        assertThat(result.getBodyAsString(), is("{\"data\":{\"OtherField\":\"value\"}}"));
    }

    @Test
    @DisplayName("Should not apply the transformer globally")
    void shouldNotApplyTransformerGlobally() {
        assertThat(transformer.applyGlobally(), is(false));
    }

    @Test
    @DisplayName("Should return name of the transformer")
    void shouldReturnName() {
        assertThat(transformer.getName(), is(DYNAMIC_TTL_REMOVE_RESPONSE_TRANSFORMER));
    }

    @ParameterizedTest(name = "Should not throw exception for malformed or missing case_data or json")
    @ValueSource(
        strings = {
            // with case_data null
            "{\"case_details\": null}",

            // with null case_data
            "{\"case_details\":{\"case_data\": null}}",

            // with missing case_data
            "{\"case_details\": {}}",

            // with missing case_details
            "{\"other_element\": {}}",

            // bad json
            "{\"bad-json\"}"
        }
    )
    void shouldNotThrowExceptionForMalformedOrMissingCaseData(String requestJson) {

        // GIVEN
        Request request = mock(Request.class);
        when(request.getBodyAsString()).thenReturn(requestJson);
        String responseTemplate = "{\"data\":{}}";
        Response response = Response.response().body(responseTemplate).build();

        // WHEN
        Response result = transformer.transform(request, response, null, null);

        // THEN
        assertThat(result.getBodyAsString(), is(responseTemplate));
    }

}
