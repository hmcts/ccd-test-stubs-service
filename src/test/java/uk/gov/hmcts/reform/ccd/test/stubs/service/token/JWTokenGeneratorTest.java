package uk.gov.hmcts.reform.ccd.test.stubs.service.token;

import com.nimbusds.jose.JOSEException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class JWTokenGeneratorTest {

    @Test
    void generateTokenStoreDynamic() throws JOSEException {
        Map<String, Object> map = new HashMap<>();
        String token = JWTokenGenerator.generateToken("http://localhost:5555/0", 50000);
        Assert.assertNotNull(token);
    }
}
