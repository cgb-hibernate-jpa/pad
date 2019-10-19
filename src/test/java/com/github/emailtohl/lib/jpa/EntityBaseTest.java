package com.github.emailtohl.lib.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class EntityBaseTest {
	static class Foo extends EntityBase {
		private static final long serialVersionUID = -1617435954901924810L;
		short s1;
		Short s2 = new Short((short) 0);
		int i1;
		Integer i2;
		String str = "";
		boolean b1;
		Boolean b2 = new Boolean(false);
		Em em = Em.A;
		byte by1;
		Byte by2 = new Byte((byte) 0);
		char c1 = 'A';
		Character c2 = new Character('A');
	}
	static class Bar extends Foo {
		private static final long serialVersionUID = 2706461043553392610L;
		double d1;
		Double d2 = new Double(0.0);
		long l1;
		Long l2 = new Long(0L);
		LocalDate now1 = LocalDate.now();
		LocalTime now2 = LocalTime.now();
		Instant now3 = Instant.now();
		LocalDateTime now4 = LocalDateTime.now();
		Date now5 = new Date();
		Calendar now6 = Calendar.getInstance();
		
		Bar parent;
	}
	
	@Test
	public void testClone() throws InterruptedException {
		int n = 5;
		CountDownLatch count = new CountDownLatch(n);
		for (int i = 0; i < n; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					Bar src = new Bar();
					src.d1 = 1.1;
					src.d2 = new Double(1.1);
					src.l1 = 1L;
					src.l2 = new Long(1L);
					src.now1 = src.now1.minusYears(1L);
					src.now2 = src.now2.minusHours(1L);
					src.now3 = src.now3.minusSeconds(1000L);
					src.now4 = src.now4.minusYears(1L);
					src.now5 = Date.from(src.now3);
					src.now6.set(Calendar.YEAR, src.now1.getYear());
					src.s1 = 1;
					src.s2 = new Short((short) 1);
					src.i1 = 1;
					src.i2 = new Integer(1);
					src.str = "hellow world";
					src.b1 = true;
					src.b2 = new Boolean(true);
					src.em = Em.B;
					src.by1 = 1;
					src.by2 = new Byte((byte) 1);
					src.c1 = 'B';
					src.c2 = new Character('B');
					src.parent = new Bar();

					Bar tar = (Bar) src.clone();
					assertTrue(src.d1 == tar.d1);
					assertEquals(src.d2, tar.d2);
					
					assertTrue(src.l1 == tar.l1);
					assertEquals(src.l2, tar.l2);
					
					assertEquals(src.now1, tar.now1);
					assertEquals(src.now2, tar.now2);
					assertEquals(src.now3, tar.now3);
					assertEquals(src.now4, tar.now4);
					assertEquals(src.now5, tar.now5);
					assertEquals(src.now6, tar.now6);
					
					assertTrue(src.s1 == tar.s1);
					assertEquals(src.s2, tar.s2);
					
					assertTrue(src.i1 == tar.i1);
					assertEquals(src.i2, tar.i2);
					
					assertEquals(src.str, tar.str);
					
					assertTrue(src.b1 == tar.b1);
					assertEquals(src.b2, tar.b2);
					
					assertTrue(src.em == tar.em);
					
					assertTrue(src.by1 == tar.by1);
					assertEquals(src.by2, tar.by2);
					
					assertTrue(src.c1 == tar.c1);
					assertEquals(src.c2, tar.c2);
					assertNull(tar.parent);
					count.countDown();
				}
			}).start();
		}
		count.await();
	}

}

