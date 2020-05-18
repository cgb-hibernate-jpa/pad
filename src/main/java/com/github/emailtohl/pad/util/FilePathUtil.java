package com.github.emailtohl.pad.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
	
	/**
	 * 查找目录中的所有文件
	 *
	 * @param dir 文件系统的文件夹
	 * @return
	 */
	public static List<File> findFiles(String dir) {
		File d = new File(dir);
		return findFiles(d);
	}

	/**
	 * 查找目录中的所有文件
	 *
	 * @param f 文件系统的文件或文件夹
	 * @return
	 */
	public static List<File> findFiles(File f) {
		List<File> files = new ArrayList<>();
		File[] arr = null;
		if (f.isFile()) {
			arr = new File[] { f };
		} else if (f.isDirectory()) {
			arr = f.listFiles();
		}
		BreadthFirst bf = new BreadthFirst(arr);
		bf.appendTo(files);
		return files;
	}

	private static class BreadthFirst {

		LinkedList<File> queue = new LinkedList<>();

		BreadthFirst(File[] files) {
			if (files == null) {
				return;
			}
			for (File f : files) {
				queue.add(f);
			}
		}

		void appendTo(List<File> files) {
			while (queue.size() > 0) {
				LinkedList<File> items = new LinkedList<>(queue);
				queue.clear();
				for (File f : items) {
					if (f.isDirectory()) {
						File[] arr = f.listFiles();
						if (arr != null) {
							for (File sf : arr) {
								queue.add(sf);
							}
						}
					} else if (f.isFile()) {
						files.add(f);
					}
				}
			}
		}
	}
}
