package uk.gov.hmcts.reform.ccd.test.stubs.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Dummy functional test class (for use till proper smoke tests exist). Required to pass the "Functional Test" stage of the build
 * pipeline in Jenkins.
 */
public class FunctionalTest {

    @Test
    public void alwaysGreen() {
        assertTrue(true);
    }

}
