package com.github.emailtohl.lib.util;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;

public class PackageScannerTest {

	@Test
	public void testGetClasses() {
		Set<Class<?>> set = PackageScanner.getClasses("com.github.emailtohl.lib");
		assertFalse(set.isEmpty());
		assertTrue(set.contains(PackageScanner.class));
	}

}
