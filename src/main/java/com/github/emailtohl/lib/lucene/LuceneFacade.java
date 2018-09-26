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
import org.apache.lucene.search.BooleanQuery;
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
public class LuceneFacade implements AutoCloseable {
	/** 在IndexWriter中是不能获取到docId的（分段合并会发生变化），所以需要唯一标识一个Document的属性 */
	public static final String ID_NAME = "UUID";
	/** Document的属性名，创建时间 */
	public static final String CREATE_TIME = "CREATE_TIME";
	/** 索引和查询时使用的分词器 */
	public final Analyzer analyzer;
	/** 日志 */
	private final Logger LOG = LogManager.getLogger();
	/** 记录文档有哪些属性，便于查询 */
	private final Set<String> indexableFieldNames = new CopyOnWriteArraySet<String>();
	/** 默认查询前TOP_HITS个文档 */
	private final int DEFAULT_TOP_HITS = 100;
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
	 * @param indexPath 指定索引存储地址
	 * @param analyzer 指定索引和搜索使用的分词器
	 * @throws IOException 来自底层的输入输出异常
	 */
	public LuceneFacade(Directory indexPath, Analyzer analyzer) throws IOException {
		this.analyzer = analyzer;
		IndexWriterConfig conf = new IndexWriterConfig(analyzer);
		// 每一次访问，创建新的索引,第二次访问，删掉原来的创建新的索引
		conf.setOpenMode(OpenMode.CREATE);
		writer = new IndexWriter(indexPath, conf);
		reader = DirectoryReader.open(writer);
		searcher = new IndexSearcher(reader);
	}

	/**
	 * 指定索引目录，也可以接受内存形式的索引目录
	 * 
	 * @param indexPath 索引目录
	 * @throws IOException 来自底层的输入输出异常
	 */
	public LuceneFacade(Directory indexPath) throws IOException {
		this(indexPath, new StandardAnalyzer());
	}
	
	/**
	 * 只接受文件系统的索引目录
	 * 
	 * @param indexPath 文件系统的索引目录
	 * @throws IOException 来自底层的输入输出异常
	 */
	public LuceneFacade(String indexPath) throws IOException {
		this(FSDirectory.open(Paths.get(indexPath)), new StandardAnalyzer());
	}
	
