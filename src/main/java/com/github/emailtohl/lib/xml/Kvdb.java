package com.github.emailtohl.lib.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.github.emailtohl.lib.exception.InvalidDataException;

/**
 * key value本地存储库，使用场景主要是用于收集redis的数据以便于mock掉redis接口
 * 
 * @author HeLei
 */
public class Kvdb implements Serializable {
	/**
	 * 序列化id
	 */
	private static final long serialVersionUID = 1105747046663001195L;
	/**
	 * ConcurrentHashMap不能存储value为null的值，所以用特殊字符替代
	 */
	private static final String NULL = "_null_";
	/**
	 * xml元素的名字，存储redis的字符串类型
	 */
	private static final String tagString = "string";
	/**
	 * xml元素的名字，存储redis的散列类型
	 */
	private static final String tagHash = "hash";
	/**
	 * xml元素的名字，存储redis的集合类型
	 */
	private static final String tagSet = "set";
	/**
	 * xml元素的名字，存储redis的列表类型
	 */
	private static final String tagArray = "array";
	/**
	 * xml元素的名字，存储每个键值对的元素
	 */
	private static final String tagEntry = "entry";
	/**
	 * xml元素的名字，存储key的元素
	 */
	private static final String tagKey = "key";
	/**
	 * xml元素的名字，存储value的元素
	 */
	private static final String tagValue = "value";
	/**
	 * xml元素的名字，存储多个value的元素
	 */
	private static final String tagValues = "values";

	/**
	 * xml配置文件工厂
	 */
	private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private transient String path;
	/**
	 * redis的字符串类型
	 */
	private ConcurrentHashMap<String, String> string = new ConcurrentHashMap<String, String>();
	/**
	 * redis的散列类型
	 */
	private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> hash = new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();
	/**
	 * redis的列表类型
	 */
	private ConcurrentHashMap<String, CopyOnWriteArrayList<String>> array = new ConcurrentHashMap<String, CopyOnWriteArrayList<String>>();
	/**
	 * redis的集合类型
	 */
	private ConcurrentHashMap<String, CopyOnWriteArraySet<String>> set = new ConcurrentHashMap<String, CopyOnWriteArraySet<String>>();

	/**
	 * 构造KVLocalStore实例，序列化的内容将保存在参数指定的文件中
	 * 
	 * @param path 指定保存的文件
	 */
	public Kvdb(String path) {
		this.path = path;
		File f = new File(path);
		if (!f.exists()) {
			saveToFile();
		} else {
			load(path);
		}
	}

	/**
	 * 保存字符串类型的值
	 * 
	 * @param key   键
	 * @param value 字符串值
	 */
	public void saveString(String key, String value) {
		string.put(key, value == null ? NULL : value);
		saveToFile();
	}

	/**
	 * 读取字符串类型的值
	 * 
	 * @param key 键
	 * @return 字符串值
	 */
	public String readString(String key) {
		String value = string.get(key);
		return NULL.equals(value) ? null : value;
	}

	/**
	 * 删除字符串类型的值
	 * 
	 * @param key 键
	 */
	public void delString(String key) {
		string.remove(key);
		saveToFile();
	}

	/**
	 * 保存hash类型的值
	 * 
	 * @param key     键
	 * @param hashKey 散列键
	 * @param value   字符串值
	 */
	public void saveHash(String key, String hashKey, String value) {
		ConcurrentHashMap<String, String> hashValue = hash.get(key);
		if (hashValue == null) {
			hashValue = new ConcurrentHashMap<String, String>();
			ConcurrentHashMap<String, String> previous = hash.putIfAbsent(key, hashValue);
			hashValue = previous == null ? hashValue : previous;
		}
		hashValue.put(hashKey, value == null ? NULL : value);
		saveToFile();
	}

	/**
	 * 读取hash类型的值
	 * 
	 * @param key     键
	 * @param hashKey 散列键
	 * @return 字符串值
	 */
	public String readHash(String key, String hashKey) {
		ConcurrentHashMap<String, String> hashValue = hash.get(key);
		if (hashValue == null) {
			return null;
		}
		String value = hashValue.get(hashKey);
		return NULL.equals(value) ? null : value;
	}

	/**
	 * 删除hash类型的值
	 * 
	 * @param key     键
	 * @param hashKey 散列键
	 */
	public void delHash(String key, String hashKey) {
		ConcurrentHashMap<String, String> hashValue = hash.get(key);
		if (hashValue != null) {
			// the previous value associated with key, or null if there was no mapping for key
			String res = hashValue.remove(hashKey);
			if (res != null) {
				saveToFile();
			}
		}
	}

