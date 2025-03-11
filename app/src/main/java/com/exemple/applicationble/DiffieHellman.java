package com.exemple.applicationble;

import android.util.Base64;
import android.util.Log;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
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
            this.peerPublicKey = keyFactory.generatePublic(x509Spec);
            // Réinitialiser le secret partagé pour forcer son recalcul
            sharedKey = null;
        }

        // Ou directement depuis un objet PublicKey
        public void setPeerPublicKey(PublicKey peerPublicKey) throws Exception {
            this.peerPublicKey = peerPublicKey;
            // Réinitialiser le secret partagé pour forcer son recalcul
            sharedKey = null;
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

    public static String encodePublicKey(PublicKey publicKey) {
        return Base64.encodeToString(publicKey.getEncoded(), Base64.NO_WRAP);
    }

    public static PublicKey decodePublicKey(String base64PublicKey) {
        try {
            if (base64PublicKey.contains("BEGIN")) {
                base64PublicKey = base64PublicKey
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s+", "");
            }
            byte[] decodedKey = Base64.decode(base64PublicKey, Base64.NO_WRAP);
            Log.d("DECODE_KEY", "Taille de la clé décodée : " + decodedKey.length);

            if (decodedKey[0] != 0x30) {
                Log.e("DECODE_KEY", "Erreur : La clé ne commence pas par 0x30, format incorrect !");
                return null;
            }
            KeyFactory keyFactory = KeyFactory.getInstance("DH");
            try {
                return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
            } catch (InvalidKeySpecException e) {
                Log.w("DECODE_KEY", "X.509 échoué, tentative avec PKCS8...");
                return keyFactory.generatePublic(new PKCS8EncodedKeySpec(decodedKey));
            }
        } catch (Exception e) {
            Log.e("DECODE_KEY", "Erreur lors du décodage de la clé publique", e);
            return null;
        }
    }
}
