package com.github.emailtohl.lib.lucene;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

/**
 * <p>对Lucene的IndexWriter和IndexReader进行简易的封装</p>
 * <p>主要是自实现IndexWriter和IndexReader的线程控制，确保索引更新后，IndexReader在没有运行时关闭并重建</p>
 * <p>仿数据库的访问方式，会自动为Document添加上id和create_time</p>
 * 
 * @author HeLei
 */
public class LuceneClient implements AutoCloseable {
	/** 在IndexWriter中是不能获取到docId的（分段合并会发生变化），所以需要唯一标识一个Document的属性 */
	public static final String ID_NAME = "UUID";
	/** Document的属性名，创建时间 */
	public static final String CREATE_TIME = "CREATE_TIME";
	/** 日志 */
	private final Logger LOG = LogManager.getLogger();
	/** 记录文档有哪些属性，便于查询 */
	private final Set<String> indexableFieldNames = new CopyOnWriteArraySet<String>();
	/** 查询前TOP_HITS个文档 */
	private final int TOP_HITS = 50;
	/** 索引写入器 */
	private final IndexWriter writer;
	/** 索引读取器 */
	private IndexReader reader;
	/** 搜索器 */
	private IndexSearcher searcher;
	/** 查询线程的计数器，没有修改索引或没有执行查询时为0，修改索引会小于0，执行查询会大于0 */
	private volatile int queryCount = 0;
	
	/**
	 * 构造LuceneClient
	 * @param indexBase 指定索引存储地址
	 * @param analyzer 指定索引和搜索使用的分词器
	 * @throws IOException 来自底层的输入输出异常
	 */
	public LuceneClient(Directory indexBase, Analyzer analyzer) throws IOException {
		IndexWriterConfig conf = new IndexWriterConfig(analyzer);
		// 每一次访问，创建新的索引,第二次访问，删掉原来的创建新的索引
		conf.setOpenMode(OpenMode.CREATE);
		writer = new IndexWriter(indexBase, conf);
		reader = DirectoryReader.open(writer);
		searcher = new IndexSearcher(reader);
	}

	/**
	 * 指定索引目录，也可以接受内存形式的索引目录
	 * 
	 * @param indexBase 索引目录
	 * @throws IOException 来自底层的输入输出异常
	 */
	public LuceneClient(Directory indexBase) throws IOException {
		this(indexBase, new StandardAnalyzer());
	}
	
	/**
	 * 只接受文件系统的索引目录
	 * 
	 * @param path 文件系统的索引目录
	 * @throws IOException 来自底层的输入输出异常
	 */
	public LuceneClient(String path) throws IOException {
		this(FSDirectory.open(Paths.get(path)), new StandardAnalyzer());
	}
	
	/**
	 * 若使用默认构造器，则索引基于内存
	 * @throws IOException 来自底层的输入输出异常
	 */
	public LuceneClient() throws IOException {
		this(new RAMDirectory(), new StandardAnalyzer());
	}

	/**
	 * 将多个Document添加进索引，indexWriter是线程安全的，修改索引不必加锁
	 * 
	 * @param documents 要添加进索引的文档，执行后，每个Document中会添加id属性
	 * @throws IOException 来自底层的输入输出异常
	 */
	public void index(List<Document> documents) throws IOException {
		for (Document doc : documents) {
			doc.add(new StringField(ID_NAME, UUID.randomUUID().toString(), Store.YES));
			doc.add(new LongField(CREATE_TIME, System.currentTimeMillis(), Store.YES));
			for (IndexableField field : doc.getFields()) {
				indexableFieldNames.add(field.name());
			}
			writer.addDocument(doc);
		}
		writer.commit();
		if (LOG.isDebugEnabled()) {
			LOG.debug("numDocs: {}", writer.numDocs());
		}
		refreshIndexReader();
	}

	/**
	 * 添加一个文档进索引，indexWriter是线程安全的，修改索引不必加锁
	 * @param document 新增的文档，执行后，document中会添加id属性
	 * @return 新增文档ID_NAME Field中的值
	 * @throws IOException 来自底层的输入输出异常
	 */
	public String create(Document document) throws IOException {
		String id = UUID.randomUUID().toString();
		document.add(new StringField(ID_NAME, id, Store.YES));
		document.add(new LongField(CREATE_TIME, System.currentTimeMillis(), Store.YES));
		for (IndexableField field : document.getFields()) {
			indexableFieldNames.add(field.name());
		}
		writer.addDocument(document);
		writer.commit();
		refreshIndexReader();
		return id;
	}
	
	/**
	 * 根据ID_NAME Field中的值，获取文档
	 * @param id ID_NAME Field中的值，能唯一标识这个文档
	 * @return lucene中的文档，若未查找到，则返回null
	 */
	public Document read(String id) {
		Document doc = null;
		try {
			// 若正在执行refreshIndexReader中，那么就在此处等待
			// 同一时间也只能由一个查询线程修改queryCount
			synchronized (this) {
				queryCount++;
			}
			Query query = new TermQuery(new Term(ID_NAME, id));
			TopDocs docs = searcher.search(query, 1);
			if (docs.scoreDocs.length == 0) {
				return null;
			}
			doc = searcher.doc(docs.scoreDocs[0].doc);
			LOG.debug(doc);
		} catch (IOException e) {
			LOG.error("Failed to open the index library", e);
		} finally {
			synchronized (this) {
				// 无论发送什么错误也必须复原queryCount状态，并通知等待中的线程
				queryCount--;
				notifyAll();
			}
		}
		return doc;
	}
	
