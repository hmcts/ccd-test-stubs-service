package uk.gov.hmcts.reform.ccd.test.stubs.service.config;

import java.io.File;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WireMockServerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(WireMockServerConfig.class);

    @Value("${wiremock.server.port}")
    private int port;

    @Value("${wiremock.server.mappings-path}")
    private String mappingsPath;

    @Bean
    public WireMockServer wireMockServer() {
        LOG.info("WireMock port: {}, mappings file path: {}", port, mappingsPath);

        WireMockServer wireMockServer = new WireMockServer(options()
                                                               .port(port)
                                                               .usingFilesUnderDirectory(mappingsPath)
                                                               .extensions("uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension"
                                                                               + ".DynamicCaseDataResponseTransformer"));

        File mappingDirectory = new File(mappingsPath);
        LOG.info("Mappings directory path: {}", mappingDirectory.getAbsolutePath());

        LOG.info("Stubs registered with wiremock");
        wireMockServer.getStubMappings().forEach(w -> LOG.info("\nRequest : {}, \nResponse: {}", w.getRequest(), w.getResponse()));

        return wireMockServer;
    }

}
