package com.github.emailtohl.pad.xml;

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

import com.github.emailtohl.pad.xml.Kvdb;

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
		db.set(key, value);
		String res = db.get(key);
		assertEquals(value, res);
		db.del(key);
		res = db.get(key);
		assertFalse(StringUtils.hasText(res));
		
		value = null;
		db.set(key, value);
		res = db.get(key);
		assertNull(res);
	}

	@Test
	public void testHash() {
		String key = "bar";
		String hkey = "baz";
		String value = "foo\\";
		db.hset(key, hkey, value);
		String res = db.hget(key, hkey);
		assertEquals(value, res);
		db.hdel(key, hkey);
		res = db.hget(key, hkey);
		assertNull(value, res);

		Map<String, String> mk = new HashMap<String, String>();
		mk.put(hkey, value);
		db.hsetall(key, mk);
		res = db.hget(key, hkey);
		assertEquals(value, res);
		assertNotNull(db.hgetall(key));
		
		
		value = null;
		db.hset(key, hkey, value);
		res = db.hget(key, hkey);
		assertNull(res);
	}

	@Test
	public void testSet() {
		String key = "baz";
		String value = "fux";
		db.sadd(key, value);
		assertTrue(db.sgetall(key).contains(value));
	}

	@Test
	public void testList() {
		String key = "fux";
		String value = "baz";
		db.rpush(key, value);
		assertTrue(db.lrangeall(key).contains(value));
	}

}
