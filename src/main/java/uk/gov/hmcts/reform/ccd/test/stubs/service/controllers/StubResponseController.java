package uk.gov.hmcts.reform.ccd.test.stubs.service.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import io.micrometer.core.instrument.util.IOUtils;
import net.minidev.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.hmcts.reform.ccd.test.stubs.service.mock.server.MockHttpServer;
import uk.gov.hmcts.reform.ccd.test.stubs.service.token.JWTokenGenerator;
import uk.gov.hmcts.reform.ccd.test.stubs.service.token.KeyGenUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.joining;

/**
 * Default endpoints per application.
 */
@RestController
@RequestMapping("/")
public class StubResponseController {

    private static final Logger LOG = LoggerFactory.getLogger(StubResponseController.class);
    static final String WIREMOCK_STUB_MAPPINGS_ENDPOINT = "/__admin/mappings";
    static final List<String> CUSTOM_HEADERS = List.of("Client-Context");


    @Value("${wiremock.server.host}")
    private String mockHttpServerHost;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.expiration}")
    private long expiration;

    @Value("classpath:userInfoOverrideRequestTemplate.json")
    private Resource userInfoRequestTemplate;

    private final HttpClient httpClient;

    private final MockHttpServer mockHttpServer;
    private final ObjectMapper mapper;

    @Autowired
    public StubResponseController(HttpClient httpClient, MockHttpServer mockHttpServer, ObjectMapper mapper) {
        this.httpClient = httpClient;
        this.mockHttpServer = mockHttpServer;
        this.mapper = mapper;
    }

    @GetMapping(value = "/login")
    public ResponseEntity<Object> redirectToOauth2(@RequestParam("redirect_uri") final String redirectUri,
                                                   @RequestParam(value = "scope", required = false) final String scope,
                                                   @RequestParam(value = "state", required = false) final String state,
                                                   @RequestParam(value = "client_id", required = false)
                                                       final String clientId) throws URISyntaxException {
        URIBuilder builder = new URIBuilder(redirectUri);
        builder.addParameter("code", "54402a0b-e311-4788-b273-efc2c3fc53f0");
        addUriParams(builder, scope, state, clientId);
        URI oauth2Endpoint = builder.build();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(oauth2Endpoint);
        return new ResponseEntity<>(httpHeaders, HttpStatus.FOUND);
    }

    @GetMapping(value = "/o/jwks")
    public ResponseEntity<Object> jwkeys(HttpServletRequest request) throws JOSEException {
        return getPublicKey();
    }

    private ResponseEntity<Object> getPublicKey() throws JOSEException {
        RSAKey rsaKey = KeyGenUtil.getRsaJWK();
        Map<String, List<JSONObject>> body = new LinkedHashMap<>();
        List<JSONObject> keyList = new ArrayList<>();
        keyList.add(new JSONObject(rsaKey.toPublicJWK().toJSONObject()));
        body.put("keys", keyList);
        HttpHeaders httpHeaders = new HttpHeaders();

        return new ResponseEntity<>(body, httpHeaders, HttpStatus.OK);
    }

    @PostMapping(value = "/o/token")
    public ResponseEntity<Object> openIdToken(HttpServletRequest request) throws JOSEException {
        return createToken();
    }

    @PostMapping(value = "/oauth2/token")
    public ResponseEntity<Object> oauth2Token(HttpServletRequest request) throws JOSEException {
        return createToken();
    }

    private ResponseEntity<Object> createToken() throws JOSEException {
        Map<String, Object> body = new LinkedHashMap<>();
        String token = JWTokenGenerator.generateToken(issuer, expiration);
        body.put("access_token", token);
        body.put("token_type", "Bearer");
        body.put("expires_in", expiration);
        body.put("id_token", token);
        HttpHeaders httpHeaders = new HttpHeaders();
        return new ResponseEntity<>(body, httpHeaders, HttpStatus.OK);
    }

    /**
     * Forward GET requests to Wiremock Server and return GET responses to Test Stub Client.
     */
    @GetMapping(value = "**", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> forwardGetRequests(HttpServletRequest request) throws InterruptedException {
        try {
            String requestPath = new AntPathMatcher().extractPathWithinPattern("**", request.getRequestURI());
            Map<String, String[]> parameterMap = request.getParameterMap();
            URI uri = URI.create(getMockHttpServerUrl(requestPath, parameterMap));

            HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .build();
            HttpResponse httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return new ResponseEntity<>(httpResponse.body().toString(),
                HttpStatus.valueOf(httpResponse.statusCode()));
        } catch (IOException e) {
            LOG.error("Error occurred", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
        }
    }

    /**
     * Forward POST requests to Wiremock Server and return POST responses to Test Stub Client.
     */
    @PostMapping(value = "**", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> forwardPostRequests(HttpServletRequest request) throws InterruptedException {
        try {
            String requestPath = new AntPathMatcher().extractPathWithinPattern("**", request.getRequestURI());
            final String requestBody = IOUtils.toString(request.getInputStream());
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(getMockHttpServerUrl(requestPath)))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            HttpResponse httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            HttpHeaders customHeaders = getCustomHeaders(httpResponse.headers());

            if (customHeaders.size() > 0) {
                return new ResponseEntity<>(httpResponse.body().toString(),
                    customHeaders,
                    HttpStatus.valueOf(httpResponse.statusCode()));
            } else {
                return new ResponseEntity<>(httpResponse.body().toString(),
                    HttpStatus.valueOf(httpResponse.statusCode()));
            }
        } catch (IOException e) {
            LOG.error("Error occurred", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
        }
    }

    /**
     * Forward PUT requests to Wiremock Server and return PUT responses to Test Stub Client.
     */
    @PutMapping(value = "**", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> forwardPutRequests(HttpServletRequest request) throws InterruptedException {
        try {
            String requestPath = new AntPathMatcher().extractPathWithinPattern("**", request.getRequestURI());
            final String requestBody = IOUtils.toString(request.getInputStream());
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(getMockHttpServerUrl(requestPath)))
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            HttpResponse httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return new ResponseEntity<>(httpResponse.body().toString(),
                HttpStatus.valueOf(httpResponse.statusCode()));
        } catch (IOException e) {
            LOG.error("Error occurred", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
        }
    }

    /**
     * Forward DELETE requests to Wiremock Server and return DELETE responses to Test Stub Client.
     */
    @DeleteMapping(value = "**")
    public ResponseEntity<Object> forwardDeleteRequests(HttpServletRequest request) throws InterruptedException {
        try {
            String requestPath = new AntPathMatcher().extractPathWithinPattern("**", request.getRequestURI());
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(getMockHttpServerUrl(requestPath)))
                .DELETE()
                .build();
            HttpResponse httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return new ResponseEntity<>(httpResponse.body().toString(),
                HttpStatus.valueOf(httpResponse.statusCode()));
        } catch (IOException e) {
            LOG.error("Error occurred", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
        }
    }

    /**
     * Change the stubbed user info at runtime by posting the desired user info.
     */
    @PostMapping(
        path = "/idam-user",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> configureUser(@RequestBody IdamUserInfo userInfo)
        throws JsonProcessingException, InterruptedException {
        LOG.info("setting stub user info to: {}", asJson(userInfo));

        String request = createWiremockRequestForUserInfo(asJson(userInfo));
        String requestUrl = getMockHttpServerUrl(WIREMOCK_STUB_MAPPINGS_ENDPOINT);

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(requestUrl))
                .POST(HttpRequest.BodyPublishers.ofString(request))
                .build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            ResponseEntity<String> stringResponseEntity = new ResponseEntity<String>(httpResponse.body().toString(),
                HttpStatus.valueOf(httpResponse.statusCode()));

            stringResponseEntity.getStatusCode();
            return ResponseEntity.ok().build();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOG.error("Error configuring stub IDAM user", e);
            return new ResponseEntity<>("Some error occurred", e.getStatusCode());
        } catch (IOException e) {
            LOG.error("Error configuring stub IDAM user", e);
            return new ResponseEntity<>("Some unknown error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private HttpHeaders getCustomHeaders(java.net.http.HttpHeaders originalHeaders) {

        HttpHeaders springHeaders = new HttpHeaders();
        CUSTOM_HEADERS.forEach(context -> {
            if (null != originalHeaders && originalHeaders.map().containsKey(context.toLowerCase())) {
                springHeaders.put(context, originalHeaders.map().get(context.toLowerCase()));
            }
        });

        return springHeaders;
    }

    private String asJson(Object object) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    private String createWiremockRequestForUserInfo(String userInfoAsJson) {
        String requestTemplate = asString(userInfoRequestTemplate);
        return requestTemplate.replace("$USER_INFO_BODY_PLACEHOLDER", userInfoAsJson);
    }

    private static String asString(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String getMockHttpServerUrl(String requestPath) {
        return "http://" + mockHttpServerHost + ":" + mockHttpServer.portNumber() + requestPath;
    }

    private String getMockHttpServerUrl(String requestPath, Map<String, String[]> parameterMap) {
        return parameterMap.entrySet().stream()
            .map(entry -> String.join(
                "=",
                entry.getKey(),
                entry.getValue()[0]))
            .collect(
                collectingAndThen(
                    joining("&"),
                    params ->
                        params.length() > 0
                            ? String.join(
                                    "",
                                getMockHttpServerUrl(requestPath),
                                "?",
                                params)
                            : getMockHttpServerUrl(requestPath)
                )
            );
    }

    void addUriParams(URIBuilder builder, final String scope,
                      final String state,
                      final String clientId) {
        if ("xuiwebapp".equalsIgnoreCase(clientId)
            || "xui_webapp".equalsIgnoreCase(clientId)) {
            String localIss = "http://localhost:5555/o";
            builder.addParameter("scope", scope);
            builder.addParameter("state", state);
            builder.addParameter("iss", localIss);
        }
    }
}
