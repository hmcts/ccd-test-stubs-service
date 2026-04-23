package uk.gov.hmcts.reform.ccd.test.stubs.service.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.test.stubs.service.service.PrdStubStateService;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PrdStubStateControllerTest {

    private PrdStubStateController prdStubStateController;

    @BeforeEach
    void setUp() {
        prdStubStateController =
            new PrdStubStateController(new PrdStubStateService("/refdata/external/v1/organisations/users"));
    }

    @Test
    @DisplayName("Should return default PRD stub state when unset")
    void shouldReturnDefaultPrdStubStateWhenUnset() {
        ResponseEntity<Map<String, String>> responseEntity = prdStubStateController.getPrdOrganisationUsersStubState();

        assertNotNull(responseEntity);
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        assertThat(responseEntity.getBody().get("stubMode"), is("present"));
    }

    @Test
    @DisplayName("Should update PRD stub state")
    void shouldUpdatePrdStubState() {
        ResponseEntity<Map<String, String>> responseEntity =
            prdStubStateController.configurePrdOrganisationUsersStubState(Map.of("stubMode", "restricted-failed"));

        assertNotNull(responseEntity);
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
        assertThat(responseEntity.getBody().get("stubMode"), is("restricted-failed"));
    }
}
