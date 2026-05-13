package uk.gov.hmcts.reform.ccd.test.stubs.service.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.test.stubs.service.service.PrdStubStateService;

import java.util.Map;

@RestController
@RequestMapping("/stub-state/prd-organisations-users")
public class PrdStubStateController {

    private static final Logger LOG = LoggerFactory.getLogger(PrdStubStateController.class);
    private static final String STUB_MODE_FIELD = "stubMode";

    private final PrdStubStateService prdStubStateService;

    public PrdStubStateController(PrdStubStateService prdStubStateService) {
        this.prdStubStateService = prdStubStateService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getPrdOrganisationUsersStubState() {
        return ResponseEntity.ok(Map.of(STUB_MODE_FIELD, prdStubStateService.getStubMode()));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> configurePrdOrganisationUsersStubState(
        @RequestBody Map<String, String> requestBody
    ) {
        prdStubStateService.setStubMode(requestBody == null ? null : requestBody.get(STUB_MODE_FIELD));
        logPrdOrganisationUsersStubStateUpdated();
        return ResponseEntity.ok(Map.of(STUB_MODE_FIELD, prdStubStateService.getStubMode()));
    }

    private void logPrdOrganisationUsersStubStateUpdated() {
        LOG.info("PRD organisation users stub state updated");
    }
}
