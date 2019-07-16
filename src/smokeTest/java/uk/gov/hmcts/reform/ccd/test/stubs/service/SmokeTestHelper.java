package uk.gov.hmcts.reform.ccd.test.stubs.service;

import org.apache.commons.lang3.Validate;

public class SmokeTestHelper {

    private SmokeTestHelper() {
    }

    public static String getTestUrl() {
        return getEnvVariable("TEST_URL");
    }

    private static String getEnvVariable(String name) {
        return Validate.notNull(System.getenv(name), "Environment variable `%s` is required", name);
    }

}
