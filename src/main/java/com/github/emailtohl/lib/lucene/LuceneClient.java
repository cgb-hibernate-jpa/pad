package com.github.emailtohl.lib.lucene;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongField;
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
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * 对Lucene的IndexWriter和IndexReader进行简易的封装，自实现更新索引后新建IndexReader的功能。
 * 与数据库自动生成id一样，索引时，会自动添加id字段进Document中。
 * 注意：创建多个LuceneClient实例时需区分索引目录，以免引起并发冲突。
 * @author HeLei
 */
public class LuceneClient implements AutoCloseable {
	/** 唯一标识一个Document的属性名 */
	public static final String ID_NAME = "_id";
	/** Document的属性名，创建时间 */
	public static final String CREATE_TIME = "CREATE_TIME";
	/** id生成器，能唯一标识一个Document */
	private static final AtomicLong idCreator = new AtomicLong(1);
	/** 日志 */
	private final Logger LOG = LogManager.getLogger();
	/** 记录文档有哪些属性，便于查询 */
	private final Set<String> indexableFieldNames = new CopyOnWriteArraySet<String>();
	/** 查询前TOP_HITS个文档 */
	private final int TOP_HITS = 100;
	/** 分词器 */
	private Analyzer analyzer = new StandardAnalyzer();
	/** 索引存储目录 */
	private final Directory indexBase;
	/** 索引写入器 */
	private IndexWriter indexWriter;
	/** 索引读取器 */
	private IndexReader indexReader;
	/** 搜索器 */
	private IndexSearcher indexSearcher;
	/** 查询线程的计数器，没有查询时为0，这时候可以更新IndexReader */
	private volatile int queryCount = 0;
	/** 是否索引过，如果已经索引了，则不能再设置分词器 */
	private volatile boolean isIndexed = false;

	/**
	 * 若使用默认构造器，则索引基于内存
	 */
	public LuceneClient() {
		this.indexBase = new RAMDirectory();
	}

	/**
	 * 可接受文件系统的索引目录，也可以接受内存形式的索引目录
	 * 
	 * @param indexBase 索引目录
	 */
	public LuceneClient(Directory indexBase) {
		this.indexBase = indexBase;
	}

	/**
	 * 只接受文件系统的索引目录
	 * 
	 * @param indexBaseFSDirectory 文件系统的索引目录
	 * @throws IOException 来自底层的输入输出异常
	 */
	public LuceneClient(String indexBaseFSDirectory) throws IOException {
		this.indexBase = FSDirectory.open(Paths.get(indexBaseFSDirectory));
	}

	/**
	 * 将文档添加进索引
	 * 
	 * @param documents 要添加进索引的文档，执行后，每个Document中会添加id属性
	 * @return 被索引的文档数
	 * @throws IOException 来自底层的输入输出异常
	 */
	public int index(List<Document> documents) throws IOException {
		synchronized (this) {
			int numIndexed = 0;
			try {
				// queryCount == 0 表示既没查询，也没有索引在执行
				// queryCount < 0 表示在索引执行中
				// queryCount > 0 表示在有查询执行中
				while (queryCount != 0)
					wait();
				queryCount--;
				close();
				IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
				// 每一次都会进行创建新的索引,第二次删掉原来的创建新的索引
				indexWriterConfig.setOpenMode(OpenMode.CREATE);
				// 创建索引的indexWriter
				indexWriter = new IndexWriter(indexBase, indexWriterConfig);
				for (Document doc : documents) {
					doc.add(new LongField(ID_NAME, idCreator.getAndIncrement(), Store.YES));
					doc.add(new LongField(CREATE_TIME, System.currentTimeMillis(), Store.YES));
					for (IndexableField field : doc.getFields()) {
						indexableFieldNames.add(field.name());
					}
					indexWriter.addDocument(doc);
				}
				indexWriter.commit();
				numIndexed = indexWriter.numDocs();
				isIndexed = true;
				indexReader = DirectoryReader.open(indexWriter);
				indexSearcher = new IndexSearcher(indexReader);
			} catch (InterruptedException e) {
				LOG.catching(e);
			} finally {
				queryCount++;
				notifyAll();
			}
			return numIndexed;
		}
	}

