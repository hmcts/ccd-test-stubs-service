package uk.gov.hmcts.reform.ccd.test.stubs.service.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.Key;

public final class JWTokenParser {

    private JWTokenParser() {
    }

    public static Claims parseToken(String publicKeyFileName, String jwt) {
        final Key signingKey = KeyUtil.getPublicKey(publicKeyFileName);
        return Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(jwt)
            .getBody();
    }
}
