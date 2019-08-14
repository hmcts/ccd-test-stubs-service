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
        String directory = "wiremock";
        WireMockServerConfig config = new WireMockServerConfig(directory);

        WireMockServer wireMockServer = config.wireMockServer();
        assertThat(wireMockServer.getOptions().filesRoot().getPath(), is(directory));
    }

    @Test
    void shouldSetWireMockPathWithResourcesDirectory() {
        String resourcesDirectory = "src/main/resources";
        WireMockServerConfig config = new WireMockServerConfig(resourcesDirectory);

        WireMockServer wireMockServer = config.wireMockServer();
        assertThat(wireMockServer.getOptions().filesRoot().getPath(), is(not(isEmptyString())));
    }

}