	/**
	 * 添加一个文档进索引
	 * @param document 新增的文档，执行后，document中会添加id属性
	 * @return 新增文档ID_NAME Field中的值
	 * @throws IOException 来自底层的输入输出异常
	 */
	public long create(Document document) throws IOException {
		long id = idCreator.getAndIncrement();
		document.add(new LongField(ID_NAME, id, Store.YES));
		document.add(new LongField(CREATE_TIME, System.currentTimeMillis(), Store.YES));
		for (IndexableField field : document.getFields()) {
			indexableFieldNames.add(field.name());
		}
		indexWriter.addDocument(document);
		indexWriter.commit();
		refreshIndexReader();
		isIndexed = true;
		return id;
	}
	
	/**
	 * 根据ID_NAME Field中的值，获取文档
	 * @param id ID_NAME Field中的值，能唯一标识这个文档
	 * @return lucene中的文档，若未查找到，则返回null
	 */
	public Document read(long id) {
		Document doc = null;
		try {
			synchronized (this) {
				// queryCount == 0 表示既没查询，也没有索引正在执行
				// queryCount < 0 表示在索引执行中
				// queryCount > 0 表示在有查询执行中
				while (queryCount < 0)
					wait();
				queryCount++;
			}
			Query query = NumericRangeQuery.newLongRange(ID_NAME, id, id + 1, true, false);
			TopDocs docs = indexSearcher.search(query, 1);
			if (docs.scoreDocs.length == 0) {
				return null;
			}
			LOG.debug(docs.scoreDocs[0].score);
			doc = indexSearcher.doc(docs.scoreDocs[0].doc);
			LOG.debug(doc);
		} catch (IOException e) {
			LOG.error("Failed to open the index library", e);
		} catch (InterruptedException e) {
			LOG.catching(e);
		} finally {
			synchronized (this) {
				queryCount--;
				notifyAll();
			}
		}
		return doc;
	}

	/**
	 * 更新索引，现将原文档删除，然后再添加新文档
	 * 
	 * @param id ID_NAME Field中的值，能唯一标识这个文档
	 * @param document 更新的文档，执行后，document中会添加id属性
	 * @return 新增文档ID_NAME Field中的值
	 * @throws IOException 来自底层的输入输出异常
	 */
	public long update(long id, Document document) throws IOException {
		long newId = idCreator.getAndIncrement();
		document.add(new LongField(ID_NAME, newId, Store.YES));
		document.add(new LongField(CREATE_TIME, System.currentTimeMillis(), Store.YES));
		for (IndexableField field : document.getFields()) {
			indexableFieldNames.add(field.name());
		}
		indexWriter.updateDocument(new Term(ID_NAME, new BytesRef(getBytes(id))), document);
		indexWriter.commit();
		refreshIndexReader();
		isIndexed = true;
		return newId;
	}

	/**
	 * 在索引中删除一个文档
	 * 
	 * @param id ID_NAME Field中的值，能唯一标识这个文档
	 * @throws IOException 来自底层的输入输出异常
	 */
	public void delete(long id) throws IOException {
		Query query = NumericRangeQuery.newLongRange(ID_NAME, id, id + 1, true, false);
		indexWriter.deleteDocuments(query);
//		indexWriter.deleteDocuments(new Term(ID_NAME, new BytesRef(getBytes(id))));
		indexWriter.commit();
		refreshIndexReader();
	}
	
	/**
	 * 查询出Lucene原始的Document对象
	 * 
	 * @param queryString 查询字符串
	 * @return lucene的文档列表
	 */
	public List<Document> query(String queryString) {
		List<Document> list = new ArrayList<Document>();
		try {
			synchronized (this) {
				// queryCount == 0 表示既没查询，也没有索引正在执行
				// queryCount < 0 表示在索引执行中
				// queryCount > 0 表示在有查询执行中
				while (queryCount < 0)
					wait();
				queryCount++;
			}
			String[] fields = new String[indexableFieldNames.size()];
			QueryParser queryParser = new MultiFieldQueryParser(indexableFieldNames.toArray(fields), analyzer);
			Query query = queryParser.parse(queryString);
			TopDocs docs = indexSearcher.search(query, TOP_HITS);
			LOG.debug(docs.totalHits);
			for (ScoreDoc sd : docs.scoreDocs) {
				LOG.debug(sd.score);
				Document doc = indexSearcher.doc(sd.doc);
				LOG.debug(doc);
				list.add(doc);
			}
		} catch (IOException e) {
			LOG.error("Failed to open the index library", e);
		} catch (ParseException e) {
			LOG.error("Query statement parsing failed", e);
		} catch (InterruptedException e) {
			LOG.catching(e);
		} finally {
			synchronized (this) {
				queryCount--;
				notifyAll();
			}
		}
		return list;
	}

