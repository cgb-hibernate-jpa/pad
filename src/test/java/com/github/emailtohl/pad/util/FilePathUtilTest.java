package com.github.emailtohl.pad.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

public class FilePathUtilTest {

	@Test
	public void testJoin() {
		String dir1 = "/abc\\bcd/";
		String dir2 = "\\edf/ghi\\jk";
		String dir = FilePathUtil.join(dir1, dir2);
		assertNotNull(dir);
	}

	@Test
	public void testGetOrCreateDir() throws IOException {
		String path = FilePathUtil.join(System.getProperty("java.io.tmpdir"), "test_dir");
		File dir = FilePathUtil.getOrCreateDir(path);
		assertTrue(dir.exists());
		assertTrue(dir.delete());
	}

	@Test
	public void testGetOrCreateFile() throws IOException {
		String path = FilePathUtil.join(System.getProperty("java.io.tmpdir"), "test_file");
		File dir = FilePathUtil.getOrCreateFile(path);
		assertTrue(dir.exists());
		assertTrue(dir.delete());
	}
	
	@Test
	void findFiles() {
		String tmp = System.getProperty("java.io.tmpdir");
		List<File> files = FilePathUtil.findFiles(tmp);
		assertTrue(files.size() >= 0);
	}
}
