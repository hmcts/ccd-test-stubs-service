package uk.gov.hmcts.reform.ccd.test.stubs.service.token;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public final class JWTokenGenerator {

    public static String generateToken(String privateKeyFileName,
                                       String issuer,
                                       long ttlMillis,
                                       Map<String, Object> claims) {
        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;

        final long nowMillis = System.currentTimeMillis();
        final Date now = new Date(nowMillis);

        final Key signingKey = KeyUtil.getPrivateKey(privateKeyFileName);

        final JwtBuilder builder = Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .setIssuer(issuer)
            .setIssuedAt(now)
            .setClaims(claims)
            .signWith(signingKey, signatureAlgorithm);

        if (ttlMillis >= 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp);
        }

        return builder.compact();
    }
}
