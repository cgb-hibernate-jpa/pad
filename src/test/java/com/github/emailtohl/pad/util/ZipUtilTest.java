package com.github.emailtohl.pad.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class ZipUtilTest {

	@Test
	public void testZipUnZipFiles() throws IOException {
		String tmp = System.getProperty("java.io.tmpdir");
	    File target = new File(tmp, "r.zip");
	    
	    ClassPathResource r = new ClassPathResource("");
	    
	    ZipUtil.zipFiles(r.getFile(), target);

	    String dir = FilePathUtil.join(tmp, "r");
	    ZipUtil.unZipFiles(target, dir);
	    
	    FileUtils.deleteDirectory(new File(dir));
	    FileUtils.deleteQuietly(target);
	}

}
