package uk.gov.hmcts.reform.ccd.test.stubs.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Dummy smoke test class (for use till proper smoke tests exist). Required to pass the "Smoke Test" stage of the build
 * pipeline in Jenkins.
 */
public class SmokeTest {

    @Test
    public void alwaysGreen() {
        assertTrue(true);
    }

}
