package com.github.emailtohl.lib.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.emailtohl.lib.exception.InnerDataStateException;

/**
 * 自定义的xml元素数据模型，但满足自定义的equals hashcode，可在容器中识别
 * 使用场景，mock 以xml作为输入输出的接口：
 * 首先将预先收集的输入xml和输出xml收集起来；
 * 然后将输入的xml构造成本类的实例；
 * 最后将该实例和输出xml存储在Map&lt;Elem, String&gt;中，这样就能mock掉真实的接口
 * 
 * @author helei
 *
 */
public class Elem {
	private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private static final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
	/**
	 * 节点名字
	 */
	public final String name;
	/**
	 * 子节点
	 */
	public final List<Elem> children = new ArrayList<Elem>();
	/**
	 * 该元素上的属性
	 */
	public final Attrs attrs = new Attrs();
	/**
	 * 元素的文本集合
	 */
	public final List<String> texts = new ArrayList<String>();

	/**
	 * 从Element中转成本类实例
	 * 
	 * @param element org.w3c.dom.Element
	 */
	public Elem(Element element) {
		this.name = element.getNodeName();
		fillRoot(element);
	}

	/**
	 * 将xml解析为自定义元素数据结构
	 * 
	 * @param xmlContent xml的文本
	 * @throws SAXException If any parse errors occur
	 */
	public Elem(String xmlContent) throws SAXException {
		Element element = getElement(xmlContent);
		this.name = element.getNodeName();
		fillRoot(element);
	}

	/**
	 * 解析xml文本获取元素，屏蔽不可能发生的异常
	 * 
	 * @param xmlContent xml文本
	 * @return 解析的xml元素实例
	 * @throws SAXException If any parse errors occur
	 */
	private Element getElement(String xmlContent) throws SAXException {
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes());
			Document document = builder.parse(inputStream);
			return document.getDocumentElement();
		} catch (ParserConfigurationException e) {
			throw new InnerDataStateException(e);
		} catch (IOException e) {
			throw new InnerDataStateException(e);
		}
	}

	/**
	 * 将org.w3c.dom.Element的信息填充到本类实例中
	 * 
	 * @param element org.w3c.dom.Element
	 */
	private void fillRoot(Element element) {
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
		result = prime * result + (children.size() == 0 ? 0 : childreHashCode());
		result = prime * result + (StringUtils.hasText(name) ? 0 : name.hashCode());
		result = prime * result + (texts.size() == 0 ? 0 : textHashCode());
		return result;
	}

	private int childreHashCode() {
		final int prime = 31;
		int result = 1;
		for (Elem e : children) {
			result = prime * result + e.hashCode();
		}
		return result;
	}
	
	private int textHashCode() {
		final int prime = 31;
		int result = 1;
		for (String s : texts) {
			result = prime * result + s.hashCode();
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
		try {
			return writer.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

}
