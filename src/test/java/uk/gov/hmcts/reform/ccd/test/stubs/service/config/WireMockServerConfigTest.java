package uk.gov.hmcts.reform.ccd.test.stubs.service.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;

class WireMockServerConfigTest {

    @Test
    void shouldSetWireMockConfig() {
        String directory = "";
        int portNumber = 9090;
        WireMockServerConfig config = new WireMockServerConfig(portNumber, directory);

        WireMockServer wireMockServer = config.wireMockServer();
        assertThat(wireMockServer.getOptions().portNumber(), is(portNumber));
        assertThat(wireMockServer.getOptions().filesRoot().getPath(), is(directory));
    }

    @Test
    void shouldSetWireMockPathWithResourcesDirectory() {
        String resourcesDirectory = "src/main/resources";
        WireMockServerConfig config = new WireMockServerConfig(9090, resourcesDirectory);

        WireMockServer wireMockServer = config.wireMockServer();
        assertThat(wireMockServer.getOptions().filesRoot().getPath(), is(not(isEmptyString())));
    }


}
