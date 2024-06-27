package uk.gov.hmcts.reform.ccd.test.stubs.service.util;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

public class HttpHeadersProvider implements HeadersProvider {
    private final HttpHeaders httpHeaders;

    public HttpHeadersProvider(HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return httpHeaders.map();
    }
}
