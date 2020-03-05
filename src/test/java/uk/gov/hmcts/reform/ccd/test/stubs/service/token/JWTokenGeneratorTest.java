package uk.gov.hmcts.reform.ccd.test.stubs.service.token;

import io.jsonwebtoken.Claims;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

class JWTokenGeneratorTest {

    @Test
    void generateToken() {
        Map<String, Object> map = new HashMap<>();
        map.put("Test", "12345");
        String token = JWTokenGenerator.generateToken("private_key.der", "http://localhost:5555/0", 50000, map);
        Assert.assertNotNull(token);

        Claims claims = JWTokenParser.parseToken("public_key.der", token);
        Assert.assertNotNull(claims);
        Assert.assertEquals(claims.get("Test"), "12345");
    }
}
