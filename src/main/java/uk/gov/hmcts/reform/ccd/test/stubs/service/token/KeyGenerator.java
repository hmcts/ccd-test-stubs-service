package uk.gov.hmcts.reform.ccd.test.stubs.service.token;

import java.security.Key;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import static io.jsonwebtoken.SignatureAlgorithm.RS256;

public final class KeyGenerator {

    private KeyGenerator() {
    }

    public static Key secretKey(String key) {
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(key);
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, RS256.getJcaName());
        return signingKey;
    }
}
