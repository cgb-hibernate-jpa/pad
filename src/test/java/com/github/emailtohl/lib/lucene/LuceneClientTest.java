package com.github.emailtohl.lib.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LuceneClientTest {
	private String field_name = "compName_s";
	private String field_address = "address_s";
	LuceneClient client;

	@Before
	public void setUp() throws Exception {
		client = new LuceneClient();
		client.setAnalyzer(new StandardAnalyzer());
		List<Document> documents = getDocuments();
		client.index(documents);
	}

	@After
	public void tearDown() throws Exception {
		client.close();
	}

	@Test
	public void testCRUD() throws IOException {
		Document doc = new Document();
		doc.add(new TextField("number", "F8V7067-APL-KIT", Store.YES));
		doc.add(new TextField("name", "Belkin Mobile Power Cord for iPod w/ Dock", Store.NO));
		doc.add(new TextField("manu", "Belkin", Store.NO));
		long id = client.create(doc);
		
		// 再添加一个文档，使其增加id
		doc = new Document();
		doc.add(new TextField("isbn", "978-1423103349", Store.YES));
		doc.add(new TextField("name", "Percy Jackson and the Olympians", Store.NO));
		doc.add(new TextField("author", "Rick Riordan", Store.NO));
		doc.add(new DoubleField("price", 6.49, Store.NO));
		client.create(doc);
		
		doc = client.read(id);
		assertNotNull(doc);
		assertEquals("F8V7067-APL-KIT", doc.get("number"));
		
		doc = new Document();
		doc.add(new TextField("number", "IW-02", Store.YES));
		doc.add(new TextField("name", "iPod &amp; iPod Mini USB 2.0 Cable", Store.NO));
		doc.add(new TextField("manu", "Belkin", Store.NO));
		long newId = client.update(id, doc);
		assertNotEquals(id, newId);
		doc = client.read(newId);
		assertEquals("IW-02", doc.get("number"));
		
		doc = client.read(id);
//		assertNull(doc);
		
		client.delete(newId);
		doc = client.read(newId);
		assertNull(doc);
		
	}

	@Test
	public void testUpdateIndex() {
		fail("Not yet implemented");
	}

	@Test
	public void testDeleteIndex() {
		fail("Not yet implemented");
	}

	@Test
	public void testQueryString() {
		fail("Not yet implemented");
	}

	@Test
	public void testQueryStringPageable() {
		fail("Not yet implemented");
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
