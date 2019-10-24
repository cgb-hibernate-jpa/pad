package com.github.emailtohl.pad.xml;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.emailtohl.pad.xml.Attrs;

public class AttrsTest {
	Attrs a = new Attrs();
	Attrs b = new Attrs();

	@Before
	public void setUp() throws Exception {
		a.put("foo", "bar");
		a.put("baz", "fuz");
		a.put("empty", " ");
		b.put("foo", "    bar   ");
		b.put("baz", "fuz");
		b.put("empty", null);
		b.put("no-content-1", null);
		b.put("no-content-2", "   ");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testHashCode() {
		int ha = a.hashCode();
		int hb = b.hashCode();
		assertEquals(ha, hb);
	}

	@Test
	public void testEqualsObject() {
		assertEquals(a, b);
	}

}
