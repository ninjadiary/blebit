package cybervelia.server;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AdvancedEncryptionStandard
{
    private byte[] key;
    private final int IV_SIZE = 12;
    private final int AUTHENTICATION_TAG_LENGTH = 128;
    private static final String ALGORITHM = "AES";

    public AdvancedEncryptionStandard(byte[] key)
    {
        this.key = key;
    }

    /**
     * Encrypts the given plain text
     *
     * @param plainText The plain text to encrypt
     */
    public byte[] encrypt(byte[] plainText) throws Exception
    {

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.setSeed(secureRandom.generateSeed(32));

        byte[] iv = new byte[12]; //NEVER REUSE THIS IV WITH SAME KEY
        secureRandom.nextBytes(iv);
        SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(AUTHENTICATION_TAG_LENGTH, iv); //128 bit auth tag length
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        byte []cipherText = cipher.doFinal(plainText);
        byte []finalCipherText = new byte[cipherText.length+IV_SIZE];
        System.arraycopy(iv,0,finalCipherText,0, IV_SIZE);
        System.arraycopy(cipherText,0,finalCipherText,IV_SIZE, cipherText.length);
        return finalCipherText;
    }

    /**
     * Decrypts the given byte array
     *
     * @param cipherMessage The data to decrypt
     */
    public byte[] decrypt(byte[] cipherMessage) throws Exception
    {
        ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
        if (cipherMessage.length < IV_SIZE)
            throw new Exception("cipher-text size should be bigger");

        byte[] iv = new byte[IV_SIZE];
        byteBuffer.get(iv);
        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(AUTHENTICATION_TAG_LENGTH, iv));

        return cipher.doFinal(cipherText);
    }
}