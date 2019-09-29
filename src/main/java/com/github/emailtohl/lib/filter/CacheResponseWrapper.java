package com.github.emailtohl.lib.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * 包装HttpServletResponse，以便于能从其提取出相应的内容
 * 
 * @author HeLei
 */
public class CacheResponseWrapper extends HttpServletResponseWrapper {
	private OutputStreamWrapper streamWrapper;
	private PrintWriter writer;

	public CacheResponseWrapper(HttpServletResponse response) throws IOException {
		super(response);
	}

	@Override
	public synchronized ServletOutputStream getOutputStream() throws IOException {
		if (writer != null)
			throw new IllegalStateException("getWriter() already called.");
		if (streamWrapper == null)
			// 创建一个自定义的输出流，当此流的write方法被调用时，会将数据存储一份到内存中
			streamWrapper = new OutputStreamWrapper(super.getOutputStream());
		return streamWrapper;
	}

	@Override
	public synchronized PrintWriter getWriter() throws IOException {
		if (writer == null && streamWrapper != null)
			throw new IllegalStateException("getOutputStream() already called.");
		if (writer == null) {
			// 创建一个自定义的输出流，当此流的write方法被调用时，会将数据存储一份到内存中
			streamWrapper = new OutputStreamWrapper(super.getOutputStream());
			// 同样创建一个自定义的PrintWriter对象用于getWriter返回，当此对象的print方法被调用时，它最终会调用到输出流的write方法上，数据也会存储一份到内存中
			writer = new PrintWriter(new WriterWrapper(streamWrapper));
		}
		return writer;
	}

	/**
	 * @return 返回内存中的内容
	 */
	public byte[] getContent() {
		return streamWrapper.getByteArrayOutputStream().toByteArray();
	}

	private class OutputStreamWrapper extends ServletOutputStream {
		private ServletOutputStream outputStream;
		private ByteArrayOutputStream memoryStream = new ByteArrayOutputStream();

		OutputStreamWrapper(ServletOutputStream servletOutputStream) throws IOException {
			this.outputStream = servletOutputStream;
		}

		ByteArrayOutputStream getByteArrayOutputStream() {
			return memoryStream;
		}

		@Override
		public boolean isReady() {
			return outputStream.isReady();
		}

		@Override
		public void setWriteListener(WriteListener listener) {
			outputStream.setWriteListener(listener);
		}

		@Override
		public void write(int b) throws IOException {
			outputStream.write(b);
			memoryStream.write(b);
		}

		@Override
		public void close() throws IOException {
			outputStream.close();
			memoryStream.close();
		}

	}

	private class WriterWrapper extends Writer {
		private ServletOutputStream stream;

		WriterWrapper(ServletOutputStream stream) {
			this.stream = stream;
		}

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			for (int i = off; i < off + len; i++) {
				stream.write((int) cbuf[i]);
			}
		}

		@Override
		public void flush() throws IOException {
			stream.flush();
		}

		@Override
		public void close() throws IOException {
			stream.close();
		}

	}
}
