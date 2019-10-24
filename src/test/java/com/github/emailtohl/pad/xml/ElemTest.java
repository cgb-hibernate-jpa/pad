package com.github.emailtohl.pad.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.github.emailtohl.pad.xml.Elem;

public class ElemTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testElemString() throws ParserConfigurationException, SAXException, IOException {
		String axml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + 
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + 
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n" + 
				"	<modelVersion>4.0.0</modelVersion>\r\n" + 
				"\r\n" + 
				"	<groupId>com.github.emailtohl</groupId>\r\n" + 
				"	<artifactId>pad</artifactId>\r\n" + 
				"	<version>2.1.1-RELEASE</version>\r\n" + 
				"	<packaging>jar</packaging>\r\n" + 
				"\r\n" + 
				"	<name>pad</name>\r\n" + 
				"	<description>\r\n" + 
				"		The basic development kit, which encapsulates common features in projects such as JPA, file search, encryption and decryption, makes development easier.\r\n" + 
				"		<span>version</span>\r\n" + 
				"		2.0.0-RELEASE:jpa\r\n" + 
				"		2.0.5-RELEASE:util\r\n" + 
				"		2.1.0-RELEASE:filter,xml\r\n" + 
				"		<em>current</em>\r\n" + 
				"		2.1.1-RELEASE:code improve\r\n" + 
				"	</description>\r\n" + 
				"	\r\n" + 
				"</project>";
		String bxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + 
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + 
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n" + 
				"	<modelVersion>4.0.0</modelVersion>\r\n" + 
				"	<groupId>com.github.emailtohl</groupId><artifactId>pad</artifactId><version>2.1.1-RELEASE</version><packaging>jar</packaging>\r\n" + 
				"	<name>pad</name>\r\n" + 
				"	<description>\r\n" + 
				"		The basic development kit, which encapsulates common features in projects such as JPA, file search, encryption and decryption, makes development easier.\r\n" + 
				"		<span>version</span>\r\n" + 
				"		2.0.0-RELEASE:jpa\r\n" + 
				"		2.0.5-RELEASE:util\r\n" + 
				"		2.1.0-RELEASE:filter,xml\r\n" + 
				"		<em>current</em>\r\n" + 
				"		2.1.1-RELEASE:code improve\r\n" + 
				"	</description>\r\n" + 
				"</project>";
		
		Elem a = new Elem(axml);
		Elem b = new Elem(bxml);
		
		assertEquals(a, b);
		assertEquals(a.attrs, b.attrs);
		String astr = a.toString(), bstr = b.toString();
		assertEquals(astr, bstr);
		
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a.attrs.hashCode(), b.attrs.hashCode());
		
		Set<Elem> set = new HashSet<Elem>();
		set.add(a);
		
		assertTrue(set.contains(b));
	}

}
