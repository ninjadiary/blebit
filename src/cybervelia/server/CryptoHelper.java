package cybervelia.server;


import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;


public class CryptoHelper {

    public static AdvancedEncryptionStandard generateAES(String hmacStr, String password) throws Exception{
        byte hmacByte[] = Base64.getDecoder().decode(hmacStr);

        // re-create hmac
        byte hmacHash[] = CryptoHelper.HMAC(password.getBytes(), hmacByte);

        // decrypt keys using hmac
        byte newHash256[] = new byte[32];
        // get only 16-bytes (constraint of AES)
        System.arraycopy(hmacHash, 0, newHash256, 0, 32);

        AdvancedEncryptionStandard AES = new AdvancedEncryptionStandard(newHash256);
        return AES;
    }

    public static byte[] sign(byte plainText[], PrivateKey privateKey) throws Exception {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKey);
        privateSignature.update(plainText);
        byte[] signature = privateSignature.sign();
        return signature;
    }

    public static boolean verify(byte plainText[], byte signature[], PublicKey publicKey) throws Exception {
        Signature publicSignature = Signature.getInstance("SHA256withRSA");
        publicSignature.initVerify(publicKey);
        publicSignature.update(plainText);
        return publicSignature.verify(signature);
    }

    public static String Base64Encoded(byte data[]){
        return Base64.getEncoder().encodeToString(data).replaceAll("\\+", "%2B").replaceAll("/", "%2F");
        
    }

    public static byte[] encryptAES(byte data[], byte password[], byte randomHMACKey[]) throws Exception{

        // generate hmac hash
        byte hmacHash[] = CryptoHelper.HMAC(password, randomHMACKey);    // create hmac hash using random key generated

        // encrypt the data using hmac hash as key
        byte newHash16[] = new byte[16];

        // get only 16-bytes (constraint of AES)
        System.arraycopy(hmacHash, 0, newHash16, 0, 16);

        // Encrypt data
        AdvancedEncryptionStandard aes = new AdvancedEncryptionStandard(newHash16);
        byte encryptedData[] = aes.encrypt(data);

        return encryptedData;
    }

    public static KeyPair buildKeyPair() throws NoSuchAlgorithmException {
        final int keySize = 2048;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.genKeyPair();
    }

    public static PublicKey getPublicKeyBytes(byte data[]) throws Exception{
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(data));
    }

    public static PrivateKey getPrivateKeyBytes(byte data[]) throws Exception{
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(data));
    }

    public static byte[] randomBytes(int arrayLength) throws NoSuchAlgorithmException {
        SecureRandom ng = new SecureRandom();
        ng.setSeed(ng.generateSeed(32));

        byte[] randomBytes = new byte[arrayLength];
        ng.nextBytes(randomBytes);
        return randomBytes;
    }

    public static String bytesToHex(byte[] input_data) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < input_data.length; i++) {
            String hex = Integer.toHexString(0xff & input_data[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    public static String bytesToHex(byte[] input_data, int size) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < input_data.length && i<size; i++) {
            String hex = Integer.toHexString(0xff & input_data[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static byte[] HMAC(byte data[], byte key[]) throws NoSuchAlgorithmException, InvalidKeyException
    {
        SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        return mac.doFinal(data);
    }

    public static String bytesToSHA256(byte data[]) throws NoSuchAlgorithmException{
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return bytesToHex(digest.digest(data));
    }


    public static byte[] bytesToSHA256bytes(byte data[]) throws NoSuchAlgorithmException{
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }

    public static String bytesToSHA1(byte data[]) throws NoSuchAlgorithmException{
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        return bytesToHex(digest.digest(data));
    }

    public static byte[] RSAencrypt(byte[] data, PublicKey publicKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    public static byte[] RSAdecrypt(byte[] data, PrivateKey privateKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static boolean isValidHex(String str){
        str = str.toLowerCase();
        char[] passHex = str.toCharArray();
        boolean isHex = true;

        if (str.length() % 2 == 0 && str.length() > 2) {
            for (int i = 0; i < passHex.length; i++) {
                if (Character.digit(passHex[i], 16) == -1)
                    isHex = false;
            }
        }else isHex = false;
        if (isHex) {
            return true;
        }

        return false;
    }

    public static byte[] hashGroupPassword(String password) throws Exception {
        int iterationCount = 1000;
        if (!CryptoHelper.isValidHex(password)) {
            int aesKeyStrength = 256;
            byte[] salt = new byte[16];
            System.arraycopy(CryptoHelper.bytesToSHA256bytes(password.getBytes()), 0, salt, 0, 16);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, aesKeyStrength);
            SecretKey tmp = factory.generateSecret(spec);
            return tmp.getEncoded();
        }
        else
        {
            //Log.e("Type of Pass", "Hex password used");
            byte[] rawPass = CryptoHelper.hexStringToByteArray(password);
            for(int i=0; i<iterationCount; ++i)
                rawPass = bytesToSHA256bytes(rawPass);
            byte[] finalPass = new byte[32];
            System.arraycopy(rawPass, 0, finalPass, 0, 32);
            return finalPass;
        }
    }


}
