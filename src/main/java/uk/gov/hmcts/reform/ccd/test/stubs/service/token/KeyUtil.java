package uk.gov.hmcts.reform.ccd.test.stubs.service.token;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyUtil {

    public static PublicKey getPublicKey(String filename) {
        try {
            final byte[] keyBytes = Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(filename).toURI()));
            final X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return factory().generatePublic(spec);
        } catch (IOException ex) {
            throw new RuntimeException(
                "Error loading public key '" + filename + "'.", ex
            );
        } catch (final InvalidKeySpecException ex) {
            throw new RuntimeException(
                "Error reading public key '" + filename + "'.", ex
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(
                "Error reading public key '" + filename + "'.", e
            );
        }
    }

    public static PrivateKey getPrivateKey(String filename) {
        try {
            final byte[] keyBytes = Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(filename).toURI()));
            final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return factory().generatePrivate(spec);
        } catch (IOException ex) {
            throw new RuntimeException(
                "Error loading private key '" + filename + "'.", ex
            );
        } catch (final InvalidKeySpecException ex) {
            throw new RuntimeException(
                "Error reading private key '" + filename + "'.", ex
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(
                "Error reading private key '" + filename + "'.", e
            );
        }
    }

    private static KeyFactory factory() {
        try {
            return KeyFactory.getInstance("RSA");
        } catch (final NoSuchAlgorithmException ex) {
            throw new RuntimeException(
                "Could not find the RSA algorithm.", ex
            );
        }
    }

    private KeyUtil() {}
}