	/**
	 * 分页查询出Lucene原始的Document对象
	 * 
	 * @param queryString 查询语句
	 * @param pageable Spring-data的分页对象
	 * @return Spring-data的页面对象
	 */
	public Page<Document> query(String queryString, Pageable pageable) {
		List<Document> list = new ArrayList<Document>();
		int count = 0;
		try {
			synchronized (this) {
				// queryCount == 0 表示既没查询，也没有索引在执行
				// queryCount < 0 表示在索引执行中
				// queryCount > 0 表示在有查询执行中
				while (queryCount < 0)
					wait();
				queryCount++;
			}
			String[] fields = new String[indexableFieldNames.size()];
			QueryParser queryParser = new MultiFieldQueryParser(indexableFieldNames.toArray(fields), analyzer);
			Query query = queryParser.parse(queryString);
			count = indexSearcher.count(query);
			Sort sort = getSort(pageable);
			TopDocs docs = indexSearcher.search(query, TOP_HITS, sort);
			LOG.debug(docs.totalHits);
			int offset = (int) pageable.getOffset();
			int end = offset + pageable.getPageSize();

			for (int i = offset; i < end && i < count && i < TOP_HITS; i++) {
				ScoreDoc sd = docs.scoreDocs[i];
				LOG.debug(sd.score);
				Document doc = indexSearcher.doc(sd.doc);
				LOG.debug(doc);
				list.add(doc);
			}
		} catch (IOException e) {
			LOG.error("Failed to open the index library", e);
		} catch (ParseException e) {
			LOG.error("Query statement parsing failed", e);
		} catch (InterruptedException e) {
			LOG.catching(e);
		} finally {
			synchronized (this) {
				queryCount--;
				notifyAll();
			}
		}
		return new PageImpl<Document>(list, pageable, count);
	}

	/**
	 * 当索引变更时，为保持查询有效，需更新IndexReader
	 * 
	 * @throws IOException 来自底层的输入输出异常
	 */
	private void refreshIndexReader() throws IOException {
		synchronized (this) {
			try {
				// queryCount == 0 表示既没查询，也没有索引在执行
				// queryCount < 0 表示在索引执行中
				// queryCount > 0 表示在有查询执行中
				while (queryCount != 0)
					wait();
				queryCount--;
				IndexReader newReader = DirectoryReader.openIfChanged((DirectoryReader) indexReader);
				if (newReader != null && newReader != indexReader) {
					indexReader.close();
					indexReader = newReader;
					indexSearcher = new IndexSearcher(indexReader);
				}
			} catch (InterruptedException e) {
				LOG.catching(e);
			} finally {
				queryCount++;
				notifyAll();
			}
		}
	}
	
	private Sort getSort(Pageable pageable) {
		Sort sort = new Sort();
		List<SortField> ls = new ArrayList<SortField>();
		org.springframework.data.domain.Sort s = pageable.getSort();
		if (s != null) {
			for (Iterator<org.springframework.data.domain.Sort.Order> i = s.iterator(); i.hasNext();) {
				org.springframework.data.domain.Sort.Order o = i.next();
				SortField sortField = new SortField(o.getProperty(), Type.SCORE);// 以相关度进行排序
				ls.add(sortField);
			}
		}
		if (ls.size() > 0) {
			SortField[] sortFields = new SortField[ls.size()];
			sort.setSort(ls.toArray(sortFields));
		}
		return sort;
	}

	/**
	 * 设置分词器，必须在索引前调用，一旦索引完成，就不能再设置了
	 * 
	 * @param analyzer 分词器
	 */
	public synchronized void setAnalyzer(Analyzer analyzer) {
		if (isIndexed)
			throw new IllegalStateException("Has been indexed, can no longer set analyzer!");
		this.analyzer = analyzer;
	}

	/**
	 * 关闭所有资源
	 * 
	 * @throws IOException 来自底层的输入输出异常
	 */
	@Override
	public void close() throws IOException  {
		if (indexReader != null)
			indexReader.close();
		if (indexWriter != null && indexWriter.isOpen())
			indexWriter.close();
		isIndexed = false;
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}
	
	/**
	 * long值转成byte[]
	 * @param value long型的值
	 * @return byte数组
	 */
	public byte[] getBytes(long value) {  
        ByteBuffer buffer = ByteBuffer.allocate(8);  
        buffer.order(ByteOrder.BIG_ENDIAN);  
        buffer.putLong(value);  
        return buffer.array();  
    }  
}
