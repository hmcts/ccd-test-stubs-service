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
import static uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension.DynamicTTLUppercaseResponseTransformer.DYNAMIC_TTL_UPPERCASE_RESPONSE_TRANSFORMER;

class DynamicTTLUppercaseResponseTransformerTest {

    private final DynamicTTLUppercaseResponseTransformer transformer = new DynamicTTLUppercaseResponseTransformer();

    @Test
    @DisplayName("Should copy TTL into response with uppercase suspended value")
    void shouldCopyTTLIntoResponseWithUpperSuspended() {

        // GIVEN
        Request request = mock(Request.class);
        String data = "{\"data\":{\"TTL\":{"
            + "\"SystemTTL\":\"2022-07-18\","
            + "\"OverrideTTL\":\"2025-01-02\","
            + "\"Suspended\":\"lowercase_value\""
            + "},\"OtherField\":\"value\"}}";
        when(request.getBodyAsString()).thenReturn(data);
        Response response = Response.response().body("{\"data\":{}}").build();
        String expectedData = data.replace("lowercase_value", "LOWERCASE_VALUE");

        // WHEN
        Response result = transformer.transform(request, response, null, null);

        // THEN
        assertThat(result.getBodyAsString(), is(expectedData));
    }

    @Test
    @DisplayName("Should remove TTL Suspended from response if null")
    void shouldRemoveTTLSuspendedFromResponseIfNull() {

        // GIVEN
        Request request = mock(Request.class);
        String data = "{\"data\":{\"TTL\":{"
            + "\"SystemTTL\":\"2022-07-18\","
            + "\"OverrideTTL\":\"2025-01-02\","
            + "\"Suspended\":null"
            + "},\"OtherField\":\"value\"}}";
        when(request.getBodyAsString()).thenReturn(data);
        Response response = Response.response().body("{\"data\":{}}").build();
        String expectedData = data.replace(",\"Suspended\":null", "");

        // WHEN
        Response result = transformer.transform(request, response, null, null);

        // THEN
        assertThat(result.getBodyAsString(), is(expectedData));
    }

    @Test
    @DisplayName("Should remove SystemTTL from response if null")
    void shouldRemoveSystemTTLFromResponseIfNull() {

        // GIVEN
        Request request = mock(Request.class);
        String data = "{\"data\":{\"TTL\":{"
            + "\"SystemTTL\":null,"
            + "\"OverrideTTL\":\"2025-01-02\","
            + "\"Suspended\":\"\""
            + "},\"OtherField\":\"value\"}}";
        when(request.getBodyAsString()).thenReturn(data);
        Response response = Response.response().body("{\"data\":{}}").build();
        String expectedData = data.replace("\"SystemTTL\":null,", "");

        // WHEN
        Response result = transformer.transform(request, response, null, null);

        // THEN
        assertThat(result.getBodyAsString(), is(expectedData));
    }

    @Test
    @DisplayName("Should remove OverrideTTL from response if null")
    void shouldRemoveOverrideTTLFromResponseIfNull() {

        // GIVEN
        Request request = mock(Request.class);
        String data = "{\"data\":{\"TTL\":{"
            + "\"SystemTTL\":\"2022-07-18\","
            + "\"OverrideTTL\":null,"
            + "\"Suspended\":\"\""
            + "},\"OtherField\":\"value\"}}";
        when(request.getBodyAsString()).thenReturn(data);
        Response response = Response.response().body("{\"data\":{}}").build();
        String expectedData = data.replace("\"OverrideTTL\":null,", "");

        // WHEN
        Response result = transformer.transform(request, response, null, null);

        // THEN
        assertThat(result.getBodyAsString(), is(expectedData));
    }

    @Test
    @DisplayName("Should not fail if no TTL in request")
    void shouldNotFailIfNoTTLInRequest() {

        // GIVEN
        Request request = mock(Request.class);
        when(request.getBodyAsString()).thenReturn("{\"data\":{\"OtherField\":\"value\"}}");
        Response response = Response.response().body("{\"case_data\":{}}").build();

        // WHEN
        Response result = transformer.transform(request, response, null, null);

        // THEN
        assertThat(result.getBodyAsString(), is("{\"case_data\":{\"OtherField\":\"value\"}}"));
    }

    @ParameterizedTest(
        name = "Should not fail if TTL field missing in request"
    )
    @ValueSource(
        strings = {
            // missing Suspended
            "{\"data\":{\"TTL\":{"
                + "\"SystemTTL\":\"2022-07-18\","
                + "\"OverrideTTL\":\"2025-01-02\""
                + "},\"OtherField\":\"value\"}}",

            // missing SystemTTL
            "{\"data\":{\"TTL\":{"
                + "\"OverrideTTL\":\"2025-01-02\","
                + "\"Suspended\":\"\""
                + "},\"OtherField\":\"value\"}}",

            // missing OverrideTTL
            "{\"data\":{\"TTL\":{"
                + "\"SystemTTL\":\"2022-07-18\","
                + "\"Suspended\":\"\""
                + "},\"OtherField\":\"value\"}}"
        }
    )
    void shouldNotFailIfTTLFieldMissingInRequest(String data) {

        // GIVEN
        Request request = mock(Request.class);
        when(request.getBodyAsString()).thenReturn(data);
        Response response = Response.response().body("{\"data\":{}}").build();

        // WHEN
        Response result = transformer.transform(request, response, null, null);

        // THEN
        assertThat(result.getBodyAsString(), is(data)); // i.e. unchanged (NB: Suspended is empty)
    }

    @Test
    @DisplayName("Should not apply the transformer globally")
    void shouldNotApplyTransformerGlobally() {
        assertThat(transformer.applyGlobally(), is(false));
    }

    @Test
    @DisplayName("Should return name of the transformer")
    void shouldReturnName() {
        assertThat(transformer.getName(), is(DYNAMIC_TTL_UPPERCASE_RESPONSE_TRANSFORMER));
    }

    @Test
    @DisplayName("Should not throw exception for malformed request json")
    void shouldNotThrowExceptionForMalformedRequestJson() {

        // GIVEN
        Request request = mock(Request.class);
        when(request.getBodyAsString()).thenReturn("{\"bad-json\"}");
        Response response = Response.response().body("{\"data\":{}}").build();

        // WHEN
        Response result = transformer.transform(request, response, null, null);

        // THEN
        assertThat(result.getBodyAsString(), is("{\"data\":{}}"));
    }

}
