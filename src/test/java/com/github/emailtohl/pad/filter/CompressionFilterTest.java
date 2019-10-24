package com.github.emailtohl.pad.filter;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletException;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.github.emailtohl.pad.filter.CompressionFilter;

public class CompressionFilterTest {

	@Test
	public void testDoFilter() throws IOException, ServletException {
		CompressionFilter filter = new CompressionFilter();
		MockFilterChain chain = new MockFilterChain(new ServletForTest());
		MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/hello");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
		request.addParameter("name", "foo");
		filter.doFilter(request, response, chain);
		assertEquals("gzip", response.getHeader(HttpHeaders.CONTENT_ENCODING));

		ByteArrayInputStream in = new ByteArrayInputStream(response.getContentAsByteArray());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPInputStream gin = new GZIPInputStream(in);
		byte[] buffer = new byte[256];
		int n;
		while ((n = gin.read(buffer)) >= 0) {
			out.write(buffer, 0, n);
		}
		assertEquals("hello foo", new String(out.toByteArray()));
	}

}
