package uk.gov.hmcts.reform.ccd.test.stubs.service;

import static org.hamcrest.core.Is.is;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("functional")
class StubFunctionalTest {

    private static final String URL = "/case_type/aat/about_to_start";

    @Test
    void stubTest() {
        withDefaultRequestSpec()
            .contentType(ContentType.JSON)
            .post(URL)
            .then()
            .statusCode(200)
            .body("data.CallbackText", is("test"))
            .body("data.PersonLastName", is("LastName"))
            .log()
            .all();
    }

    private RequestSpecification withDefaultRequestSpec() {
        RestAssured.useRelaxedHTTPSValidation();
        return RestAssured.given(new RequestSpecBuilder()
                                     .setBaseUri("http://localhost:5555")
                                     .build());
    }

}
