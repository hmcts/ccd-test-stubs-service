package uk.gov.hmcts.reform.ccd.test.stubs.service.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.test.stubs.service.mock.server.MockHttpServer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StubResponseControllerTest {

    private StubResponseController stubResponseController;

    @Autowired
    ObjectMapper mapper;

    @Mock
    RestTemplate restTemplate;

    @Mock
    MockHttpServer mockHttpServer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        stubResponseController = new StubResponseController(restTemplate, mockHttpServer, mapper);
    }

    @Test
    @DisplayName("Should return internal server error on invalid request input stream")
    void shouldReturnInternalServerError() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        doThrow(new IOException("")).when(request).getInputStream();

        ResponseEntity<Object> responseEntity = stubResponseController.forwardGetRequests(request);
        assertNotNull(responseEntity);
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    @DisplayName("Should return internal server error on invalid request input stream")
    void shouldReturnInternalServerErrorForJcTest2() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        doThrow(new IOException("")).when(request).getInputStream();

        try {
            ResponseEntity<Object> responseEntity = stubResponseController.jctest2(request);
            fail();
        } catch (IOException e) {
            assertThat(e.getMessage(), is(""));
        }
    }

    @Test
    @DisplayName("Should return response")
    void shouldReturnResponse() throws IOException {
        MockHttpServletRequest request = mock(MockHttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getCharacterEncoding()).thenReturn("ISO-8859-1");
        ResponseEntity<Object> responseEntity = new ResponseEntity<>(HttpStatus.OK);

        when(restTemplate.exchange(
            Matchers.anyString(),
            Matchers.any(HttpMethod.class),
            Matchers.any(),
            Matchers.<Class<Object>>any(),
            Matchers.<Map<String, String[]>>any()
            )
        ).thenReturn(responseEntity);

        ResponseEntity<Object> responseEntityReturned = stubResponseController.forwardPostRequests(request);
        assertNotNull(responseEntityReturned);
        assertThat(responseEntityReturned.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    @DisplayName("Should return jwkeys")
    void shouldReturnJwkeys() throws JOSEException {
        HttpServletRequest request = mock(HttpServletRequest.class);

        ResponseEntity<Object> responseEntity = stubResponseController.jwkeys(request);
        assertNotNull(responseEntity);
        try {
            JSONObject keys = new JSONObject(responseEntity.getBody().toString());
            assertNull(keys.get("p"));
            assertNull(keys.get("d"));
            assertNull(keys.get("privateOnly"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    @DisplayName("Should return openid token")
    void shouldReturnOpenIdToken() throws JOSEException {
        HttpServletRequest request = mock(HttpServletRequest.class);

        ResponseEntity<Object> responseEntity = stubResponseController.openIdToken(request);
        assertNotNull(responseEntity);
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    @DisplayName("Should return oauth2 token")
    void shouldReturnOauth2Token() throws JOSEException {
        HttpServletRequest request = mock(HttpServletRequest.class);

        ResponseEntity<Object> responseEntity = stubResponseController.oauth2Token(request);
        assertNotNull(responseEntity);
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    }
}
