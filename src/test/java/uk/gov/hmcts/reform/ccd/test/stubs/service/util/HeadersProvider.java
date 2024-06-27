package uk.gov.hmcts.reform.ccd.test.stubs.service.util;

import java.util.List;
import java.util.Map;

public interface HeadersProvider {
    Map<String, List<String>> getHeaders();
}
