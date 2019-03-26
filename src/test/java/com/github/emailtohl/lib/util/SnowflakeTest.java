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

public class SnowflakeTest {
	@Test
	public void testUnique() {
		final int count = 500000;
		// 尽量让插入最快速
		LinkedList<Long> list = new LinkedList<Long>();
		SnowFlake snowFlake = new SnowFlake(0, 0);

		long startTime = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			long id = snowFlake.nextId();
			list.add(id);
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Snowflake spend：" + (endTime - startTime) + "ms");
		
		// 过滤重复项
		Set<Long> set = new HashSet<Long>();
		set.addAll(list);
		
		// 若个数相同则证明没有重复项
		assertEquals(count, set.size());
		assertEquals(list.size(), set.size());
	}

	// 测试多线程调用nextId()是否能获取唯一ID
	@Test
	public void testConcurrency1() throws InterruptedException {
		SnowFlake snowFlake = new SnowFlake(1, 1);
		CopyOnWriteArraySet<Long> set = new CopyOnWriteArraySet<Long>();
		short count = 100;
		CountDownLatch latch = new CountDownLatch(count);
		ExecutorService exec = Executors.newCachedThreadPool();
		for (int i = 0; i < count; i++) {
			exec.submit(() -> {
				set.add(snowFlake.nextId());
				latch.countDown();
			});
		}
		latch.await();
		assertEquals(count, set.size());
	}
	
	// 测试多个数据中心和多台机器各自生成ID是否冲突
	@Test
	public void testConcurrency2() throws InterruptedException {
		CopyOnWriteArraySet<Long> set = new CopyOnWriteArraySet<Long>();
		int count = (int) (SnowFlake.MAX_DATACENTER_NUM * SnowFlake.MAX_MACHINE_NUM);
		CountDownLatch latch = new CountDownLatch(count);
		ExecutorService exec = Executors.newCachedThreadPool();
		for (long i = 0; i < SnowFlake.MAX_DATACENTER_NUM; i++) {
			for (long j = 0; j < SnowFlake.MAX_MACHINE_NUM; j++) {
				SnowFlake snowFlake = new SnowFlake(i, j);
				exec.submit(() -> {
					long id = snowFlake.nextId();
					set.add(id);
					latch.countDown();
				});
			}
		}
		latch.await();
		assertEquals(count, set.size());
	}
}
