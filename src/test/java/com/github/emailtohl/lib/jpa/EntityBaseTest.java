package com.github.emailtohl.lib.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.github.emailtohl.lib.model.Bid;

public class EntityBaseTest {

	@Test
	public void testClone() throws InterruptedException {
		int n = 5;
		CountDownLatch count = new CountDownLatch(n);
		for (int i = 0; i < n; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					TestBean src = new TestBean();
					src.i = 2;
					src.b1 = false;
					src.b2 = false;
					src.by1 = 2;
					src.by2 = 2;
					src.c1 = 97;
					src.c2 = new Character('a');
					src.parent = new Bid();

					TestBean tar = (TestBean) src.clone();
					System.out.println(Thread.currentThread().getName() + "   assert");
					assertEquals(2, tar.i);
					assertFalse(tar.b1);
					assertFalse(tar.b2);
					assertTrue(2 == tar.by1);
					assertTrue(2 == tar.by2);
					assertEquals(97, tar.c1);
					assertEquals(new Character('a'), tar.c2);
					assertNull(tar.parent);
					count.countDown();
					System.out.println(count.getCount());
				}
			}).start();
		}
		count.await();
	}

}

class TestBean extends Bid {
	private static final long serialVersionUID = -7029749856275498527L;
	int ii;
	int i = 1;
	boolean b1 = true;
	Boolean b2 = true;
	byte by1 = 1;
	Byte by2 = 1;
	char c1 = 1;
	Character c2 = 1;
	Bid parent;
	Set<Object> aset = new HashSet<>();

	@Override
	public String toString() {
		return "TestBean [i=" + i + ", b1=" + b1 + ", b2=" + b2 + ", by1=" + by1 + ", by2=" + by2 + ", c1=" + c1
				+ ", c2=" + c2 + "]";
	}
}
