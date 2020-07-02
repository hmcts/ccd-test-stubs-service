package uk.gov.hmcts.reform.ccd.test.stubs.service.token;

import com.nimbusds.jose.JOSEException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class JWTokenGeneratorTest {

    @Test
    void generateTokenStoreDynamic() throws JOSEException {
        String token = JWTokenGenerator.generateToken("http://localhost:5555/0", 50000);
        Assert.assertNotNull(token);
    }
}
