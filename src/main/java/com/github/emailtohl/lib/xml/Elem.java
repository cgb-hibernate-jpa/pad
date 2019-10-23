package com.github.emailtohl.lib.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.util.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.github.emailtohl.lib.exception.InnerDataStateException;

/**
 * 自定义的xml元素数据模型，但满足自定义的equals hashcode，可在容器中识别 使用场景，mock 以xml作为输入输出的接口：
 * 首先将预先收集的输入xml和输出xml收集起来； 然后将输入的xml构造成本类的实例； 最后将该实例和输出xml存储在Map&lt;Elem,
 * String&gt;中，这样就能mock掉真实的接口
 * 
 * @author helei
 *
 */
public class Elem {
	private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	/**
	 * 节点名字
	 */
	public final String name;
	/**
	 * 该元素上的属性
	 */
	public final Attrs attrs = new Attrs();
	/**
	 * 元素的文本集合
	 */
	public final List<String> texts = new ArrayList<String>();
	/**
	 * 子节点
	 */
	public final List<Elem> children = new ArrayList<Elem>();

	/**
	 * 从Element中转成本类实例
	 * 
	 * @param element org.w3c.dom.Element
	 */
	public Elem(Element element) {
		this.name = element.getNodeName();
		fillSelf(element);
	}

	/**
	 * 将xml解析为自定义元素数据结构
	 * 
	 * @param xmlContent xml的文本
	 * @throws SAXException If any parse errors occur
	 */
	public Elem(String xmlContent) throws SAXException {
		Element element = getElement(xmlContent).getDocumentElement();
		this.name = element.getNodeName();
		fillSelf(element);
	}

	/**
	 * 将org.w3c.dom.Element的信息填充到本类实例中
	 * 
	 * @param element org.w3c.dom.Element
	 */
	private void fillSelf(Element element) {
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node n = attributes.item(i);
			if (n instanceof Attr) {
				Attr a = (Attr) n;
				this.attrs.put(a.getNodeName(), a.getNodeValue());
			}
		}
		this.fillChildren(element.getChildNodes());
	}

	/**
	 * 将org.w3c.dom.Element子节点的数据填充到本类实例对应的子节点中
	 * 
	 * @param nodeList org.w3c.dom.Element
	 */
	private void fillChildren(NodeList nodeList) {
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node instanceof Element) {
				Element element = (Element) node;
				Elem el = new Elem(element);
				this.children.add(el);
			} else if (node instanceof Attr) {
				Attr attr = (Attr) node;
				this.attrs.put(attr.getNodeName(), attr.getNodeValue());
			} else if (node instanceof Text) {
				String content = node.getTextContent();
				if (StringUtils.hasText(content)) {
					this.texts.add(content);
				}
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (attrs.size() == 0 ? 0 : attrs.hashCode());
		result = prime * result + (children.size() == 0 ? 0 : childrenHashCode());
		result = prime * result + (StringUtils.hasText(name) ? 0 : name.hashCode());
		result = prime * result + (texts.size() == 0 ? 0 : textHashCode());
		return result;
	}

	private int childrenHashCode() {
		int result = 1;
		for (Elem e : children) {
			result = result + e.hashCode();
		}
		return result;
	}

	private int textHashCode() {
		int result = 1;
		for (String s : texts) {
			result = result + s.hashCode();
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Elem other = (Elem) obj;
		if (!attrs.equals(other.attrs))
			return false;
		if (!children.containsAll(other.children) || !other.children.containsAll(children))
			return false;
		if (!texts.containsAll(other.texts) || !other.texts.containsAll(texts))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append('\n').append('<').append(name).append(attrString()).append('>');
		// 将文本和元素的顺序交替写入数组中，以便于更符合实际xml的顺序
		Object[] arr = alternateTextsAndElements();
		for (Object o : arr) {
			s.append(o.toString());
		}
		s.append('\n').append('<').append('/').append(name).append('>');
		return s.toString();
	}

	/**
	 * 将元素属性值创建成一段字符串
	 * 
	 * @return 属性的字符串
	 */
	private String attrString() {
		if (attrs.isEmpty()) {
			return "";
		}
		StringBuilder s = new StringBuilder();
		for (Entry<String, String> e : attrs.entrySet()) {
			s.append(' ').append(e.getKey()).append('=').append('"').append(e.getValue()).append('"');
		}
		return s.toString();
	}

	/**
	 * 将文本和元素的顺序交替写入数组中，以便于更符合实际xml的顺序
	 * 
	 * @return 文本和元素交替顺序的数组
	 */
	private Object[] alternateTextsAndElements() {
		Object[] arr = new Object[texts.size() + children.size()];
		Iterator<String> itext = texts.iterator();
		Iterator<Elem> ichildren = children.iterator();
		int i = 0;
		while (itext.hasNext() && ichildren.hasNext()) {
			arr[i++] = itext.next();
			arr[i++] = ichildren.next();
		}
		while (itext.hasNext()) {
			arr[i++] = itext.next();
		}
		while (ichildren.hasNext()) {
			arr[i++] = ichildren.next();
		}
		return arr;
	}

	/**
	 * 解析xml文本获取元素，屏蔽不可能发生的异常
	 * 
	 * @param xmlContent xml文本
	 * @return 解析的xml元素实例
	 * @throws SAXException If any parse errors occur
	 */
	private Document getElement(String xmlContent) throws SAXException {
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes());
			return builder.parse(inputStream);
		} catch (ParserConfigurationException | IOException e) {
			throw new InnerDataStateException(e);
		}
	}
}
