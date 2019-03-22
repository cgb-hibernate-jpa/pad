package com.github.emailtohl.lib.util;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

public class SnowflakeIdWorkerTest {

	@Test
	public void testUnique() {
		final int count = 500000;
		// 尽量让插入最快速
		LinkedList<Long> list = new LinkedList<Long>();
		SnowflakeIdWorker idWorker = new SnowflakeIdWorker(0, 0);

		long startTime = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			long id = idWorker.nextId();
			list.add(id);
		}
		long endTime = System.currentTimeMillis();
		System.out.println("SnowflakeIdWorker spend：" + (endTime - startTime) + "ms");
		
		// 过滤重复项
		Set<Long> set = new HashSet<Long>();
		set.addAll(list);
		
		// 若个数相同则证明没有重复项
		assertEquals(count, set.size());
		assertEquals(list.size(), set.size());
	}

	public void testConcurrency() throws InterruptedException {
		SnowflakeIdWorker idWorker = new SnowflakeIdWorker(1, 1);
		CopyOnWriteArraySet<Long> set = new CopyOnWriteArraySet<Long>();
		short count = 100;
		CountDownLatch latch = new CountDownLatch(count);
		ExecutorService exec = Executors.newCachedThreadPool();
		for (int i = 0; i < count; i++) {
			exec.submit(() -> set.add(idWorker.nextId()));
		}
		latch.await();
		assertEquals(count, set.size());
	}
	
}
