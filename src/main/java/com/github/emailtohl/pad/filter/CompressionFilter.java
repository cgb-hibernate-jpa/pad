package com.github.emailtohl.pad.filter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;

/**
 * 收录的gzip压缩filter
 */
public class CompressionFilter implements Filter {
	private static final Logger log = LogManager.getLogger();
	private static final String gzip = "gzip";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (!(request instanceof HttpServletRequest && response instanceof HttpServletResponse)) {
			chain.doFilter(request, response);
			return;
		}
		String accept = ((HttpServletRequest) request).getHeader(HttpHeaders.ACCEPT_ENCODING);
		if (accept != null && accept.contains(gzip)) {
			log.trace("Encoding requested.");
			((HttpServletResponse) response).setHeader(HttpHeaders.CONTENT_ENCODING, gzip);
			ResponseWrapper wrapper = new ResponseWrapper((HttpServletResponse) response);
			try {
				chain.doFilter(request, wrapper);
			} finally {
				try {
					wrapper.finish();
				} catch (Exception e) {
					log.catching(e);
				}
			}
		} else {
			log.trace("Encoding not requested.");
			chain.doFilter(request, response);
		}
	}

	@Override
	public void init(FilterConfig filterConfig) {
	}

	@Override
	public void destroy() {
	}

	private class ResponseWrapper extends HttpServletResponseWrapper {
		private GZIPServletOutputStream outputStream;
		private PrintWriter writer;

		ResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		@Override
		public synchronized ServletOutputStream getOutputStream() throws IOException {
			if (writer != null)
				throw new IllegalStateException("getWriter() already called.");
			if (outputStream == null)
				outputStream = new GZIPServletOutputStream(super.getOutputStream());
			return outputStream;
		}

		@Override
		public synchronized PrintWriter getWriter() throws IOException {
			if (writer == null && outputStream != null)
				throw new IllegalStateException("getOutputStream() already called.");
			if (writer == null) {
				outputStream = new GZIPServletOutputStream(super.getOutputStream());
				writer = new PrintWriter(new OutputStreamWriter(outputStream, getCharacterEncoding()));
			}
			return writer;
		}

		@Override
		public void flushBuffer() throws IOException {
			if (writer != null)
				writer.flush();
			else if (outputStream != null)
				outputStream.flush();
			super.flushBuffer();
		}

		@Override
		public void setContentLength(int length) {
		}

		@Override
		public void setContentLengthLong(long length) {
		}

		@Override
		public void setHeader(String name, String value) {
			if (!HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name))
				super.setHeader(name, value);
		}

		@Override
		public void addHeader(String name, String value) {
			if (!HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name))
				super.setHeader(name, value);
		}

		@Override
		public void setIntHeader(String name, int value) {
			if (!HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name))
				super.setIntHeader(name, value);
		}

		@Override
		public void addIntHeader(String name, int value) {
			if (!HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name))
				super.setIntHeader(name, value);
		}

		public void finish() throws IOException {
			if (writer != null)
				writer.close();
			else if (outputStream != null)
				outputStream.finish();
		}
	}

	private static class GZIPServletOutputStream extends ServletOutputStream {
		private final ServletOutputStream servletOutputStream;
		private final GZIPOutputStream gzipStream;

		GZIPServletOutputStream(ServletOutputStream servletOutputStream) throws IOException {
			this.servletOutputStream = servletOutputStream;
			gzipStream = new GZIPOutputStream(servletOutputStream);
		}

		@Override
		public boolean isReady() {
			return servletOutputStream.isReady();
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			servletOutputStream.setWriteListener(writeListener);
		}

		@Override
		public void write(int b) throws IOException {
			gzipStream.write(b);
		}

		@Override
		public void close() throws IOException {
			gzipStream.close();
		}

		@Override
		public void flush() throws IOException {
			gzipStream.flush();
		}

		public void finish() throws IOException {
			gzipStream.finish();
		}
	}
}
