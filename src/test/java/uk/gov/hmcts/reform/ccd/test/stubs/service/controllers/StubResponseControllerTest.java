package uk.gov.hmcts.reform.ccd.test.stubs.service.controllers;

import com.nimbusds.jose.JOSEException;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.test.stubs.service.mock.server.MockHttpServer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class StubResponseControllerTest {

    private StubResponseController stubResponseController;

    @Mock
    RestTemplate restTemplate;

    @Mock
    MockHttpServer mockHttpServer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        stubResponseController = new StubResponseController(restTemplate, mockHttpServer);
    }

    @Test
    @DisplayName("Should return internal server error on invalid request input stream")
    void shouldReturnInternalServerError() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        doThrow(new IOException("")).when(request).getInputStream();

        ResponseEntity<Object> responseEntity = stubResponseController.forwardGetRequests(request);
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    @DisplayName("Should return jwkeys")
    void shouldReturnJwkeys() throws JOSEException {
        HttpServletRequest request = mock(HttpServletRequest.class);

        ResponseEntity<Object> responseEntity = stubResponseController.jwkeys(request);
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    @DisplayName("Should return openid token")
    void shouldReturnOpenIdToken() throws JOSEException {
        HttpServletRequest request = mock(HttpServletRequest.class);

        ResponseEntity<Object> responseEntity = stubResponseController.openIdToken(request);
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    @DisplayName("Should return openid token")
    void shouldReturnOauth2Token() throws JOSEException {
        HttpServletRequest request = mock(HttpServletRequest.class);

        ResponseEntity<Object> responseEntity = stubResponseController.oauth2Token(request);
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    }
}
