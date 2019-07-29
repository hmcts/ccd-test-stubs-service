package uk.gov.hmcts.reform.ccd.test.stubs.service.controllers;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ccd.test.stubs.service.mock.server.MockHttpServer;

class StubResponseControllerTest {

    private StubResponseController stubResponseController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        stubResponseController = new StubResponseController(mock(RestTemplate.class), mock(MockHttpServer.class));
    }

    @Test
    @DisplayName("Should return internal server error on invalid request input stream")
    void shouldReturnInternalServerError() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        doThrow(new IOException("")).when(request).getInputStream();

        ResponseEntity<Object> responseEntity = stubResponseController.forwardGetRequests(request);
        assertThat(responseEntity.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
