package uk.gov.hmcts.reform.ccd.test.stubs.service.mock.server;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WireMockHttpServer implements MockHttpServer {

    private static final Logger LOG = LoggerFactory.getLogger(WireMockHttpServer.class);

    private final WireMockServer wireMockServer;

    @Autowired
    public WireMockHttpServer(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    @PostConstruct
    @Override
    public void start() {
        //this.wireMockServer.start();
        LOG.info("Started WireMock server on port {}", wireMockServer.port());
    }

    @PreDestroy
    @Override
    public void stop() {
        this.wireMockServer.stop();
    }

}
