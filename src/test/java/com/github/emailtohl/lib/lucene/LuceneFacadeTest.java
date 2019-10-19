package com.github.emailtohl.lib.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.emailtohl.lib.lucene.LuceneFacade.Result;

public class LuceneFacadeTest {
	private String field_name = "compName_s";
	private String field_address = "address_s";

	private static Random random = new Random();
	private static final int count = 10;
	private CountDownLatch countDownLatch;
	private CopyOnWriteArrayList<Long> ids = new CopyOnWriteArrayList<Long>();
	
	LuceneFacade facade;

	@Before
	public void setUp() throws Exception {
		countDownLatch = new CountDownLatch(count);
		facade = new LuceneFacade();
		List<Document> documents = getDocuments();
		facade.index(documents);
		
		for (int i = 0; i < count; i++) {
			new Thread(() -> {
				try {
					long id;
					switch (random.nextInt(3)) {
					case 0:
						id = facade.create(createDocument());
						ids.add(id);
						break;
					case 1:
						try {
							id = ids.get(random.nextInt(ids.size()));
							facade.update(id, createDocument());
						} catch (IndexOutOfBoundsException ignored) {}
						break;
					case 2:
						try {
							id = ids.get(random.nextInt(ids.size()));
							facade.delete(id);
						} catch (IndexOutOfBoundsException ignored) {}
						break;
					default:
						break;
					}
				} catch (IOException | IllegalArgumentException ignored) {
				} finally {
					countDownLatch.countDown();
				}
			}).start();
		}
	}

	private Document createDocument() {
		Document doc = new Document();
		doc.add(new TextField(field_name, UUID.randomUUID().toString(), Store.NO));
		doc.add(new TextField(field_address, UUID.randomUUID().toString(), Store.NO));
		return doc;
	}
	
	@After
	public void tearDown() throws Exception {
		facade.close();
	}

	@Test
	public void testCRUD() throws IOException, InterruptedException {
		Document doc = new Document();
		doc.add(new StringField("number", "F8V7067-APL-KIT", Store.YES));
		doc.add(new TextField("name", "Belkin Mobile Power Cord for iPod w/ Dock", Store.NO));
		doc.add(new TextField("manu", "Belkin", Store.NO));
		long id = facade.create(doc);
		
		// 再添加一个文档，使其增加id
		doc = new Document();
		doc.add(new StringField("isbn", "978-1423103349", Store.YES));
		doc.add(new TextField("name", "Percy Jackson and the Olympians", Store.NO));
		doc.add(new TextField("author", "Rick Riordan", Store.NO));
		doc.add(new DoubleField("price", 6.49, Store.NO));
		facade.create(doc);
		
		doc = facade.read(id);
		assertNotNull(doc);
		assertEquals("F8V7067-APL-KIT", doc.get("number"));
		assertEquals(Long.toString(id), doc.get(LuceneFacade.ID_NAME));
		
		long _id = facade.getId(doc);
		assertEquals(id, _id);
		
		doc = facade.first("isbn", "978-1423103349");
		assertNotNull(doc);
		assertEquals("978-1423103349", doc.get("isbn"));
		
		doc = new Document();
		doc.add(new StringField("number", "IW-02", Store.YES));
		doc.add(new TextField("name", "iPod &amp; iPod Mini USB 2.0 Cable", Store.NO));
		doc.add(new TextField("manu", "Belkin", Store.NO));
		long newId = facade.update(id, doc);
		assertNotEquals(id, newId);
		doc = facade.read(newId);
		assertEquals("IW-02", doc.get("number"));
		assertEquals(Long.toString(newId), doc.get(LuceneFacade.ID_NAME));
		
		doc = facade.read(id);
		assertNull(doc);
		
		facade.delete(newId);
		doc = facade.read(newId);
		assertNull(doc);
		
		countDownLatch.await();
	}

	@Test
	public void testSearch() throws InterruptedException {
		Result r = facade.search("Apple");
		assertFalse(r.documents.isEmpty());
		
		LuceneFacade.Result page = facade.search("Belkin", 0, 10);
		assertFalse(page.documents.isEmpty());
		countDownLatch.await();
	}
	
	private List<Document> getDocuments() {
		List<Document> documents = new ArrayList<Document>();
		Document doc = new Document();
		doc.add(new TextField(field_name, "A-Data Technology", Store.NO));
		doc.add(new TextField(field_address, "46221 Landing Parkway Fremont, CA 94538", Store.NO));
		documents.add(doc);
		
		doc = new Document();
		doc.add(new TextField(field_name, "Apple", Store.NO));
		doc.add(new TextField(field_address, "1 Infinite Way, Cupertino CA", Store.NO));
		documents.add(doc);
		
		doc = new Document();
		doc.add(new TextField(field_name, "ASUS Computer", Store.NO));
		doc.add(new TextField(field_address, "800 Corporate Way Fremont, CA 94539", Store.NO));
		documents.add(doc);
		
		doc = new Document();
		doc.add(new TextField(field_name, "ATI Technologies", Store.NO));
		doc.add(new TextField(field_address, "33 Commerce Valley Drive East Thornhill, ON L3T 7N6 Canada", Store.NO));
		documents.add(doc);
		
		doc = new Document();
		doc.add(new TextField(field_name, "Belkin", Store.NO));
		doc.add(new TextField(field_address, "12045 E. Waterfront Drive Playa Vista, CA 90094", Store.NO));
		documents.add(doc);
		
		doc = new Document();
		doc.add(new TextField(field_name, "Canon, Inc.", Store.NO));
		doc.add(new TextField(field_address, "One Canon Plaza Lake Success, NY 11042", Store.NO));
		documents.add(doc);
		
		doc = new Document();
		doc.add(new TextField(field_name, "Corsair Microsystems", Store.NO));
		doc.add(new TextField(field_address, "46221 Landing Parkway Fremont, CA 94538", Store.NO));
		documents.add(doc);
		
		doc = new Document();
		doc.add(new TextField(field_name, "Dell, Inc.", Store.NO));
		doc.add(new TextField(field_address, "One Dell Way Round Rock, Texas 78682", Store.NO));
		documents.add(doc);
		
		doc = new Document();
		doc.add(new TextField(field_name, "Maxtor Corporation", Store.NO));
		doc.add(new TextField(field_address, "920 Disc Drive Scotts Valley, CA 95066", Store.NO));
		documents.add(doc);
		
		doc = new Document();
		doc.add(new TextField(field_name, "Samsung Electronics Co. Ltd.", Store.NO));
		doc.add(new TextField(field_address, "105 Challenger Rd. Ridgefield Park, NJ 07660-0511", Store.NO));
		documents.add(doc);
		
		doc = new Document();
		doc.add(new TextField(field_name, "ViewSonic Corp", Store.NO));
		doc.add(new TextField(field_address, "381 Brea Canyon Road Walnut, CA 91789-0708", Store.NO));
		documents.add(doc);
		
		return documents;
	}

}
