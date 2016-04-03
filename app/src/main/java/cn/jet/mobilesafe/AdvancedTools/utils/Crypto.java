package cn.jet.mobilesafe.AdvancedTools.utils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;
import android.util.Log;

public class Crypto {
	static byte [] mRawKey = null;
	/**
	 * 加密一个文本，返回base64编码后的内容。
	 * 
	 * @param seed  种子 密码
	 * @param plain  原文
	 * @return 密文
	 * @throws Exception
	 */
	public static String encrypt(String seed, String plain) throws Exception {
		//byte[] rawKey = getRawKey(seed.getBytes());
		byte[] rawKey = generateKey(seed);
		//Log.i("Crypto", "encrypt rawKey: " + Base64.encodeToString(rawKey, Base64.DEFAULT));
		byte[] encrypted = encrypt(rawKey, plain.getBytes());
		return Base64.encodeToString(encrypted, Base64.DEFAULT);
	}

	/**
	 * 解密base64编码后的密文
	 * 
	 * @param seed  种子 密码
	 * @param encrypted  密文
	 * @return 原文
	 * @throws Exception
	 */
	public static String decrypt(String seed, String encrypted)
			throws Exception {
		//byte[] rawKey = getRawKey(seed.getBytes());
		byte[] rawKey = generateKey(seed);
		//Log.i("Crypto", "decrypt rawKey: " + Base64.encodeToString(rawKey, Base64.DEFAULT));
		byte[] enc = Base64.decode(encrypted.getBytes(), Base64.DEFAULT);
		byte[] result = decrypt(rawKey, enc);
		return new String(result);
	}


	//在linux环境编译getRawKey  每次返回值不同 //javax.crypto.BadPaddingException: pad block corrupted
	private static byte[] getRawKey(byte[] seed) throws Exception {
		KeyGenerator keygen = KeyGenerator.getInstance("DES");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
		random.setSeed(seed);
		//keygen.init(128, random); //AES加密 192 and 256 bits may not be available
		keygen.init(random);
		SecretKey key = keygen.generateKey();
		byte[] raw = key.getEncoded();
		return raw;
	}

	private static byte[] encrypt(byte[] rawKey, byte[] plain) throws Exception {
		SecretKeySpec keySpec = new SecretKeySpec(rawKey, "DES");
		Cipher cipher = Cipher.getInstance("DES");
		cipher.init(Cipher.ENCRYPT_MODE, keySpec);
		byte[] encrypted = cipher.doFinal(plain);
		return encrypted;
	}

	private static byte[] decrypt(byte[] rawKey, byte[] encrypted)
			throws Exception {
		SecretKeySpec keySpec = new SecretKeySpec(rawKey, "DES");
		Cipher cipher = Cipher.getInstance("DES");
		cipher.init(Cipher.DECRYPT_MODE, keySpec);
		//javax.crypto.BadPaddingException: pad block corrupted
		byte[] decrypted = cipher.doFinal(encrypted);
		return decrypted;
	}

	private static byte[] generateKey(String secretKey)
			throws NoSuchAlgorithmException,InvalidKeyException,InvalidKeySpecException{
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		DESKeySpec keySpec = new DESKeySpec(secretKey.getBytes());
		keyFactory.generateSecret(keySpec);
		SecretKey factorySecretKey =  keyFactory.generateSecret(keySpec);
		byte[] raw = factorySecretKey.getEncoded();
		return raw;
	}
}