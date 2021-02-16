package uk.gov.hmcts.reform.ccd.test.stubs.service.config;

import java.io.File;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WireMockServerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(WireMockServerConfig.class);
    private static final String MAPPINGS_DIRECTORY_NAME = "/mappings";

    private final String mappingsPath;

    @Autowired
    public WireMockServerConfig(@Value("${wiremock.server.mappings-path}") String mappingsPath) {
        this.mappingsPath = mappingsPath;
    }

    @Bean
    public WireMockServer wireMockServer() {
        LOG.info("WireMock mappings file path: {}", mappingsPath);

        WireMockServer wireMockServer = new WireMockServer(getWireMockConfig());

        LOG.info("Stubs registered with wiremock");
        wireMockServer.getStubMappings().forEach(w -> LOG.info("\nRequest : {}, \nResponse: {}", w.getRequest(),
                w.getResponse()));

        return wireMockServer;
    }

    private WireMockConfiguration getWireMockConfig() {
        File mappingDirectory = new File(mappingsPath + MAPPINGS_DIRECTORY_NAME);
        LOG.info("Mappings directory path: {}", mappingDirectory.getAbsolutePath());

        if (mappingDirectory.isDirectory()) {
            return options()
                .dynamicHttpsPort()
                .dynamicPort()
                .usingFilesUnderDirectory(mappingsPath)
                .extensions("uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension"
                                + ".DynamicCaseDataResponseTransformer",
                            "uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension"
                                + ".DynamicRoleAssignmentsResponseTransformer");
        } else {
            LOG.info("using classpath resources to resolve mappings");
            return options()
                .dynamicHttpsPort()
                .dynamicPort()
                .usingFilesUnderClasspath(mappingsPath)
                .extensions("uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension"
                                + ".DynamicCaseDataResponseTransformer",
                            "uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension"
                                + ".DynamicRoleAssignmentsResponseTransformer");
        }
    }
}
