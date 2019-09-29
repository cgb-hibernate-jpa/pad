package com.github.emailtohl.lib.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * 包装HttpServletResponse，以便于能从其提取出相应的内容
 * 调用本过滤器前，不能调用HttpServletResponse#getOutputStream()
 * 
 * @author HeLei
 */
public class CacheResponseWrapper extends HttpServletResponseWrapper {
	private OutputStreamWrapper streamWrapper;
	private PrintWriter writer;

	public CacheResponseWrapper(HttpServletResponse response) throws IOException {
		super(response);
		// 创建一个自定义的输出流，当此流的write方法被调用时，会将数据存储一份到内存中
		streamWrapper = new OutputStreamWrapper(response.getOutputStream());
		// 同样创建一个自定义的PrintWriter对象用于getWriter返回，当此对象的print方法被调用时，它最终会调用到输出流的write方法上，数据也会存储一份到内存中
		writer = new PrintWriter(streamWrapper);
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return streamWrapper;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return writer;
	}

	/**
	 * @return 返回内存中的内容
	 * @throws IOException 刷新数据时出现错误
	 */
	public byte[] getContent() throws IOException {
		// writer刷新时会不仅刷新自己的缓存，而且会将数据刷新到streamWrapper流中
		writer.flush();
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
		public void flush() throws IOException {
			outputStream.flush();
		}

		@Override
		public void close() throws IOException {
			outputStream.close();
			memoryStream.close();
		}

	}

}
