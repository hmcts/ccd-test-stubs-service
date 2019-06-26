package uk.gov.hmcts.reform.ccd.test.stubs.service.mock.server;

import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class WireMockHttpServerTest {

    @Mock
    private WireMockServer wireMockServer;

    private WireMockHttpServer wireMockHttpServer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        wireMockHttpServer = new WireMockHttpServer(wireMockServer);
    }

    @DisplayName("Should start wiremock server")
    @Test
    void shouldStartWireMockServer() {
        wireMockHttpServer.start();

        verify(wireMockServer).start();
    }

    @DisplayName("Should stop wiremock server")
    @Test
    void shouldStopWireMockServer() {
        wireMockHttpServer.stop();

        verify(wireMockServer).stop();
    }
}
