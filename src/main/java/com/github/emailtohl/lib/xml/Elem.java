package com.github.emailtohl.lib.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * 简易的xml元素数据模型，但满足自定义的equals hashcode，可在容器中识别
 * 
 * @author helei
 *
 */
public class Elem {
	private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private static final Logger log = LogManager.getLogger(Elem.class);
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
	 * @param xml string文本
	 * @throws ParserConfigurationException if a DocumentBuilder cannot be created
	 *                                      which satisfies the configuration
	 *                                      requested
	 * @throws IOException                  If any IO errors occur
	 * @throws SAXException                 If any parse errors occur
	 */
	public Elem(String xml) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
		Document document = builder.parse(inputStream);
		Element element = document.getDocumentElement();
		this.name = element.getNodeName();
		fillRoot(element);
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
		if (log.isDebugEnabled()) {
			log.debug(this);
		}
	}

	/**
	 * 将org.w3c.dom.Element子节点的数据填充到本类实例对应的子节点中
	 * 
	 * @param element org.w3c.dom.Element
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
				if (log.isTraceEnabled()) {
					log.trace(node.getTextContent());
				}
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attrs == null) ? 0 : attrs.hashCode());
		result = prime * result + ((children == null) ? 0 : children.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		if (attrs == null) {
			if (other.attrs != null)
				return false;
		} else if (!attrs.equals(other.attrs))
			return false;
		if (children == null) {
			if (other.children != null)
				return false;
		} else if (!children.containsAll(other.children) || !other.children.containsAll(children))
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
		return "Ele [name=" + name + ", children=" + children + ", attrs=" + attrs + "]";
	}

}
