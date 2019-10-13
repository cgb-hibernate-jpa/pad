package com.github.emailtohl.lib.util;

import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.emailtohl.lib.exception.InnerDataStateException;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

/**
 * 依赖okhttp，能访问https，但所有校验均放过，不安全
 * 
 * @author HeLei
 */
public class HttpsUnsafeUtil {
	private static final Logger logger = LogManager.getLogger();
	private static final ConcurrentHashMap<String, List<Cookie>> COOKIE_STORE = new ConcurrentHashMap<>();

	private HttpsUnsafeUtil() {
	}

	/**
	 * 获取一个可以使用https的OkHttpClient
	 * @return 返回一个OkHttpClient
	 */
	public static OkHttpClient getOkHttpClient() {
		return new OkHttpClient.Builder().hostnameVerifier(getHostnameVerifier())
				.sslSocketFactory(getDefaultSSLSocketFactory(), getX509TrustManager()).cookieJar(new CookieJar() {// 这里可以做cookie传递，保存等操作
					@Override
					public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {// 可以做保存cookies操作
						COOKIE_STORE.put(url.host(), cookies);
					}

					@Override
					public List<Cookie> loadForRequest(HttpUrl url) {// 加载新的cookies
						List<Cookie> cookies = COOKIE_STORE.get(url.host());
						return cookies != null ? cookies : new ArrayList<Cookie>();
					}
				}).build();
	}

	/**
	 * 获取SSLSocketFactory
	 * @param certificates 证书的输入流
	 * @return 返回一个SSLSocketFactory
	 * @throws InnerDataStateException 任何异常将转成InnerDataStateException抛出
	 */
	public static SSLSocketFactory getSSLSocketFactory(InputStream... certificates) {
		try {
			// 用我们的证书创建一个keystore
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(null);
			int index = 0;
			for (InputStream certificate : certificates) {
				String certificateAlias = "server" + Integer.toString(index++);
				keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate));
				if (certificate != null) {
					certificate.close();
				}
			}
			// 创建一个trustmanager，只信任我们创建的keystore
			SSLContext sslContext = SSLContext.getInstance("TLS");
			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keyStore);
			sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
			return sslContext.getSocketFactory();
		} catch (Exception e) {
			logger.catching(e);
			throw new InnerDataStateException(e);
		}
	}

	/**
	 * 获取X509TrustManager证书管理器，此不对客户端和服务端做校验
	 * @return 返回一个X509TrustManager
	 */
	public static X509TrustManager getX509TrustManager() {
		return new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		};
	}

	/**
	 * 获取SSLSocketFactory
	 * @return 返回一个默认的SSLSocketFactory
	 * @throws InnerDataStateException 任何异常将转成InnerDataStateException抛出
	 */
	public static SSLSocketFactory getDefaultSSLSocketFactory() {
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { getX509TrustManager() }, null);
			return sslContext.getSocketFactory();
		} catch (GeneralSecurityException e) {
			logger.catching(e);
			throw new InnerDataStateException(e);
		}
	}

	/**
	 * 获取HostnameVerifier，但不对hostname和SSLSession做校验
	 * @return 返回HostnameVerifier
	 */
	public static HostnameVerifier getHostnameVerifier() {
		return new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
	}

}
