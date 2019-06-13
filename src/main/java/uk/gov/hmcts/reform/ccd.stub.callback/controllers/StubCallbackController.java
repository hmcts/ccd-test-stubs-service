package uk.gov.hmcts.reform.ccd.stub.callback.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Default endpoints per application.
 */
@RestController
@RequestMapping("/callbacks")
public class StubCallbackController {

    private static final Logger LOG = LoggerFactory.getLogger(StubCallbackController.class);

    private StubJsonResponseReader stubJsonResponseReader;

    @Autowired
    public StubCallbackController(StubJsonResponseReader stubJsonResponseReader) {
        this.stubJsonResponseReader = stubJsonResponseReader;
    }

    @PostMapping("/responseWithRequestData")
    public ResponseEntity<JsonNode> responseWithRequestData(@RequestBody JsonNode request) {
        LOG.info("Returning response with request data");
        LOG.info("request: {}", request);

        JsonNode response = StubResponseBuilder.create(request).build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/responseWithRequestDataAndStubbedData/{jsonStubName}")
    public ResponseEntity<JsonNode> responseWithRequestDataAndStubbedData(@PathVariable String jsonStubName, @RequestBody JsonNode request) {
        LOG.info("Returning response with request data and static data :{}", jsonStubName);
        LOG.info("request: {}", request);

        JsonNode response = StubResponseBuilder.create(request)
            .withDataFields(stubJsonResponseReader.getStubJsonResponse(jsonStubName))
            .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/responseWithStubbedData/{jsonStubName}")
    public ResponseEntity<JsonNode> responseWithStubbedData(@PathVariable String jsonStubName, @RequestBody JsonNode request) {
        LOG.info("Returning response with static stubbed data :{}", jsonStubName);

        JsonNode response = StubResponseBuilder.create()
            .withDataFields(stubJsonResponseReader.getStubJsonResponse(jsonStubName))
            .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/responseWithError")
    public ResponseEntity<String> responseWithError(@RequestBody JsonNode request) {
        LOG.info("Returning response with error");
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body("Stub exception");
    }

    @PostMapping("/nullResponse")
    public ResponseEntity<JsonNode> nullResponse(@RequestBody JsonNode request) {
        LOG.info("Returning null response");

        return ResponseEntity.ok(NullNode.getInstance());
    }

}
