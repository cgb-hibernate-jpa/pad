package com.github.emailtohl.pad.util;

import java.io.IOException;
import java.net.ConnectException;

import okhttp3.ResponseBody;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.emailtohl.pad.util.HttpsUnsafeUtil;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpsUnsafeUtilTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetOkHttpClient() throws IOException {
		OkHttpClient client = HttpsUnsafeUtil.getOkHttpClient();
		Request request = new Request.Builder().url("https://localhost").build();
		try {
			Response response = client.newCall(request).execute();
			if (response.isSuccessful()) {
				ResponseBody resp = response.body();
				System.out.println(resp == null ? "" : resp.string());
			}
		} catch (ConnectException ignored) {}
		
	}

}
