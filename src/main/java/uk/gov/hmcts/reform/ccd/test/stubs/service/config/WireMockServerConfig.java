package uk.gov.hmcts.reform.ccd.test.stubs.service.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@Configuration
public class WireMockServerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(WireMockServerConfig.class);
    private static final String MAPPINGS_DIRECTORY_NAME = "/mappings";
    private static final String WIREMOCK_EXTENSION_PREFIX =
            "uk.gov.hmcts.reform.ccd.test.stubs.service.wiremock.extension";

    private final String mappingsPath;

    @Autowired
    public WireMockServerConfig(@Value("${wiremock.server.mappings-path}") String mappingsPath) {
        this.mappingsPath = mappingsPath;
    }

    @Bean
    public WireMockServer wireMockServer() {
        LOG.info("WireMock mappings file path: {}", mappingsPath);

        var wireMockServer = new WireMockServer(getWireMockConfig());

        LOG.info("Stubs registered with wiremock");
        wireMockServer.getStubMappings().forEach(w -> LOG.info("\nRequest : {}, \nResponse: {}", w.getRequest(),
                w.getResponse()));

        return wireMockServer;
    }

    private WireMockConfiguration getWireMockConfig() {
        var mappingDirectory = new File(mappingsPath + MAPPINGS_DIRECTORY_NAME);
        LOG.info("Mappings directory path: {}", mappingDirectory.getAbsolutePath());
        var extension1 = WIREMOCK_EXTENSION_PREFIX + ".DynamicCaseDataResponseTransformer";
        var extension2 = WIREMOCK_EXTENSION_PREFIX + ".DynamicRoleAssignmentsResponseTransformer";
        var extension3 = WIREMOCK_EXTENSION_PREFIX + ".DynamicCaseCaseDataResponseTransformer";
        var extension4 = WIREMOCK_EXTENSION_PREFIX + ".DynamicTTLNullResponseTransformer";
        var extension5 = WIREMOCK_EXTENSION_PREFIX + ".DynamicTTLRemoveResponseTransformer";
        var extension6 = WIREMOCK_EXTENSION_PREFIX + ".DynamicTTLUppercaseResponseTransformer";

        if (mappingDirectory.isDirectory()) {
            return options()
                .dynamicHttpsPort()
                .dynamicPort()
                .usingFilesUnderDirectory(mappingsPath)
                .templatingEnabled(true)
                .extensions(extension1, extension2, extension3, extension4, extension5, extension6);
        } else {
            LOG.info("using classpath resources to resolve mappings");
            return options()
                .dynamicHttpsPort()
                .dynamicPort()
                .usingFilesUnderClasspath(mappingsPath)
                .templatingEnabled(true)
                .extensions(extension1, extension2, extension3, extension4, extension5, extension6);
        }
    }
}
