package com.github.emailtohl.pad.util;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;

import com.github.emailtohl.pad.util.PackageScanner;

public class PackageScannerTest {

	@Test
	public void testGetClasses() {
		Set<Class<?>> set = PackageScanner.getClasses("com.github.emailtohl.pad");
		assertFalse(set.isEmpty());
		assertTrue(set.contains(PackageScanner.class));
	}
	
	@Test
	public void testScanJar() {
		Set<Class<?>> set = PackageScanner.getClasses("org.junit");
		assertFalse(set.isEmpty());
		assertTrue(set.contains(Test.class));
	}

}
