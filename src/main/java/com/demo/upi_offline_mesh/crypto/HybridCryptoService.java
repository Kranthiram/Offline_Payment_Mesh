package com.demo.upi_offline_mesh.crypto;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class HybridCryptoService {

    private static final int AES_KEY_SIZE = 256;       // bits
    private static final int GCM_IV_LENGTH = 12;        // bytes — standard for GCM
    private static final int GCM_TAG_LENGTH = 128;       // bits — auth tag size
    private static final int RSA_KEY_BLOCK_SIZE = 256;   // bytes — RSA-2048 encrypted output size

    private final ServerKeyHolder keyHolder;
    private final SecureRandom secureRandom = new SecureRandom();

    public HybridCryptoService(ServerKeyHolder keyHolder) {
        this.keyHolder = keyHolder;
    }


    public String encrypt(String plaintext) throws Exception {
        // Step 1: generate a fresh AES key for THIS packet only
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE);
        SecretKey aesKey = keyGen.generateKey();

        // Step 2: generate a random IV (must be unique per encryption)
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        // Step 3: encrypt the plaintext with AES-GCM
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        byte[] aesCiphertext = aesCipher.doFinal(plaintext.getBytes("UTF-8"));

        // Step 4: encrypt the AES key itself with RSA public key
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, keyHolder.getPublicKey());
        byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

        // Step 5: concatenate [encryptedAesKey][iv][aesCiphertext] and base64-encode
        ByteBuffer buffer = ByteBuffer.allocate(encryptedAesKey.length + iv.length + aesCiphertext.length);
        buffer.put(encryptedAesKey);
        buffer.put(iv);
        buffer.put(aesCiphertext);

        return Base64.getEncoder().encodeToString(buffer.array());
    }


    public String decrypt(String base64Ciphertext) throws Exception {
        byte[] fullPacket = Base64.getDecoder().decode(base64Ciphertext);

        // Step 1: split the packet back into its three parts
        byte[] encryptedAesKey = new byte[RSA_KEY_BLOCK_SIZE];
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] aesCiphertext = new byte[fullPacket.length - RSA_KEY_BLOCK_SIZE - GCM_IV_LENGTH];

        ByteBuffer buffer = ByteBuffer.wrap(fullPacket);
        buffer.get(encryptedAesKey);
        buffer.get(iv);
        buffer.get(aesCiphertext);

        // Step 2: decrypt the AES key using RSA private key
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaCipher.init(Cipher.DECRYPT_MODE, keyHolder.getPrivateKey());
        byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        // Step 3: decrypt the payload using AES-GCM (also verifies the auth tag)
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
        byte[] plaintextBytes = aesCipher.doFinal(aesCiphertext);  // throws if tampered

        return new String(plaintextBytes, "UTF-8");
    }

    /**
     * SHA-256 hash of the ciphertext — used for idempotency dedupe.
     */
    public String hashCiphertext(String base64Ciphertext) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(base64Ciphertext.getBytes("UTF-8"));

        StringBuilder hex = new StringBuilder();
        for (byte b : hashBytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}