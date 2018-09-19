package com.github.emailtohl.lib.lucene;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.store.Directory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.github.emailtohl.lib.util.TextUtil;

/**
 * 对磁盘文件系统中的文本文件进行索引和查询
 * 
 * @author HeLei
 */
public class FileSearch implements AutoCloseable {
	public static final String FILE_NAME = "fileName";
	public static final String FILE_TIME = "fileTime";
	public static final String FILE_CONTENT = "fileContent";
	public static final String FILE_PATH = "filePath";
	public static final String FILE_SIZE = "fileSize";
	public static final int TOP_HITS = 1000;
	private LuceneClient client;
	
	/** 文本文件过滤器 */
	private FileFilter textFileFilter = new TextFilesFilter();

	/**
	 * 可接受文件系统的索引目录，也可以接受内存形式的索引目录
	 * 
	 * @param indexBase
	 *            索引目录
	 * @throws IOException 
	 */
	public FileSearch(Directory indexBase) throws IOException {
		client = new LuceneClient(indexBase);
	}

	/**
	 * 只接受文件系统的索引目录
	 * 
	 * @param indexBaseFSDirectory
	 *            文件系统的索引目录
	 * @throws IOException
	 *             来自底层的输入输出异常
	 */
	public FileSearch(String indexBaseFSDirectory) throws IOException {
		client = new LuceneClient(indexBaseFSDirectory);
	}

	/**
	 * 为需要查询的目录创建索引
	 * 
	 * @param searchDir 需要查询的目录
	 * @return 被索引的Document数
	 * @throws IOException 来自底层的输入输出异常
	 */
	public synchronized void index(File searchDir) throws IOException {
		client.index(getDocuments(searchDir));
	}

	/**
	 * 分析文本文件，并创建一个Lucene的Document
	 * 
	 * @param file 文件系统中的文档
	 * @return Lucene中的文档
	 * @throws IOException 来自底层的输入输出异常
	 */
	private Document getDocument(File file) throws IOException {
		// String content = FileUtils.readFileToString(file,
		// StandardCharsets.UTF_8);
		FileInputStream fis = null;
		String content = "";
		try {
			fis = new FileInputStream(file);
			content = TextUtil.readFileToString(fis);
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
		// TextField既被索引又被分词，但是没有词向量
		Field fName = new TextField(FILE_NAME, file.getName(), Store.YES);
		fName.setBoost(1.2F);
		Field fContent = new TextField(FILE_CONTENT, content, Store.NO);
		// StringField被索引不被分词，整个值被看作为一个单独的token而被索引
		// 这里使用file.getCanonicalPath()，获取的是文件绝对路径，getCanonicalPath和getAbsolutePath的不同在于：
		// File file = new File("..\\src\\test1.txt");
		// System.out.println(file.getAbsolutePath());
		// D:\workspace\test\..\src\test1.txt
		// System.out.println(file.getCanonicalPath());
		// D:\workspace\src\test1.txt
		Field fPath = new StringField(FILE_PATH, file.getCanonicalPath(), Store.YES);
		// 创建文档对象
		Document doc = new Document();
		doc.add(fName);
		doc.add(fContent);
		doc.add(fPath);
		return doc;
	}
	
	/**
	 * 将目录下的所有文本文件分析成Document并返回
	 * @param path 文件系统中的目录
	 * @return Document集合
	 * @throws IOException 来自底层的输入输出异常
	 */
	private List<Document> getDocuments(File path) throws IOException {
		List<Document> docs = new ArrayList<Document>();
		if (textFileFilter.accept(path)) {
			docs.add(getDocument(path));
		} else if (path.isDirectory()) {
			for (File sub : path.listFiles()) {
				docs.addAll(getDocuments(sub));
			}
		}
		return docs;
	}
	
	/**
	 * 查询出Lucene原始的Document对象
	 * 
	 * @param queryString 查询字符串
	 * @return lucene文档列表
	 */
	public List<Document> query(String queryString) {
		return client.search(queryString);
	}

	/**
	 * 分页查询出Lucene原始的Document对象
	 * 
	 * @param queryString 查询语句
	 * @param pageable Spring-data的分页对象
	 * @return Spring-data的页面对象
	 */
	public Page<Document> query(String queryString, Pageable pageable) {
		LuceneClient.Fragment fragment = client.search(queryString, (int) pageable.getOffset(), pageable.getPageSize());
		return new PageImpl<Document>(fragment.documents, pageable, fragment.totalHits);
	}

	/**
	 * 添加文件的索引
	 * 
	 * @param file 文件系统中的文档
	 * @return 创建文档的id
	 * @throws IOException 来自底层的输入输出异常
	 */
	public String addIndex(File file) throws IOException {
		if (!textFileFilter.accept(file)) {
			return "";
		}
		Document doc = getDocument(file);
		return client.create(doc);
	}

	/**
	 * 更新文件的索引
	 * 
	 * @param file 文件系统中的文档
	 * @throws IOException 来自底层的输入输出异常
	 */
	public String updateIndex(File file) throws IOException {
		if (!textFileFilter.accept(file)) {
			return "";
		}
		Document document = getDocument(file);
		List<Document> ls = client.search(file.getPath());
		String id;
		if (ls.isEmpty()) {
			id = client.create(document);
		} else {
			id = client.update(ls.get(0).get(LuceneClient.ID_NAME), document);
		}
		return id;
	}

	/**
	 * 删除文件的索引
	 * 
	 * @param file
	 *            文件系统中的文档
	 * @throws IOException
	 *             来自底层的输入输出异常
	 */
	public void deleteIndex(File file) throws IOException {
		if (textFileFilter.accept(file)) {
			List<Document> ls = client.search(file.getPath());
			if (!ls.isEmpty()) {
				client.delete(ls.get(0).get(LuceneClient.ID_NAME));
			}
		}
	}

	/**
	 * 将查询结果以文件的路径返回
	 * 
	 * @param queryString 查询关键字
	 * @return 返回的路径是相对于index时的路径，若index时是绝对路径，则返回的也是绝对路径
	 */
	public Set<String> queryForFilePath(String queryString) {
		Set<String> paths = new TreeSet<String>();
		List<Document> list = query(queryString);
		for (Document doc : list) {
			paths.add(doc.getField(FILE_PATH).stringValue());
		}
		return paths;
	}

	@Override
	public void close() throws Exception {
		client.close();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}

	/**
	 * 将包名转成目录名
	 * 
	 * @param packageName Java的包名
	 * @return 文件系统中的路径
	 */
	public static String convertPackageNameToFilePath(String packageName) {
		String replacement;
		if (File.separator.equals("\\"))
			replacement = "\\\\";
		else
			replacement = "/";
		return packageName.replaceAll("\\.", replacement);
	}

	/**
	 * 根据后缀过滤一部分不是文本的文件
	 * 
	 * @author HeLei
	 */
	class TextFilesFilter implements FileFilter {
		private static final long MAX_BYTES = 10_485_760L;// 10兆
		private final Set<String> SUFFIX_SET = new HashSet<String>(Arrays.asList("dll", "jpg", "png", "gif", "tif",
				"bmp", "dwg", "psd", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "mdb", "wpd", "zip", "gz", "rar",
				"wav", "avi", "ram", "rm", "mpg", "mpq", "mov", "asf", "mid", "exe", "wps"));

		@Override
		public boolean accept(File f) {
			if (f.isDirectory() || f.length() > MAX_BYTES)
				return false;
			String ext = FilenameUtils.getExtension(f.getName());
			return !SUFFIX_SET.contains(ext.toLowerCase());
		}
	}

}
