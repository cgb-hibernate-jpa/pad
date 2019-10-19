package com.github.emailtohl.lib.filter;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class CacheResponseWrapperTest {

	@Test
	public void test() throws IOException, ServletException {
		Filter filter = new Filter() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {
				CacheResponseWrapper resp = new CacheResponseWrapper((HttpServletResponse) response);
				chain.doFilter(request, resp);
				String charset = resp.getCharacterEncoding();
				String respStr = new String(resp.getContent(), charset);
				assertEquals("hello bar", respStr);
			}
		};
		MockFilterChain chain = new MockFilterChain(new ServletForTest());
		MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/hello");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.addParameter("name", "bar");
		filter.doFilter(request, response, chain);

		assertEquals("hello bar", response.getContentAsString());
	}

}
