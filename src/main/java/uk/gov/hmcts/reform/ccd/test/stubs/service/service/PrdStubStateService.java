package uk.gov.hmcts.reform.ccd.test.stubs.service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class PrdStubStateService {

    static final String DEFAULT_STUB_MODE = "present";

    private final String prdOrganisationUsersPath;
    private final AtomicReference<String> prdOrganisationUsersStubMode = new AtomicReference<>();

    public PrdStubStateService(
        @Value("${app.prd.organisation-users-path:/refdata/external/v1/organisations/users}")
        String prdOrganisationUsersPath
    ) {
        this.prdOrganisationUsersPath = prdOrganisationUsersPath;
    }

    public String getStubMode() {
        return normaliseStubMode(prdOrganisationUsersStubMode.get());
    }

    public void setStubMode(String stubMode) {
        prdOrganisationUsersStubMode.set(normaliseStubMode(stubMode));
    }

    public boolean hasStoredStubMode() {
        return prdOrganisationUsersStubMode.get() != null;
    }

    public boolean isPrdOrganisationUsersPath(String requestPath) {
        return prdOrganisationUsersPath.equals(requestPath);
    }

    private String normaliseStubMode(String stubMode) {
        return (stubMode == null || stubMode.isBlank()) ? DEFAULT_STUB_MODE : stubMode;
    }
}
