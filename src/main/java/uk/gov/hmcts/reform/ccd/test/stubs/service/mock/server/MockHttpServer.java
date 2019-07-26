package uk.gov.hmcts.reform.ccd.test.stubs.service.mock.server;

public interface MockHttpServer {

    void start();

    void stop();

    int portNumber();
}