	/**
	 * 若使用默认构造器，则索引基于内存
	 * @throws IOException 来自底层的输入输出异常
	 */
	public LuceneFacade() throws IOException {
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
		} catch (IOException e) {
			LOG.error("Lucene Searcher throw the Exception", e);
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
	 * <p>根据字段名和值精确查询第一个文档，一般用于唯一性键值查询</p>
	 * <p>这里使用TermQuery进行精确查询，所以需要业务上保证此域的值唯一性</p>
	 * <p>另外域的类型一般选择StringField，即在索引期间不做分词处理，否则原始值会被分词器处理，如去掉停用词，转为全小写等操作，造成查询失败</p>
	 * @param fieldName 索引时，建议用StringField存储的字段名
	 * @param value 查询的值
	 * @return Lucene的文档
	 */
	public Document first(String fieldName, String value) {
		Document doc = null;
		try {
			// 若正在执行refreshIndexReader中，那么就在此处等待
			// 同一时间也只能由一个查询线程修改queryCount
			synchronized (this) {
				queryCount++;
			}
			Query query = new TermQuery(new Term(fieldName, value));
			TopDocs docs = searcher.search(query, 1);
			if (docs.scoreDocs.length == 0) {
				return null;
			}
			doc = searcher.doc(docs.scoreDocs[0].doc);
		} catch (IOException e) {
			LOG.error("Lucene Searcher throw the Exception", e);
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
	 * 代理原搜索器的搜索方法，对搜索的线程进行统计，以保证搜索器在执行时不被关闭
	 * @param query 结构化的查询参数
	 * @return 搜索结果，包括总数量，最大评分以及Lucene文档集合
	 * @throws IOException BooleanQuery.TooManyClauses If a query would exceed 
     *         {@link BooleanQuery#getMaxClauseCount()} clauses.
	 */
	public Result search(Query query) throws IOException {
		try {
			// 若正在执行refreshIndexReader中，那么就在此处等待
			// 同一时间也只能由一个查询线程修改queryCount
			synchronized (this) {
				queryCount++;
			}
			TopDocs topDocs = searcher.search(query, DEFAULT_TOP_HITS);
			Result result = new Result(topDocs);
			for (ScoreDoc sd : topDocs.scoreDocs) {
				Document doc = searcher.doc(sd.doc);
				result.documents.add(doc);
			}
			return result;
		} finally {
			synchronized (this) {
				// 无论发送什么错误也必须复原queryCount状态，并通知等待中的线程
				queryCount--;
				notifyAll();
			}
		}
	}
	
	/**
	 * 分段查询
	 * 代理原搜索器的搜索方法，对搜索的线程进行统计，以保证搜索器在执行时不被关闭
	 * @param query 结构化的查询参数
	 * @param offset 数量
	 * @param size 每页大小
	 * @return 搜索结果，包括总数量，最大评分以及Lucene文档集合
	 * @throws IOException BooleanQuery.TooManyClauses If a query would exceed 
     *         {@link BooleanQuery#getMaxClauseCount()} clauses.
	 */
	public Result search(Query query, int offset, int size) throws IOException {
		try {
			// 若正在执行refreshIndexReader中，那么就在此处等待
			// 同一时间也只能由一个查询线程修改queryCount
			synchronized (this) {
				queryCount++;
			}
			int end = offset + size;
			TopDocs topDocs = searcher.search(query, DEFAULT_TOP_HITS);
			Result result = new Result(topDocs);
			for (int i = offset; i < end && i < topDocs.totalHits; i++) {
				ScoreDoc sd = topDocs.scoreDocs[i];
				Document doc = searcher.doc(sd.doc);
				result.documents.add(doc);
			}
			return result;
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
	 * @param queryString 查询字符串
	 * @return 搜索结果，包括总数量，最大评分以及Lucene文档集合
	 */
	public Result search(String queryString) {
		String[] fields = new String[indexableFieldNames.size()];
		QueryParser queryParser = new MultiFieldQueryParser(indexableFieldNames.toArray(fields), analyzer);
		try {
			Query query = queryParser.parse(queryString);
			return search(query);
		} catch (IOException e) {
			LOG.error("Lucene Searcher throw the Exception", e);
			return new Result(null);
		} catch (ParseException e) {
			LOG.error("Query statement parsing failed", e);
			return new Result(null);
		}
	}

	/**
	 * 分段查询出Lucene原始的Document对象
	 * 
	 * @param queryString 查询字符串
	 * @param offset 起始序号
	 * @param size 每页大小
	 * @return 搜索结果，包括总数量，最大评分以及Lucene文档集合
	 */
	public Result search(String queryString, int offset, int size) {
		String[] fields = new String[indexableFieldNames.size()];
		QueryParser queryParser = new MultiFieldQueryParser(indexableFieldNames.toArray(fields), analyzer);
		try {
			Query query = queryParser.parse(queryString);
			return search(query, offset, size);
		} catch (IOException e) {
			LOG.error("Lucene Searcher throw the Exception", e);
			return new Result(null);
		} catch (ParseException e) {
			LOG.error("Query statement parsing failed", e);
			return new Result(null);
		}
	}
	
	/**
	 * 搜索结果数据存储结构
	 * 
	 * @author HeLei
	 */
	public static class Result {
		public final List<Document> documents = new ArrayList<Document>();
		public final int totalHits;
		public final float maxScore;
		public Result(TopDocs topDocs) {
			if (topDocs == null) {
				this.totalHits = 0;
				this.maxScore = 0;
			} else {
				this.totalHits = topDocs.totalHits;
				this.maxScore = topDocs.getMaxScore();
			}
		}
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
				analyzer.close();
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
