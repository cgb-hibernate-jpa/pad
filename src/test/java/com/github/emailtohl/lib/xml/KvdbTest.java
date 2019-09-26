package com.github.emailtohl.lib.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.StringUtils;

public class KvdbTest {
	Kvdb db;

	@Before
	public void setUp() throws Exception {
		File tmp = new File(System.getProperty("java.io.tmpdir"));
		tmp = new File(tmp, "redisdata.xml");
		db = new Kvdb(tmp.getPath());
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testString() {
		String key = "\"foo\"";
		String value = "bar";
		db.saveString(key, value);
		String res = db.readString(key);
		assertEquals(value, res);
		db.delString(key);
		res = db.readString(key);
		assertFalse(StringUtils.hasText(res));
		
		value = null;
		db.saveString(key, value);
		res = db.readString(key);
		assertNull(res);
	}

	@Test
	public void testHash() {
		String key = "bar";
		String hkey = "baz";
		String value = "foo\\";
		db.saveHash(key, hkey, value);
		String res = db.readHash(key, hkey);
		assertEquals(value, res);
		db.delHash(key, hkey);
		res = db.readHash(key, hkey);
		assertNull(value, res);

		Map<String, String> mk = new HashMap<String, String>();
		mk.put(hkey, value);
		db.saveHashAll(key, mk);
		res = db.readHash(key, hkey);
		assertEquals(value, res);
		assertNotNull(db.readHashAll(key));
		
		
		value = null;
		db.saveHash(key, hkey, value);
		res = db.readHash(key, hkey);
		assertNull(res);
	}

	@Test
	public void testSet() {
		String key = "baz";
		String value = "fux";
		db.saveSet(key, value);
		assertTrue(db.readSet(key).contains(value));
	}

	@Test
	public void testList() {
		String key = "fux";
		String value = "baz";
		db.rightPushList(key, value);
		assertTrue(db.readList(key).contains(value));
	}

}