	/**
	 * 更新索引，现将原文档删除，然后再添加新文档，indexWriter是线程安全的，修改索引不必加锁
	 * 
	 * @param id ID_NAME Field中的值，能唯一标识这个文档
	 * @param document 更新的文档，执行后，document中会添加id属性
	 * @return 新增文档ID_NAME Field中的值
	 * @throws IOException 来自底层的输入输出异常
	 */
	public String update(String id, Document document) throws IOException {
		String newId = UUID.randomUUID().toString();
		document.add(new StringField(ID_NAME, newId, Store.YES));
		document.add(new LongField(CREATE_TIME, System.currentTimeMillis(), Store.YES));
		for (IndexableField field : document.getFields()) {
			indexableFieldNames.add(field.name());
		}
		writer.updateDocument(new Term(ID_NAME, id), document);
		writer.commit();
		refreshIndexReader();
		return newId;
	}

	/**
	 * 在索引中删除一个文档，indexWriter是线程安全的，修改索引不必加锁
	 * 
	 * @param id ID_NAME Field中的值，能唯一标识这个文档
	 * @throws IOException 来自底层的输入输出异常
	 */
	public void delete(String id) throws IOException {
		writer.deleteDocuments(new Term(ID_NAME, id));
		writer.commit();
		refreshIndexReader();
	}
	
	/**
	 * 当索引变更时，为保持查询有效，需更新IndexReader
	 * 
	 * @throws IOException 来自底层的输入输出异常
	 */
	private void refreshIndexReader() throws IOException {
		synchronized (this) {
			try {
				// 当有搜索还在进行时，需要等待
				while (queryCount > 0)
					wait();
				
				IndexReader newReader = DirectoryReader.openIfChanged((DirectoryReader) reader);
				if (newReader != null && newReader != reader) {
					reader.close();
					reader = newReader;
					searcher = new IndexSearcher(reader);
				}
			} catch (InterruptedException e) {
				LOG.catching(e);
			}
		}
	}
	
	/**
	 * 执行搜索
	 * @param queryString 查询字符串
	 * @return Lucene原始的顶级文档引用
	 * @throws ParseException 参数解析成Query对象发生异常
	 * @throws IOException 来自底层的输入输出异常
	 */
	private TopDocs searchTopDocs(String queryString) throws ParseException, IOException {
		try {
			// 若正在执行refreshIndexReader中，那么就在此处等待
			// 同一时间也只能由一个查询线程修改queryCount
			synchronized (this) {
				queryCount++;
			}
			String[] fields = new String[indexableFieldNames.size()];
			QueryParser queryParser = new MultiFieldQueryParser(indexableFieldNames.toArray(fields),
					writer.getAnalyzer());
			Query query = queryParser.parse(queryString);
			return searcher.search(query, TOP_HITS);
		} finally {
			synchronized (this) {
				// 无论发送什么错误也必须复原queryCount状态，并通知等待中的线程
				queryCount--;
				notifyAll();
			}
		}
	}
	
	/**
	 * 查询出Lucene原始的Document对象
	 * 
	 * @param query 查询字符串
	 * @return lucene的文档列表
	 */
	public List<Document> search(String query) {
		List<Document> list = new ArrayList<Document>();
		try {
			TopDocs docs = searchTopDocs(query);
			LOG.debug(docs.totalHits);
			for (ScoreDoc sd : docs.scoreDocs) {
				Document doc = searcher.doc(sd.doc);
				LOG.debug(doc);
				list.add(doc);
			}
		} catch (IOException e) {
			LOG.error("Failed to open the index library", e);
		} catch (ParseException e) {
			LOG.error("Query statement parsing failed", e);
		}
		return list;
	}
	
	/**
	 * 存储分段查询的数据结构
	 * 
	 * @author HeLei
	 */
	public static class Fragment {
		public final List<Document> documents = new ArrayList<Document>();
		public int totalHits;
		public float maxScore;
	}

	/**
	 * 分段查询出Lucene原始的Document对象
	 * 
	 * @param query 查询字符串
	 * @param offset 起始序号
	 * @param size 每页大小
	 * @return 分段查询的结果
	 */
	public Fragment search(String query, int offset, int size) {
		Fragment fragment = new Fragment();
		try {
			TopDocs docs = searchTopDocs(query);
			LOG.debug(docs.totalHits);
			fragment.totalHits = docs.totalHits;
			fragment.maxScore = docs.getMaxScore();
			int end = offset + size;
			for (int i = offset; i < end && i < docs.totalHits; i++) {
				ScoreDoc sd = docs.scoreDocs[i];
				Document doc = searcher.doc(sd.doc);
				LOG.debug(doc);
				fragment.documents.add(doc);
			}
		} catch (IOException e) {
			LOG.error("Failed to open the index library", e);
		} catch (ParseException e) {
			LOG.error("Query statement parsing failed", e);
		}
		return fragment;
	}
	
	/**
	 * 关闭所有资源
	 * 
	 * @throws IOException 来自底层的输入输出异常
	 */
	@Override
	public void close() throws IOException {
		// 当修改索引还在进行时，需要等待
		synchronized (this) {
			try {
				// 当有搜索还在进行时，需要等待
				while (queryCount > 0)
					wait();

				reader.close();
				if (writer.isOpen())
					writer.close();
			} catch (InterruptedException e) {
				LOG.catching(e);
			}
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}
	
}
