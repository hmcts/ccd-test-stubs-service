package uk.gov.hmcts.reform.ccd.test.stubs.service.controllers;

import io.micrometer.core.instrument.util.IOUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.test.stubs.service.mock.server.MockHttpServer;
import uk.gov.hmcts.reform.ccd.test.stubs.service.token.JWTokenGenerator;

/**
 * Default endpoints per application.
 */
@RestController
@RequestMapping("/")
public class StubResponseController {

    private static final Logger LOG = LoggerFactory.getLogger(StubResponseController.class);

    @Value("${wiremock.server.host}")
    private String mockHttpServerHost;

    @Value("${app.management-web-url}")
    private String managementWebUrl;

    @Value("${app.jwt.secret}")
    private String privateKeyFileName;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.expiration}")
    private long expiration;

    private final RestTemplate restTemplate;

    private final MockHttpServer mockHttpServer;

    @Autowired
    public StubResponseController(RestTemplate restTemplate, MockHttpServer mockHttpServer) {
        this.restTemplate = restTemplate;
        this.mockHttpServer = mockHttpServer;
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ResponseEntity<Object> redirectToOauth2() throws URISyntaxException {
        URI oauth2Endpoint = new URI(managementWebUrl + "/oauth2redirect?code=54402a0b-e311-4788-b273-efc2c3fc53f0");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(oauth2Endpoint);
        return new ResponseEntity<>(httpHeaders, HttpStatus.FOUND);
    }

    @RequestMapping(value = "/o/token", method = RequestMethod.POST)
    public ResponseEntity<Object> openIdToken(HttpServletRequest request) {
        return createToken();
    }

    @RequestMapping(value = "/oauth2/token", method = RequestMethod.POST)
    public ResponseEntity<Object> oauth2Token(HttpServletRequest request) {
        return createToken();
    }

    private ResponseEntity<Object> createToken() {
        HashMap<String, Object> claims = new HashMap<>();
        HttpHeaders httpHeaders = new HttpHeaders();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("expires_in", 28800);
        body.put("access_token", JWTokenGenerator.generateToken(privateKeyFileName,
            issuer, expiration,
            claims));
        return new ResponseEntity<>(body, httpHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "**", method = RequestMethod.GET)
    public ResponseEntity<Object> forwardGetRequests(HttpServletRequest request) {
        return forwardAllRequests(request);
    }

    @RequestMapping(value = "**", method = RequestMethod.POST)
    public ResponseEntity<Object> forwardPostRequests(HttpServletRequest request) {
        return forwardAllRequests(request);
    }

    @RequestMapping(value = "**", method = RequestMethod.PUT)
    public ResponseEntity<Object> forwardPutRequests(HttpServletRequest request) {
        return forwardAllRequests(request);
    }

    @RequestMapping(value = "**", method = RequestMethod.DELETE)
    public ResponseEntity<Object> forwardDeleteRequests(HttpServletRequest request) {
        return forwardAllRequests(request);
    }

    private ResponseEntity<Object> forwardAllRequests(HttpServletRequest request) {
        try {
            String requestPath = new AntPathMatcher().extractPathWithinPattern("**", request.getRequestURI());
            LOG.info("Request path: {}", requestPath);
            String requestBody = IOUtils.toString(request.getInputStream(), Charset.forName(request.getCharacterEncoding()));

            return restTemplate.exchange(getMockHttpServerUrl(requestPath),
                                         HttpMethod.valueOf(request.getMethod()),
                                         new HttpEntity<>(requestBody),
                                         Object.class,
                                         request.getParameterMap());

        } catch (HttpClientErrorException e) {
            return new ResponseEntity<>(e.getResponseBodyAsByteArray(), e.getResponseHeaders(), e.getStatusCode());
        } catch (IOException e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
        }
    }

    private String getMockHttpServerUrl(String requestPath) {
        return "http://" + mockHttpServerHost + ":" + mockHttpServer.portNumber() + requestPath;
    }
}
