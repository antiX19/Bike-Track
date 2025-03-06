package com.exemple.applicationble;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DiffieHellman {
    public static class DiffieHellmanManager {
        private KeyPair keyPair;
        private KeyAgreement keyAgree;
        private PublicKey peerPublicKey;
        private SecretKey sharedKey; // stocke le secret partagé une fois calculé

        // Génère la paire de clés DH une seule fois par instance
        public void generateKeyPair() throws Exception {
            if (keyPair != null) {
                return; // La paire a déjà été générée
            }
            BigInteger p = new BigInteger(
                    "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E08" +
                            "8A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD" +
                            "3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E" +
                            "7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899F" +
                            "A5AE9F24117C4B1FE649286651ECE65381FFFFFFFFFFFFFFFF", 16);
            BigInteger g = BigInteger.valueOf(2);
            DHParameterSpec dhSpec = new DHParameterSpec(p, g);

            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
            keyPairGen.initialize(dhSpec);
            keyPair = keyPairGen.generateKeyPair();

            keyAgree = KeyAgreement.getInstance("DH");
            keyAgree.init(keyPair.getPrivate());
        }

        public PublicKey getPublicKey() {
            return keyPair.getPublic();
        }

        // Affecte la clé publique du partenaire à partir d'un encodage X509
        public void setPeerPublicKey(byte[] encodedPeerPublicKey) throws Exception {
            KeyFactory keyFactory = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(encodedPeerPublicKey);
            peerPublicKey = keyFactory.generatePublic(x509Spec);
        }

        // Ou directement depuis un objet PublicKey
        public void setPeerPublicKey(PublicKey peerPublicKey) throws Exception {
            this.peerPublicKey = peerPublicKey;
        }

        // Calcule le secret partagé une seule fois par instance et le met en cache
        public SecretKey generateSharedSecret() throws Exception {
            if (sharedKey != null) {
                return sharedKey;
            }
            keyAgree.doPhase(peerPublicKey, true);
            byte[] sharedSecret = keyAgree.generateSecret();
            sharedKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");
            return sharedKey;
        }
    }

    public static class EncryptionManager {

        public static byte[] encrypt(String plaintext, SecretKey key) throws Exception {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(plaintext.getBytes("UTF-8"));
        }

        public static String decrypt(byte[] ciphertext, SecretKey key) throws Exception {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, "UTF-8");
        }
    }

    public static byte[] encryptedfunction(String message, SecretKey sharedKey) throws Exception {
        return EncryptionManager.encrypt(message, sharedKey);
    }

    public static String decryptedfunction(byte[] encryptedMessage, SecretKey sharedKey) throws Exception {
        return EncryptionManager.decrypt(encryptedMessage, sharedKey);
    }
}
