package uk.gov.hmcts.reform.ccd.test.stubs.service.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.util.Date;

public final class JWTokenGenerator {


    private JWTokenGenerator() {
    }

    /**
     * Generate JWT Signed Token.
     * @param issuer    Issuer
     * @param ttlMillis Time to live
     * @return String
     */
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public static String generateToken(String issuer, long ttlMillis) throws JOSEException {
        final long nowMillis = System.currentTimeMillis();

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
            .subject("CCD_Stub")
            .issueTime(new Date())
            .issuer(issuer)
            .claim("tokenName", "access_token");

        if (ttlMillis >= 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.expirationTime(exp);
        }

        SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(KeyGenUtil.getRsaJWK().getKeyID()).build(),
            builder.build());
        signedJWT.sign(new RSASSASigner(KeyGenUtil.getRsaJWK()));

        return signedJWT.serialize();
    }
}
