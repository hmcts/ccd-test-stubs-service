package uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractDataResponseTransformerTest {

    private static final String TESTER_NODE = "tester_node";
    private static final String MY_VALUE = "myValue";

    private class ClassUnderTest extends AbstractDataResponseTransformer {

        private String jsonNodeName;

        ClassUnderTest(String jsonNodeName) {
            this.jsonNodeName = jsonNodeName;
        }

        @Override
        protected String dynamiseResponse(Request request, Response response, Parameters parameters) {
            return super.dynamiseResponse(request, response, parameters, jsonNodeName);
        }

        @Override
        public String getName() {
            return "ClassUnderTest";
        }
    }

    private ClassUnderTest testerNodeResponseTransformer = new ClassUnderTest(TESTER_NODE);
    private ClassUnderTest myValueNodeResponseTransformer = new ClassUnderTest(MY_VALUE);

    @Test
    @DisplayName("Should add request 'tester_node' to response")
    void shouldAddTesterNodeRequestDataToResponse() {
        Request request = mock(Request.class);
        when(request.getBodyAsString()).thenReturn("{\"" + TESTER_NODE + "\":{\"lastName\":\"Gates\"}}");
        Response response = Response.response().body("{\"" + TESTER_NODE + "\":{\"firstName\":\"Bill\"}}").build();

        Response result = testerNodeResponseTransformer.transform(request, response, null, null);
        assertThat(result.getBodyAsString(),
                is("{\"" + TESTER_NODE + "\":{\"firstName\":\"Bill\",\"lastName\":\"Gates\"}}"));
    }

    @Test
    @DisplayName("Should add request 'my_value' to response")
    void shouldAddMyValueRequestDataToResponse() {
        Request request = mock(Request.class);
        when(request.getBodyAsString()).thenReturn("{\"" + MY_VALUE + "\":{\"lastName\":\"Gates\"}}");
        Response response = Response.response().body("{\"" + MY_VALUE + "\":{\"firstName\":\"Bill\"}}").build();

        Response result = myValueNodeResponseTransformer.transform(request, response, null, null);
        assertThat(result.getBodyAsString(),
                is("{\"" + MY_VALUE + "\":{\"firstName\":\"Bill\",\"lastName\":\"Gates\"}}"));
    }

    @Test
    @DisplayName("Should not apply the testerNodeResponseTransformer globally")
    void shouldNotApplytesterNodeResponseTransformerGlobally() {
        assertThat(testerNodeResponseTransformer.applyGlobally(), is(false));
    }

    @Test
    @DisplayName("Should not apply the myValueResponseTransformer globally")
    void shouldNotApplyMyValueResponseTransformerGlobally() {
        assertThat(myValueNodeResponseTransformer.applyGlobally(), is(false));
    }

    @Test
    @DisplayName("Should return 'tester_node' response as is on error")
    void shouldReturnResponseAsIsOnErrorForTesterNodeResponseTransformer() {
        Request request = mock(Request.class);
        when(request.getBodyAsString()).thenReturn("{\"" + testerNodeResponseTransformer + "\":{}");
        Response response = Response.response().body("{\"data\":{\"firstName\":\"Bill\"}}").build();

        Response result = testerNodeResponseTransformer.transform(request, response, null, null);
        assertThat(result.getBodyAsString(), is(response.getBodyAsString()));
    }

    @Test
    @DisplayName("Should return 'tester_node' response as is on error")
    void shouldReturnResponseAsIsOnErrorForMyValueNodeResponseTransformer() {
        Request request = mock(Request.class);
        when(request.getBodyAsString()).thenReturn("{\"" + myValueNodeResponseTransformer + "\":{}");
        Response response = Response.response()
                .body("{\"" + myValueNodeResponseTransformer + "\":{\"firstName\":\"Bill\"}}")
                .build();

        Response result = myValueNodeResponseTransformer.transform(request, response, null, null);
        assertThat(result.getBodyAsString(), is(response.getBodyAsString()));
    }
}