package uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension.DynamicRoleAssignmentsResponseTransformer.GET_ROLE_ASSIGNMENTS_URL;

class DynamicRoleAssignmentsResponseTransformerTest {

    public static final String USER_ID = "cc828f53-05cb-4534-b64f-1dfe30748b6c";
    public static final String USER_ID_LABEL = "{{userId}}";

    private final DynamicRoleAssignmentsResponseTransformer transformer =
        new DynamicRoleAssignmentsResponseTransformer();

    @Test
    @DisplayName("Should add url userId to response")
    void shouldAddUrlUserIdToResponse() {
        Request request = mock(Request.class);
        when(request.getMethod()).thenReturn(RequestMethod.GET);
        when(request.getUrl()).thenReturn(GET_ROLE_ASSIGNMENTS_URL + USER_ID);
        Response response = Response.response().body("{\"actorId\" : \"{{userId}}\"}").build();

        Response result = transformer.transform(request, response, null, null);
        assertThat(result.getBodyAsString(), is("{\"actorId\" : \"" + USER_ID + "\"}"));
    }

    @Test
    @DisplayName("Should NOT add url userId to response")
    void shouldNotAddUrlUserIdToResponse() {
        Request request = mock(Request.class);
        when(request.getMethod()).thenReturn(RequestMethod.POST);
        when(request.getUrl()).thenReturn(GET_ROLE_ASSIGNMENTS_URL + USER_ID);
        Response response = Response.response().body("{\"actorId\" : \"{{userId}}\"}").build();

        Response result = transformer.transform(request, response, null, null);
        assertThat(result.getBodyAsString(), is("{\"actorId\" : \"" + USER_ID_LABEL + "\"}"));
    }

}
