package com.github.emailtohl.pad.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.github.emailtohl.pad.exception.InnerDataStateException;

/**
 * 提供文件夹压缩功能
 * @author HeLei
 *
 */
public class ZipUtil {

	/**
	 * 功能:压缩多个文件成一个zip文件
	 *
	 * @param srcFile：源文件或目录
	 * @param zipFile：压缩后的文件
	 */
	public static void zipFiles(File srcFile, File zipFile) {
		List<File> files = FilePathUtil.findFiles(srcFile);
		byte[] buf = new byte[1024];
		try (OutputStream fo = new FileOutputStream(zipFile);
				ZipOutputStream out = new ZipOutputStream(fo)) {
			String base = srcFile.getCanonicalPath();
			for (File file : files) {
				String relative = file.getCanonicalPath();
				relative = relative.replace(base, "");
				if (relative.startsWith("/") || relative.startsWith("\\")) {
					relative = relative.substring(1);
				}
				try (FileInputStream in = new FileInputStream(file)) {
					out.putNextEntry(new ZipEntry(relative));
					int len;
					while ((len = in.read(buf)) > 0) {
						out.write(buf, 0, len);
					}
					out.closeEntry();
				}
			}
		} catch (IOException e) {
			throw new InnerDataStateException(e);
		}
	}

	/**
	 * 功能:解压缩
	 *
	 * @param zipFile：需要解压缩的文件
	 * @param descDir：解压后的目标目录
	 */
	public static void unZipFiles(File zipFile, String descDir) {
		try (ZipFile zf = new ZipFile(zipFile)) {
			for (Enumeration<? extends ZipEntry> entries = zf.entries(); entries.hasMoreElements();) {
				ZipEntry entry = entries.nextElement();
				String zipEntryName = entry.getName();
				File targetFile = FilePathUtil.getOrCreateFile(FilePathUtil.join(descDir, zipEntryName));
				try (InputStream in = zf.getInputStream(entry); OutputStream out = new FileOutputStream(targetFile)) {
					byte[] buf = new byte[1024];
					int len;
					while ((len = in.read(buf)) > 0) {
						out.write(buf, 0, len);
					}
				}
			}
		} catch (Exception e) {
			throw new InnerDataStateException(e);
		}
	}

}
