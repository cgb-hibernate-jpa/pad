package com.github.emailtohl.lib.encryption;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 用于文件加密解密的工具，加解密文件用的是AES算法，AES的密钥用RSA算法加密
 * @author HeLei
 */
public class Crypter {
	private static final Logger logger = LogManager.getLogger();
	private Hex hex = new Hex();
	
	/**
	 * 创建RSA的密钥对
	 * @param length 密钥长度，bit位
	 * @return 公钥和私钥
	 */
	public KeyPair createKeyPairs(int length) {
		KeyPairGenerator pairgen = null;
		try {
			pairgen = KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			logger.fatal("RSA密钥创建失败", e);
			throw new IllegalStateException(e);
		}
		SecureRandom random = new SecureRandom();
		assert pairgen != null;
		pairgen.initialize(length, random);
		return pairgen.generateKeyPair();
	}
	
	/**
	 * 创建RSA的密钥对
	 * @param length 密钥长度，bit位
	 * @param publicKeyFile 存储为文件的公钥
	 * @param privateKeyFile 存储为文件的私钥
	 */
	public void createKeyPairs(int length, File publicKeyFile, File privateKeyFile) {
		KeyPairGenerator pairgen = null;
		try {
			pairgen = KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			logger.fatal("RSA密钥创建失败", e);
			throw new IllegalStateException(e);
		}
		assert pairgen != null;
		pairgen.initialize(length, new SecureRandom());
		KeyPair keyPair = pairgen.generateKeyPair();
		try (ObjectOutputStream outPublicKey = new ObjectOutputStream(new FileOutputStream(publicKeyFile));
				ObjectOutputStream outPrivateKey = new ObjectOutputStream(new FileOutputStream(privateKeyFile))) {
			outPublicKey.writeObject(keyPair.getPublic());
			outPrivateKey.writeObject(keyPair.getPrivate());
			logger.debug("密钥创建成功");
		} catch (IOException e) {
			logger.fatal("密钥创建失败", e);
		}
	}
	
	/**
	 * 加密文件
	 * @param inFile 要加密的文件
	 * @param outFile 加密后的文件
	 * @param publicKeyFile 存储为文件的公钥
	 */
	public void encrypt(File inFile, File outFile, File publicKeyFile) {
		// wrap with RSA public key
		try (ObjectInputStream keyIn = new ObjectInputStream(new FileInputStream(publicKeyFile));
				DataOutputStream out = new DataOutputStream(new FileOutputStream(outFile));
				InputStream in = new FileInputStream(inFile)) {

			KeyGenerator keygen = KeyGenerator.getInstance("AES");
			SecureRandom random = new SecureRandom();
			keygen.init(random);
			SecretKey key = keygen.generateKey();

			Key publicKey = (Key) keyIn.readObject();
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.WRAP_MODE, publicKey);
			byte[] wrappedKey = cipher.wrap(key);
			out.writeInt(wrappedKey.length);
			out.write(wrappedKey);

			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			crypt(in, out, cipher);
		} catch (IOException | ClassNotFoundException | GeneralSecurityException e) {
			logger.fatal("加密失败", e);
		}
	}
	
	/**
	 * 解密文件
	 * @param inFile 输入的密文文件
	 * @param outFile 输出的明文文件
	 * @param privateKeyFile 存储为文件的私钥
	 */
	public void decrypt(File inFile, File outFile, File privateKeyFile) {
		try (DataInputStream in = new DataInputStream(new FileInputStream(inFile));
				ObjectInputStream keyIn = new ObjectInputStream(new FileInputStream(privateKeyFile));
				OutputStream out = new FileOutputStream(outFile)) {
			int length = in.readInt();
			byte[] wrappedKey = new byte[length];
			in.read(wrappedKey, 0, length);

			// unwrap with RSA private key
			Key privateKey = (Key) keyIn.readObject();

			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.UNWRAP_MODE, privateKey);
			Key key = cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);

			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, key);

			crypt(in, out, cipher);
		} catch (IOException | ClassNotFoundException | GeneralSecurityException e) {
			logger.fatal("解密失败", e);
		}
	}

	/**
	 * Uses a cipher to transform the bytes in an input stream and sends the
	 * transformed bytes to an output stream.
	 * @param in the input stream
	 * @param out the output stream
	 * @param cipher the cipher that transforms the bytes
	 * @throws IOException Signals that an I/O exception of some sort has occurred. This class is the general class of exceptions produced by failed or interrupted I/O operations.
	 * @throws GeneralSecurityException The GeneralSecurityException class is a generic security exception class that provides type safety for all the security-related exception classes that extend from it.
	 */
	public void crypt(InputStream in, OutputStream out, Cipher cipher) throws IOException,
			GeneralSecurityException {
		int blockSize = cipher.getBlockSize();
		int outputSize = cipher.getOutputSize(blockSize);
		byte[] inBytes = new byte[blockSize];
		byte[] outBytes = new byte[outputSize];

		int inLength = 0;
		boolean more = true;
		while (more) {
			inLength = in.read(inBytes);
			if (inLength == blockSize) {
				int outLength = cipher.update(inBytes, 0, blockSize, outBytes);
				out.write(outBytes, 0, outLength);
			} else
				more = false;
		}
		if (inLength > 0)
			outBytes = cipher.doFinal(inBytes, 0, inLength);
		else
			outBytes = cipher.doFinal();
		out.write(outBytes);
	}

	/**
	 * 加密字符串
	 * @param plaintext 明文文本
	 * @param publicKey 公钥
	 * @return 加密后的文本
	 */
	public String encrypt(String plaintext, PublicKey publicKey) {
		try (ByteArrayInputStream in = new ByteArrayInputStream(plaintext.getBytes());
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				DataOutputStream out = new DataOutputStream(bout)) {
			KeyGenerator keygen = KeyGenerator.getInstance("AES");
			SecureRandom random = new SecureRandom();
			keygen.init(random);
			SecretKey key = keygen.generateKey();

			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.WRAP_MODE, publicKey);
			byte[] wrappedKey = cipher.wrap(key);
			out.writeInt(wrappedKey.length);
			out.write(wrappedKey);

			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			crypt(in, out, cipher);

			return hex.encodeHexStr(bout.toByteArray());
		} catch (IOException | GeneralSecurityException e) {
			logger.fatal("加密失败", e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 解密字符串
	 * @param ciphertext 密文文本
	 * @param privateKey 私钥
	 * @return 解密后的明文
	 */
	public String decrypt(String ciphertext, PrivateKey privateKey) {
		byte[] ciphertextByteArray = hex.decodeHex(ciphertext.toCharArray());
		try (ByteArrayInputStream bin = new ByteArrayInputStream(ciphertextByteArray);
				DataInputStream in = new DataInputStream(bin);
				ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			int length = in.readInt();
			byte[] wrappedKey = new byte[length];
			in.read(wrappedKey, 0, length);

			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.UNWRAP_MODE, privateKey);
			Key key = cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);

			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, key);

			crypt(in, out, cipher);
			return new String(out.toByteArray());
		} catch (IOException | GeneralSecurityException e) {
			logger.fatal("解密失败", e);
			throw new RuntimeException(e);
		}
	}
	
}
