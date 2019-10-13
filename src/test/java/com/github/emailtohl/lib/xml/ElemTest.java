package com.github.emailtohl.lib.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class ElemTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testElemString() throws ParserConfigurationException, SAXException, IOException {
		String axml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + 
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n" + 
				"	<modelVersion>4.0.0</modelVersion>\r\n" + 
				"	<groupId>com.github.emailtohl</groupId>\r\n" + 
				"	<artifactId>lib</artifactId>\r\n" + 
				"	<properties>\r\n" + 
				"		<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>" + 
				"		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\r\n" + 
				"	</properties>\r\n" + 
				"</project>";
		String bxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
				"<project hello=\"\" world=\"\" xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + 
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n" + 
				"\r\n" + 
				"	<modelVersion>4.0.0</modelVersion>\r\n" + 
				"\r\n" + 
				"	<groupId>com.github.emailtohl</groupId>\r\n" + 
				"\r\n" + 
				"	<artifactId>lib</artifactId>\r\n" + 
				"\r\n" + 
				"	<properties>\r\n" + 
				"\r\n" + 
				"		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\r\n" + 
				"		<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>\r\n" + 
				"\r\n" + 
				"	</properties>\r\n" + 
				"\r\n" + 
				"</project>";
		
		Elem a = new Elem(axml);
		Elem b = new Elem(bxml);
		
		assertEquals(a, b);
		assertEquals(a.attrs, b.attrs);
		assertNotEquals(a.toString(), b.toString());
		
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a.attrs.hashCode(), b.attrs.hashCode());
		
		Set<Elem> set = new HashSet<Elem>();
		set.add(a);
		
		assertTrue(set.contains(b));
	}

}