	/**
	 * 保存整个散列值
	 * 
	 * @param key  键
	 * @param hash 散列值
	 */
	public void saveHashAll(String key, Map<String, String> hash) {
		ConcurrentHashMap<String, String> hashValue = new ConcurrentHashMap<String, String>(hash);
		this.hash.put(key, hashValue);
		saveToFile();
	}

	/**
	 * 读取整个散列值
	 * 
	 * @param key 键
	 * @return 整个键对应的散列值
	 */
	public Map<String, String> readHashAll(String key) {
		Map<String, String> value = hash.get(key);
		return value == null ? new HashMap<String, String>() : value;
	}

	/**
	 * 将数据保存到集合中
	 * 
	 * @param key   键
	 * @param value 字符串值
	 */
	public void saveSet(String key, String value) {
		CopyOnWriteArraySet<String> setValue = set.get(key);
		if (setValue == null) {
			setValue = new CopyOnWriteArraySet<String>();
			CopyOnWriteArraySet<String> previous = set.putIfAbsent(key, setValue);
			setValue = previous == null ? setValue : previous;
		}
		setValue.add(value);
		saveToFile();
	}

	/**
	 * 保存整个集合
	 * 
	 * @param key   键
	 * @param value 集合值
	 */
	public void saveSetAll(String key, Set<String> value) {
		CopyOnWriteArraySet<String> setValue = new CopyOnWriteArraySet<String>(value);
		this.set.put(key, setValue);
		saveToFile();
	}

	/**
	 * 读取集合
	 * 
	 * @param key 键
	 * @return 键对应的整个集合
	 */
	public Set<String> readSet(String key) {
		Set<String> value = set.get(key);
		return value == null ? new HashSet<String>() : value;
	}

	/**
	 * 将数据插入列表右边
	 * 
	 * @param key   键
	 * @param value 字符串值
	 */
	public void rightPushList(String key, String value) {
		CopyOnWriteArrayList<String> arrayValue = array.get(key);
		if (arrayValue == null) {
			arrayValue = new CopyOnWriteArrayList<String>();
			CopyOnWriteArrayList<String> previous = array.putIfAbsent(key, arrayValue);
			arrayValue = previous == null ? arrayValue : previous;
		}
		arrayValue.add(value);
		saveToFile();
	}

	/**
	 * 读取列表
	 * 
	 * @param key 键
	 * @return 键对应的整个列表
	 */
	public List<String> readList(String key) {
		List<String> value = array.get(key);
		return value == null ? new ArrayList<String>() : value;
	}

