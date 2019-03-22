package com.github.emailtohl.lib.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.store.RAMDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public class TextFileSearchTest {
	private static final Logger logger = LogManager.getLogger();
	private static final String SEARCH_QUERY = "长江 英雄 秋月";
	Random r = new Random();
	File tempDir;
	File textFile;
	RAMDirectory directory;
	TextFileSearch fs;

	@Before
	public void setUp() throws Exception {
		String text = "滚滚长江东逝水，浪花淘尽英雄。是非成败转头空。\r\n" + 
				"    　青山依旧在，几度夕阳红。\r\n" + 
				"    　白发渔樵江渚上，惯看秋月春风。一壶浊酒喜相逢。\r\n" + 
				"      古今多少事，都付笑谈中。\r\n" + 
				"　　　　　　　　　　　　　　　　　　——调寄《临江仙》";
		tempDir = new File(System.getProperty("java.io.tmpdir"), "testDir");
		if (!tempDir.exists()) {
			tempDir.mkdir();
		}
		textFile = new File(tempDir, "临江仙");
		if (!textFile.exists()) {
			textFile.createNewFile();
		}
		FileUtils.write(textFile, text, StandardCharsets.UTF_8);

		// 创建一个内存索引目录
		directory = new RAMDirectory();
		fs = new TextFileSearch(directory);
		fs.index(tempDir);
	}

	@After
	public void tearDown() throws Exception {
		fs.close();
		textFile.delete();
		tempDir.delete();
	}

	@Test
	public void test() throws IOException, InterruptedException {
		Set<String> result = fs.searchForFilePath(SEARCH_QUERY);
		result.forEach(logger::debug);
		assertFalse(result.isEmpty());
		
		List<Document> docs = fs.search(SEARCH_QUERY);
		assertEquals(1, docs.size());

		Page<Document> page = fs.search(SEARCH_QUERY, PageRequest.of(0, 5));
		for (Document d : page.getContent()) {
			logger.debug(d);
		}
		assertEquals(1, page.getNumberOfElements());

		// 测试并发
		short count = 100;
		CountDownLatch latch = new CountDownLatch(count);
		ExecutorService exec = Executors.newCachedThreadPool();
		for (int i = 0; i < count; i++) {
			exec.submit(() -> {
				try {
					FileUtils.writeStringToFile(textFile, r.nextInt(100) + " ", StandardCharsets.UTF_8, true);
					fs.updateIndex(textFile);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					latch.countDown();
				}
			});
		}
		for (int i = 0; i < count; i++) {
			exec.submit(() -> {
				Set<String> r = fs.searchForFilePath("渔樵 浊酒");
				assertFalse(r.isEmpty());
				List<Document> _docs = fs.search(SEARCH_QUERY);
				assertEquals(1, _docs.size());
			});
		}
		latch.await();
		
		if (logger.isTraceEnabled()) {
			logger.trace(FileUtils.readFileToString(textFile, StandardCharsets.UTF_8));
		}
		
		result = fs.searchForFilePath("笑谈");
		logger.debug(result);
		assertFalse(result.isEmpty());
		fs.deleteIndex(textFile);
		docs = fs.search("笑谈");
		assertTrue(docs.isEmpty());
	}
}
