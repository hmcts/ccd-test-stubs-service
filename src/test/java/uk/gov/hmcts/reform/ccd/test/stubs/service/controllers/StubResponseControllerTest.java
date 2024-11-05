package uk.gov.hmcts.reform.ccd.test.stubs.service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;

import org.apache.hc.core5.net.URIBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.test.stubs.service.mock.server.MockHttpServer;
import uk.gov.hmcts.reform.ccd.test.stubs.service.util.HeadersProvider;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StubResponseControllerTest {

    private StubResponseController stubResponseController;

    @Autowired
    ObjectMapper mapper;

    @Mock
    MockHttpServer mockHttpServer;

    @Mock
    HttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        stubResponseController = new StubResponseController(mockHttpClient, mockHttpServer, mapper);
    }

    /**
     * Forward GET requests unit test with status OK.
     */
    @Test
    @DisplayName("Test for forwardGetRequests() status OK")
    void shouldReturnStatusOK_ForwardGetRequests() throws IOException, InterruptedException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpResponse mockResponse = mock(HttpResponse.class);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());
        when(mockResponse.body()).thenReturn("MOCK BODY");
        when(mockResponse.statusCode()).thenReturn(200);

        ResponseEntity<Object> responseEntityReturned = stubResponseController.forwardGetRequests(mockRequest);
        assertNotNull(responseEntityReturned);
        assertThat(responseEntityReturned.getStatusCode(), is(HttpStatus.OK));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Test for forwardGetRequests() with query parameter status OK")
    void shouldReturnStatusOK_ForwardGetRequestsWhenQueryParametersPresent()
        throws IOException, InterruptedException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        String[] value = {"1"};
        when(mockRequest.getParameterMap()).thenReturn(Map.of("id", value));

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn("MOCK BODY");
        when(mockResponse.statusCode()).thenReturn(200);

        ResponseEntity<Object> responseEntityReturned = stubResponseController.forwardGetRequests(mockRequest);
        assertNotNull(responseEntityReturned);
        assertThat(responseEntityReturned.getStatusCode(), is(HttpStatus.OK));
    }

    /**
     * Forward GET requests unit test with exception thrown.
     */
    @Test
    @DisplayName("Test for forwardGetRequests() exception thrown")
    void shouldThrowException_ForwardGetRequests() throws IOException, InterruptedException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        Mockito.doThrow(new IOException("")).when(mockHttpClient).send(any(), any());

        ResponseEntity<Object> responseEntityReturned = stubResponseController.forwardGetRequests(mockRequest);
        assertNotNull(responseEntityReturned);
        assertThat(responseEntityReturned.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * Forward POST requests unit test with status OK.
     */
    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Test for forwardPostRequests() status OK")
    void shouldReturnStatusOK_ForwardPostRequests() throws IOException, InterruptedException {
        stubResponseController = new StubResponseController(mockHttpClient, mockHttpServer, mapper);
        HttpHeaders headers = HttpHeaders.of(
            Map.of("Header-Name", List.of("Header-Value")), (s1, s2) -> true);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockHttpClient.<String>send(any(), any())).thenReturn(mockResponse);
        when(mockResponse.body()).thenReturn("MOCK BODY");
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.headers()).thenReturn(headers);

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        ResponseEntity<Object> responseEntityReturned = stubResponseController.forwardPostRequests(mockRequest);
        assertNotNull(responseEntityReturned);
        assertThat(responseEntityReturned.getStatusCode(), is(HttpStatus.OK));
    }


    static Stream<Map<String, List<String>>> headersProvider() {
        return Stream.of(
            Collections.emptyMap(),
            Map.of("Header-Name-1", List.of("Header-Value1"), "Client-Context", List.of("Header-Value2"))
        );
    }

    /**
     * Forward POST requests unit test with status OK.
     */
    @Test
    @DisplayName("Test for forwardPostRequests() status OK - empty custom headers")
    void shouldReturnStatusOK_ForwardPostRequestsForEmptyCustomHeaders() throws IOException, InterruptedException {
        HttpResponse mockResponse = mock(HttpResponse.class);
        stubResponseController = new StubResponseController(mockHttpClient, mockHttpServer, mapper);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());
        when(mockResponse.body()).thenReturn("MOCK BODY");

        HeadersProvider mockHeadersProvider = mock(HeadersProvider.class);
        when(mockHeadersProvider.getHeaders()).thenReturn(Collections.emptyMap());

        when(mockResponse.statusCode()).thenReturn(200);

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        ResponseEntity<Object> responseEntityReturned = stubResponseController.forwardPostRequests(mockRequest);
        assertNotNull(responseEntityReturned);
        assertThat(responseEntityReturned.getStatusCode(), is(HttpStatus.OK));
    }

    @ParameterizedTest
    @MethodSource("headersProvider")
    @DisplayName("Test for forwardPostRequests() status OK - with custom header")
    void shouldReturnStatusOK_ForwardPostRequestsForCustomHeaders(Map<String, List<String>> headersMap)
        throws IOException, InterruptedException {
        HttpResponse mockResponse = mock(HttpResponse.class);
        stubResponseController = new StubResponseController(mockHttpClient, mockHttpServer, mapper);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());
        when(mockResponse.body()).thenReturn("MOCK BODY");

        HeadersProvider mockHeadersProvider = mock(HeadersProvider.class);
        when(mockHeadersProvider.getHeaders()).thenReturn(headersMap);

        when(mockResponse.statusCode()).thenReturn(200);

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        ResponseEntity<Object> responseEntityReturned = stubResponseController.forwardPostRequests(mockRequest);
        assertNotNull(responseEntityReturned);
        assertThat(responseEntityReturned.getStatusCode(), is(HttpStatus.OK));
    }

    /**
     * Forward POST requests unit test with status OK.
     */
    @Test
    @DisplayName("Test for forwardPostRequests() status OK - with custom header")
    void shouldReturnStatusOK_ForwardPostRequestsForCustomHeaders() throws IOException, InterruptedException {
        HttpResponse mockResponse = mock(HttpResponse.class);
        stubResponseController = new StubResponseController(mockHttpClient, mockHttpServer, mapper);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());
        when(mockResponse.body()).thenReturn("MOCK BODY");

        HttpHeaders headers = HttpHeaders.of(
            Map.of("Header-Name-1", List.of("Header-Value1"),
                "Client-Context", List.of("Header-Value2")), (s1, s2) -> true);
        when(mockResponse.headers()).thenReturn(headers);
        when(mockResponse.statusCode()).thenReturn(200);

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        ResponseEntity<Object> responseEntityReturned = stubResponseController.forwardPostRequests(mockRequest);
        assertNotNull(responseEntityReturned);
        assertThat(responseEntityReturned.getStatusCode(), is(HttpStatus.OK));
    }

    /**
     * Forward POST requests unit test with exception thrown.
     */
    @Test
    @DisplayName("Test for forwardPostRequests() exception thrown")
    void shouldThrowException_ForwardPostRequests() throws IOException, InterruptedException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        Mockito.doThrow(new IOException("")).when(mockHttpClient).send(any(), any());

        ResponseEntity<Object> responseEntityReturned = stubResponseController.forwardPostRequests(mockRequest);
        assertNotNull(responseEntityReturned);
        assertThat(responseEntityReturned.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * Forward PUT requests unit test with status OK.
     */
    @Test
    @DisplayName("Test for forwardPutRequests() status OK")
    void shouldReturnStatusOK_ForwardPutRequests() throws IOException, InterruptedException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpResponse mockResponse = mock(HttpResponse.class);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());
        when(mockResponse.body()).thenReturn("MOCK BODY");
        when(mockResponse.statusCode()).thenReturn(200);

        ResponseEntity<Object> responseEntityReturned = stubResponseController.forwardPutRequests(mockRequest);
        assertNotNull(responseEntityReturned);
        assertThat(responseEntityReturned.getStatusCode(), is(HttpStatus.OK));
    }

    /**
     * Forward PUT requests unit test with exception thrown.
     */
    @Test
    @DisplayName("Test for forwardPutRequests() exception thrown")
    void shouldThrowException_ForwardPutRequests() throws IOException, InterruptedException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        Mockito.doThrow(new IOException("")).when(mockHttpClient).send(any(), any());

        ResponseEntity<Object> responseEntityReturned = stubResponseController.forwardPutRequests(mockRequest);
        assertNotNull(responseEntityReturned);
        assertThat(responseEntityReturned.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * Forward DELETE requests unit test with status OK.
     */
    @Test
    @DisplayName("Test for forwardDeleteRequests() status OK")
    void shouldReturnStatusOK_ForwardDeleteRequests() throws IOException, InterruptedException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpResponse mockResponse = mock(HttpResponse.class);
        Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(), any());
        when(mockResponse.body()).thenReturn("MOCK BODY");
        when(mockResponse.statusCode()).thenReturn(200);

        ResponseEntity<Object> responseEntityReturned = stubResponseController.forwardDeleteRequests(mockRequest);
        assertNotNull(responseEntityReturned);
        assertThat(responseEntityReturned.getStatusCode(), is(HttpStatus.OK));
    }

    /**
     * Forward DELETE requests unit test with exception thrown.
     */
    @Test
    @DisplayName("Test for forwardDeleteRequests() exception thrown")
    void shouldThrowException_ForwardDeleteRequests() throws IOException, InterruptedException {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        Mockito.doThrow(new IOException("")).when(mockHttpClient).send(any(), any());

        ResponseEntity<Object> responseEntityReturned = stubResponseController.forwardDeleteRequests(mockRequest);
        assertNotNull(responseEntityReturned);
        assertThat(responseEntityReturned.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
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

    @Test
    @DisplayName("Should return correct UI Params")
    void shouldReturnUIParams() {
        URIBuilder builder = new URIBuilder();
        stubResponseController.addUriParams(builder,"scope", "clientid", "xuiwebapp");

        List<String> queryParameters = builder.getQueryParams()
            .stream()
            .map(p -> p.getValue()).toList();

        assertThat(queryParameters, hasItem("scope"));
        assertThat(queryParameters, hasItem("clientid"));
        assertThat(queryParameters, hasItem("http://localhost:5555/o"));
    }

    @ParameterizedTest
    @MethodSource("redirectToOauth2InvalidUriProvider")
    @DisplayName("redirectToOauth2 should handle invalid URIs")
    void redirectToOauth2ShouldHandleInvalidURIs(String redirectUri) {
        assertThrows(URISyntaxException.class, () -> {
            stubResponseController.redirectToOauth2(redirectUri, "scope", "state", "clientId");
        });
    }

    static Stream<String> redirectToOauth2InvalidUriProvider() {
        return Stream.of(
            "http://localhost:8080/callback with spaces",
            "http://localhost:8080/callback?query=<invalid>"
        );
    }
}
