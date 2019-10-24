package com.github.emailtohl.pad.util;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;

import com.github.emailtohl.pad.ConstantPattern;

/**
 * 替换成本地操作系统识别的路径
 * 
 * @author HeLei
 */
public class FilePathUtil {
	/**
	 * 将目录路径拼接起来，返回本操作系统识别的目录路径
	 * 
	 * @param paths 路径数组
	 * @return 本操作系统识别的路径
	 */
	public static String join(String... paths) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String s : paths) {
			s = s.replaceAll(ConstantPattern.SEPARATOR, Matcher.quoteReplacement(File.separator));
			if (s.endsWith(File.separator)) {
				s = s.substring(0, s.length() - 1);
			}
			if (first) {
				sb.append(s);
				first = false;
			} else {
				if (s.startsWith(File.separator)) {
					sb.append(s);
				} else {
					sb.append(File.separator).append(s);
				}
			}
		}
		return sb.toString();
	}

	/**
	 * 返回指定路径，若不存在则创建它
	 * 
	 * @param path 指定路径
	 * @return 可用的路径
	 * @throws IOException 创建路径失败
	 */
	public static File getOrCreateDir(String path) throws IOException {
		// 过滤路径分隔符
		path = join(path);
		File dir = new File(path);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				throw new IOException("create path " + dir.getAbsolutePath() + " failed");
			}
		}
		return dir;
	}

	/**
	 * 返回指定的文件，若不存在则创建它
	 * 
	 * @param path 指定的文件
	 * @return 可用的文件
	 * @throws IOException 创建文件失败
	 */
	public static File getOrCreateFile(String path) throws IOException {
		// 过滤路径分隔符
		path = join(path);
		File file = new File(path);
		File dir = file.getParentFile();
		boolean ok = true;
		if (!dir.exists()) {
			ok = dir.mkdirs();
		}
		if (!ok) {
			throw new IOException("create path " + dir.getAbsolutePath() + " failed");
		}
		if (!file.exists()) {
			ok = file.createNewFile();
		}
		if (!ok) {
			throw new IOException("create file " + file.getAbsolutePath() + " failed");
		}
		return file;
	}
}
