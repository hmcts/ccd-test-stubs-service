package uk.gov.hmcts.reform.ccd.test.stubs.service.controllers;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import io.micrometer.core.instrument.util.IOUtils;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.test.stubs.service.mock.server.MockHttpServer;
import uk.gov.hmcts.reform.ccd.test.stubs.service.token.JWTokenGenerator;
import uk.gov.hmcts.reform.ccd.test.stubs.service.token.KeyGenUtil;

/**
 * Default endpoints per application.
 */
@RestController
@RequestMapping("/")
public class StubResponseController {

	private static final String DEFAULT_USER = "divorce-solicitor@gmail.com";
	private static final Logger LOG = LoggerFactory.getLogger(StubResponseController.class);
	static final String WIREMOCK_STUB_MAPPINGS_ENDPOINT = "/__admin/mappings";

	@Value("${wiremock.server.host}")
	private String mockHttpServerHost;

	@Value("${app.management-web-url}")
	private String managementWebUrl;

	@Value("${app.jwt.issuer}")
	private String issuer;

	@Value("${app.jwt.expiration}")
	private long expiration;

	@Value("classpath:userInfoOverrideRequestTemplate.json")
	private Resource userInfoRequestTemplate;

	private final RestTemplate restTemplate;

	private final MockHttpServer mockHttpServer;
	private final ObjectMapper mapper;

	@Autowired
	public StubResponseController(RestTemplate restTemplate, MockHttpServer mockHttpServer, ObjectMapper mapper) {
		this.restTemplate = restTemplate;
		this.mockHttpServer = mockHttpServer;
		this.mapper = mapper;
	}

	@GetMapping(value = "/login")
	public ResponseEntity<Object> redirectToOauth2() throws URISyntaxException {
		URI oauth2Endpoint = new URI(managementWebUrl + "/oauth2redirect?code=54402a0b-e311-4788-b273-efc2c3fc53f0");
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
		keyList.add(rsaKey.toJSONObject());
		body.put("keys", keyList);
		HttpHeaders httpHeaders = new HttpHeaders();

		return new ResponseEntity<>(body, httpHeaders, HttpStatus.OK);
	}

	@PostMapping(value = "/o/token")
	public ResponseEntity<Object> openIdToken(HttpServletRequest request) throws JOSEException {
		log(request);
		String username = getUsernameFrom(bodyOf(request));
		return createToken(username);
	}

	private String bodyOf(HttpServletRequest request) {
		try {
			return IOUtils.toString(request.getInputStream(), Charset.forName(request.getCharacterEncoding()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@GetMapping(value = "/details")
	public ResponseEntity<Object> getDetails(HttpServletRequest request) throws JOSEException {
		log(request);
		String username = getUsernameFromToken(request.getHeader("Authorization"));
		return forwardAllRequests(request, username);
	}

	@GetMapping(value = "/o/userinfo")
	public ResponseEntity<Object> getUserInfo(HttpServletRequest request) throws JOSEException {
		log(request);
		String username = getUsernameFromToken(request.getHeader("Authorization"));
		return forwardAllRequests(request, username);
	}

	private String getUsernameFrom(String body) {
		//Log output:
		//Basic Y2NkX2dhdGV3YXk6Y2NkX2dhdGV3YXlfc2VjcmV0
		//Equals: ccd_gateway:ccd_gateway_secret
		LOG.info("Getting user from body: {}", body);
		if (body == null)
			return "no_username_in_request_body";
		String[] parts = body.split("&");
		for (String part : parts) {
			if(part.toLowerCase().startsWith("username")){
				return part.substring(9);
			}
		}
		return DEFAULT_USER;
	}

	private void log(HttpServletRequest request) {
		try {
			String requestPath = new AntPathMatcher().extractPathWithinPattern("**", request.getRequestURI());
			String userToken = request.getHeader("Authorization");

			LOG.info("\n\n\n------------------\nRequest: {} {} \nToken:{}\nuser:{}\n======\n", request.getMethod(),
					requestPath, userToken, getUsernameFromToken(userToken));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String getUsernameFromToken(String userToken) {
		LOG.info("Getting user from token >> {}", userToken);
		try {
			if (userToken != null) {
				JWT jwt = JWTParser.parse(userToken.substring(7));
				return jwt.getJWTClaimsSet().getSubject();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return DEFAULT_USER;
	}

	@PostMapping(value = "/oauth2/token")
	public ResponseEntity<Object> oauth2Token(HttpServletRequest request) throws JOSEException {
		log(request);
		String username = getUsernameFrom(bodyOf(request));
		return createToken(username);
	}

	private ResponseEntity<Object> createToken(String username) throws JOSEException {
		Map<String, Object> body = new LinkedHashMap<>();
		String token = JWTokenGenerator.generateToken(issuer, expiration, username);
		body.put("access_token", token);
		body.put("token_type", "Bearer");
		body.put("expires_in", expiration);
		body.put("id_token", token);
		HttpHeaders httpHeaders = new HttpHeaders();
		return new ResponseEntity<>(body, httpHeaders, HttpStatus.OK);
	}

	@GetMapping(value = "**")
	public ResponseEntity<Object> forwardGetRequests(HttpServletRequest request) {
		return forwardAllRequests(request);
	}

	@PostMapping(value = "**")
	public ResponseEntity<Object> forwardPostRequests(HttpServletRequest request) {
		return forwardAllRequests(request);
	}

	@PutMapping(value = "**")
	public ResponseEntity<Object> forwardPutRequests(HttpServletRequest request) {
		return forwardAllRequests(request);
	}

	@DeleteMapping(value = "**")
	public ResponseEntity<Object> forwardDeleteRequests(HttpServletRequest request) {
		return forwardAllRequests(request);
	}

	private ResponseEntity<Object> forwardAllRequests(HttpServletRequest request) {
		return forwardAllRequests(request, null);
	}

	private ResponseEntity<Object> forwardAllRequests(HttpServletRequest request, String username) {
		try {
			String requestPath = new AntPathMatcher().extractPathWithinPattern("**", request.getRequestURI());
			if (username != null) {
				requestPath += "/" + username;
			}
			String requestBody = bodyOf(request);

			ResponseEntity<Object> response = restTemplate.exchange(getMockHttpServerUrl(requestPath),
					HttpMethod.valueOf(request.getMethod()), new HttpEntity<>(requestBody), Object.class,
					request.getParameterMap());
			return response;
		} catch (HttpClientErrorException e) {
			return new ResponseEntity<>(e.getResponseBodyAsByteArray(), e.getResponseHeaders(), e.getStatusCode());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
	}

	@PostMapping(path = "/idam-user", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> configureUser(@RequestBody IdamUserInfo userInfo) throws JsonProcessingException {

		LOG.info("setting stub user info to: {}", asJson(userInfo));

		String request = createWiremockRequestForUserInfo(asJson(userInfo));
		String requestUrl = getMockHttpServerUrl(WIREMOCK_STUB_MAPPINGS_ENDPOINT);

		try {
			ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(requestUrl, request, String.class);
			stringResponseEntity.getStatusCodeValue();
			return ResponseEntity.ok().build();
		} catch (HttpClientErrorException | HttpServerErrorException e) {
			LOG.error("Error configuring stub IDAM user", e);
			return new ResponseEntity<>("Some error occurred", e.getStatusCode());
		} catch (Exception e) {
			LOG.error("Error configuring stub IDAM user", e);
			return new ResponseEntity<>("Some unknown error occurred", HttpStatus.NO_CONTENT);
		}
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
}