	/**
	 * 将本类实例构造成一个DOM树
	 * @return DOM结构的Document
	 */
	private Document buildDoc() {
		DocumentBuilder b;
		try {
			b = factory.newDocumentBuilder();
			Document document = b.newDocument();
			Element eroot = document.createElement(getClass().getSimpleName());
			document.appendChild(eroot);
			Element estring = document.createElement(tagString);
			Element ehash = document.createElement(tagHash);
			Element eset = document.createElement(tagSet);
			Element earray = document.createElement(tagArray);
			eroot.appendChild(estring);
			eroot.appendChild(ehash);
			eroot.appendChild(eset);
			eroot.appendChild(earray);
			for (Entry<String, String> entry : string.entrySet()) {
				estring.appendChild(createEntry(document, entry.getKey(), entry.getValue()));
			}
			for (Entry<String, ConcurrentHashMap<String, String>> mapEntry : hash.entrySet()) {
				Element entryEle = document.createElement(tagEntry);
				ehash.appendChild(entryEle);
				Element keyEle = document.createElement(tagKey);
				entryEle.appendChild(keyEle);
				Text keyText = document.createTextNode(mapEntry.getKey());
				keyEle.appendChild(keyText);
				Element valuesEle = document.createElement(tagValues);
				entryEle.appendChild(valuesEle);
				for (Entry<String, String> entry : mapEntry.getValue().entrySet()) {
					valuesEle.appendChild(createEntry(document, entry.getKey(), entry.getValue()));
				}
			}
			for (Entry<String, CopyOnWriteArraySet<String>> setEntry : set.entrySet()) {
				Element entryEle = document.createElement(tagEntry);
				eset.appendChild(entryEle);
				Element keyEle = document.createElement(tagKey);
				entryEle.appendChild(keyEle);
				Text keyText = document.createTextNode(setEntry.getKey());
				keyEle.appendChild(keyText);
				Element valuesEle = document.createElement(tagValues);
				entryEle.appendChild(valuesEle);
				for (String item : setEntry.getValue()) {
					Element valueEle = document.createElement(tagValue);
					valuesEle.appendChild(valueEle);
					Text valueText = document.createTextNode(item);
					valueEle.appendChild(valueText);
				}
			}
			for (Entry<String, CopyOnWriteArrayList<String>> arrayEntry : array.entrySet()) {
				Element entryEle = document.createElement(tagEntry);
				earray.appendChild(entryEle);
				Element keyEle = document.createElement(tagKey);
				entryEle.appendChild(keyEle);
				Text keyText = document.createTextNode(arrayEntry.getKey());
				keyEle.appendChild(keyText);
				Element valuesEle = document.createElement(tagValues);
				entryEle.appendChild(valuesEle);
				for (String item : arrayEntry.getValue()) {
					Element valueEle = document.createElement(tagValue);
					valuesEle.appendChild(valueEle);
					Text valueText = document.createTextNode(item);
					valueEle.appendChild(valueText);
				}
			}
			return document;
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * 将本类实例的数据写入本地文件中
	 */
	private void saveToFile() {
		Document document = buildDoc();
		synchronized (Kvdb.class) {
			try {
				DOMImplementation impl = document.getImplementation();
				DOMImplementationLS ls = (DOMImplementationLS) impl.getFeature("LS", "3.0");
				LSSerializer seri = ls.createLSSerializer();
				seri.getDomConfig().setParameter("format-pretty-print", true);
				LSOutput out = ls.createLSOutput();
				out.setEncoding("UTF-8");
				File f = availableFile(path);
				FileOutputStream stream = new FileOutputStream(f);
				out.setByteStream(stream);
				seri.write(document, out);
				stream.close();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}
	
	/**
	 * 获取可用的输出文件
	 * 
	 * @param path 文件路径
	 * @return 可写入的文件
	 * @throws IOException 写入异常
	 */
	private File availableFile(String path) throws IOException {
		File f = new File(path);
		File dir = f.getParentFile();
		boolean ok = true;
		if (!dir.exists()) {
			ok = dir.mkdirs();
		}
		if (!ok) {
			throw new IOException("Failed to create data store directory:" + dir.getAbsolutePath());
		}
		if (!f.exists()) {
			ok = f.createNewFile();
		}
		if (!ok) {
			throw new IOException("Failed to create data store file:" + f.getAbsolutePath());
		}
		return f;
	}
	
	/**
	 * 根据路径加载数据到本类实例中
	 * @param path 数据存储的文件
	 */
	private void load(String path) {
		try {
			DocumentBuilder b = factory.newDocumentBuilder();
			FileInputStream in = null;
			Document document = null;
			try {
				in = new FileInputStream(path);
				document = b.parse(in);
			} catch (SAXParseException saxpe) {
				saveToFile();
				in = new FileInputStream(path);
				document = b.parse(in);
			} finally {
				if (in != null) {
					in.close();
				}
			}
			Element root = document.getDocumentElement();
			NodeList nodeList = root.getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node typeNode = nodeList.item(i);
				if (tagString.equals(typeNode.getNodeName())) {
					NodeList stringNodes = typeNode.getChildNodes();
					for (int j = 0; j < stringNodes.getLength(); j++) {
						Node stringEntry = stringNodes.item(j);
						if (stringEntry instanceof Element) {
							String[] kv = getEntryKeyValue((Element) stringEntry);
							string.put(kv[0], kv[1]);
						}
					}
				} else if (tagHash.equals(typeNode.getNodeName())) {
					NodeList entries = typeNode.getChildNodes();
					for (int j = 0; j < entries.getLength(); j++) {
						Node entry = entries.item(j);
						if (!(entry instanceof Element)) {
							continue;
						}
						Element[] ekv = getKeyValuesElements((Element) entry);
						if (ekv[0] != null && ekv[1] != null) {
							String hashKey = ekv[0].getTextContent();
							ConcurrentHashMap<String, String> hashValue = hash.get(hashKey);
							if (hashValue == null) {
								hashValue = new ConcurrentHashMap<String, String>();
								ConcurrentHashMap<String, String> previous = hash.putIfAbsent(hashKey, hashValue);
								hashValue = previous == null ? hashValue : previous;
							}
							NodeList items = ekv[1].getChildNodes();
							for (int k = 0; k < items.getLength(); k++) {
								Node item = items.item(k);
								if (item instanceof Element) {
									String[] kv = getEntryKeyValue((Element) item);
									hashValue.put(kv[0], kv[1]);
								}
							}
						}
					}
				} else if (tagSet.equals(typeNode.getNodeName())) {
					NodeList entries = typeNode.getChildNodes();
					for (int j = 0; j < entries.getLength(); j++) {
						Node entry = entries.item(j);
						if (!(entry instanceof Element)) {
							continue;
						}
						Element[] ekv = getKeyValuesElements((Element) entry);
						if (ekv[0] != null && ekv[1] != null) {
							String setKey = ekv[0].getTextContent();
							CopyOnWriteArraySet<String> setValue = set.get(setKey);
							if (setValue == null) {
								setValue = new CopyOnWriteArraySet<String>();
								CopyOnWriteArraySet<String> previous = set.putIfAbsent(setKey, setValue);
								setValue = previous == null ? setValue : previous;
							}
							NodeList valuesEle = ekv[1].getChildNodes();
							for (int k = 0; k < valuesEle.getLength(); k++) {
								Node valueEle = valuesEle.item(k);
								if (valueEle instanceof Element) {
									setValue.add(valueEle.getTextContent());
								}
							}
						}
					}
				} else if (tagArray.equals(typeNode.getNodeName())) {
					NodeList entries = typeNode.getChildNodes();
					for (int j = 0; j < entries.getLength(); j++) {
						Node entry = entries.item(j);
						if (!(entry instanceof Element)) {
							continue;
						}
						Element[] ekv = getKeyValuesElements((Element) entry);
						if (ekv[0] != null && ekv[1] != null) {
							String setKey = ekv[0].getTextContent();
							CopyOnWriteArrayList<String> arrayValue = array.get(setKey);
							if (arrayValue == null) {
								arrayValue = new CopyOnWriteArrayList<String>();
								CopyOnWriteArrayList<String> previous = array.putIfAbsent(setKey, arrayValue);
								arrayValue = previous == null ? arrayValue : previous;
							}
							NodeList valuesEle = ekv[1].getChildNodes();
							for (int k = 0; k < valuesEle.getLength(); k++) {
								Node valueEle = valuesEle.item(k);
								if (valueEle instanceof Element) {
									arrayValue.add(valueEle.getTextContent());
								}
							}
						}
					}
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new InvalidDataException(e);
		}
	}

	/**
	 * 创建一个键值对的dom元素
	 * 
	 * @param document 用于创建元素
	 * @param key 键
	 * @param value 字符串值
	 * @return 一个entry元素，包含key元素和value元素
	 */
	private Element createEntry(Document document, String key, String value) {
		Element entryEle = document.createElement(tagEntry);
		Element keyEle = document.createElement(tagKey);
		entryEle.appendChild(keyEle);
		Element valueEle = document.createElement(tagValue);
		entryEle.appendChild(valueEle);
		Text keyText = document.createTextNode(key);
		keyEle.appendChild(keyText);
		Text valueText = document.createTextNode(value);
		valueEle.appendChild(valueText);
		return entryEle;
	}

	/**
	 * 获取一个键值对
	 * 
	 * @param entry 从entry元素中获取key value
	 * @return key value 的数组
	 */
	private String[] getEntryKeyValue(Element entry) {
		String[] kv = new String[2];
		NodeList nl = entry.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n instanceof Element) {
				Element nn = (Element) n;
				if (tagKey.equals(nn.getTagName())) {
					kv[0] = nn.getTextContent();
				} else if (tagValue.equals(nn.getTagName())) {
					kv[1] = nn.getTextContent();
				}
			}
		}
		return kv;
	}

	/**
	 * 查找这个元素下的key，values元素
	 * 
	 * @param entry entry元素
	 * @return key元素和values元素
	 */
	private Element[] getKeyValuesElements(Element entry) {
		Element[] kv = new Element[2];
		NodeList nl = entry.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n instanceof Element) {
				Element nn = (Element) n;
				if (tagKey.equals(nn.getTagName())) {
					kv[0] = nn;
				} else if (tagValues.equals(nn.getTagName())) {
					kv[1] = nn;
				}
			}
		}
		return kv;
	}

	/**
	 * 覆盖toString方法，以xml格式输出
	 */
	@Override
	public String toString() {
		Document document = buildDoc();
		DOMImplementation impl = document.getImplementation();
		DOMImplementationLS ls = (DOMImplementationLS) impl.getFeature("LS", "3.0");
		LSSerializer seri = ls.createLSSerializer();
		seri.getDomConfig().setParameter("format-pretty-print", true);
		return seri.writeToString(document);
	}
}
