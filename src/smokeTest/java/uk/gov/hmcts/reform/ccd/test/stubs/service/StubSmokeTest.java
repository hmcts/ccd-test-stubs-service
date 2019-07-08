package uk.gov.hmcts.reform.ccd.test.stubs.service;

import static org.hamcrest.core.Is.is;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

class StubSmokeTest {

    private static final String URL = "/case_type/aat/dynamic/about_to_submit";

    @Test
    void stubTest() {
        withDefaultRequestSpec()
            .contentType(ContentType.JSON)
            .body("{ \"case_data\":{ \"PersonFirstName\":\"FirstName\", \"PersonLastName\": \"Name\" }}")
            .post(URL)
            .then()
            .statusCode(200)
            .body("case_data.CallbackText", is("test"))
            .body("case_data.PersonFirstName", is("FirstName"))
            .body("case_data.PersonLastName", is("LastName"))
            .log()
            .all();
    }

    private RequestSpecification withDefaultRequestSpec() {
        return RestAssured.given(new RequestSpecBuilder()
                                     .setBaseUri(SmokeTestHelper.getTestUrl())
                                     .build());
    }

}
