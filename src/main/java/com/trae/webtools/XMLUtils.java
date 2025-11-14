package com.trae.webtools;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * XML 工具：解析、序列化、节点查询与修改
 *
 * XML 简介：
 * - XML（Extensible Markup Language，可扩展标记语言）是一种基于文本的结构化数据格式，
 *   通过标签（elements）、属性（attributes）与文本节点（text）构成层次树形结构。
 * - 基本规则：必须有且仅有一个根元素；标签大小写敏感；元素必须正确嵌套；建议声明编码与 XML 版本。
 * - 命名空间（Namespaces）：使用 URI 标识语义域并通过前缀绑定（如 ns:tag）避免元素/属性名冲突。
 * - 约束与校验：可使用 DTD 或 XSD（XML Schema）定义结构与类型；“格式良好”（well-formed）与“有效”（valid）是不同层次。
 * - 优势：自描述、可读性强、生态工具丰富（XPath/XSLT/XQuery）、适合文档与配置；
 *   局限：冗长、解析开销大、二进制需编码（如 Base64），在轻量数据交换场景常用 JSON 替代。
 * - 常见应用：SOAP/WSDL、Office Open XML、SVG、许多配置与文档格式；Web API 场景多倾向使用 JSON。
 * - 解析模型：DOM 构建完整树（适合中小文档、便于随机访问与修改）；超大文档建议使用 SAX/StAX 流式处理。
 *
 * 功能概览：
 * - 解析：将 XML 字符串解析为 W3C DOM {@link org.w3c.dom.Document}
 * - 序列化：将 {@link org.w3c.dom.Document} 输出为紧凑或美化后的字符串
 * - 节点操作：按标签获取/统计元素、读写文本与属性、添加/移除子元素、导入节点等
 *
 * 模型说明：
 * - 采用 JDK 自带的 DOM API（W3C 标准接口）进行文档构建与遍历，适合中小规模 XML
 * - 命名空间：解析时启用 {@code setNamespaceAware(true)}，便于处理带前缀的元素
 *
 * 安全提示（生产实践）：
 * - 为防止 XXE（XML 外部实体注入）与 DTD 攻击，解析时应关闭外部实体访问与 DTD；
 *   本工具为轻量示例，未默认设置这些安全特性，建议在调用方构造 DocumentBuilderFactory 时进行安全配置。
 * - 输出时默认包含 XML 声明（OMIT_XML_DECLARATION=no），如需移除可自行设置 transformer 属性。
 *
 * 典型用法：
 * <pre>
 * Document doc = XMLUtils.parseXmlString("<a><b id=\"1\">t</b></a>");
 * String xmlCompact = XMLUtils.toXmlString(doc);
 * String xmlPretty = XMLUtils.prettyPrint(doc);
 * Element b = XMLUtils.findFirstElementByTag(doc, "b");
 * String text = XMLUtils.getTextContent(b);
 * XMLUtils.setAttr(b, "name", "value");
 * XMLUtils.addChild(b, "c", "hello");
 * </pre>
 */
public final class XMLUtils {
    private XMLUtils() {}

    /**
     * 解析 XML 字符串为 Document
     *
     * 行为说明：
     * - 使用 UTF-8 将字符串转字节后解析；启用命名空间感知（namespaceAware=true）
     * - 异常统一包装为 {@link RuntimeException}
     *
     * 注意：
     * - 生产环境应为 {@link javax.xml.parsers.DocumentBuilderFactory} 设置安全特性，
     *   如禁用外部实体与 DTD，以防 XXE 攻击；示例：
     *   {@code f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)} 等
     *
     * @param xml XML 文本（UTF-8）
     * @return DOM 文档对象
     */
    public static Document parseXmlString(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * 将 Document 序列化为紧凑字符串
     *
     * 行为说明：
     * - 使用默认 Transformer 输出 XML；包含 XML 声明（OMIT_XML_DECLARATION=no）
     * - 不启用缩进，适合传输或存储紧凑文本
     *
     * @param doc DOM 文档对象
     * @return 紧凑 XML 字符串
     */
    public static String toXmlString(Document doc) {
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            StringWriter w = new StringWriter(); t.transform(new DOMSource(doc), new StreamResult(w)); return w.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * 美化输出（缩进）
     *
     * 行为说明：
     * - 启用缩进（INDENT=yes），并通过 Xalan/Apache 扩展属性设置缩进宽度（indent-amount=2）
     * - 输出包含 XML 声明；如需移除可设置 {@link OutputKeys#OMIT_XML_DECLARATION}
     *
     * @param doc DOM 文档对象
     * @return 带缩进的 XML 字符串
     */
    public static String prettyPrint(Document doc) {
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter w = new StringWriter(); t.transform(new DOMSource(doc), new StreamResult(w)); return w.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * 获取第一个指定标签的元素
     * @param doc DOM 文档
     * @param tag 标签名（不含命名空间前缀）
     * @return 首个匹配的元素，若不存在返回 null
     */
    public static Element findFirstElementByTag(Document doc, String tag) { NodeList nl=doc.getElementsByTagName(tag); return nl.getLength()>0?(Element)nl.item(0):null; }

    /**
     * 查找所有指定标签的元素
     * @param doc DOM 文档
     * @param tag 标签名（不含命名空间前缀）
     * @return 匹配元素列表（若无则为空列表）
     */
    public static List<Element> findElementsByTag(Document doc, String tag) { NodeList nl=doc.getElementsByTagName(tag); List<Element> out=new ArrayList<>(); for(int i=0;i<nl.getLength();i++) out.add((Element)nl.item(i)); return out; }

    /**
     * 获取元素的文本内容
     * @param el 元素
     * @return 文本内容（若元素为 null 则返回 null）
     */
    public static String getTextContent(Element el) { return el==null?null:el.getTextContent(); }

    /**
     * 设置元素的文本内容
     * @param el 元素（为 null 时忽略）
     * @param text 文本
     */
    public static void setTextContent(Element el, String text) { if(el!=null) el.setTextContent(text); }

    /**
     * 获取元素属性
     * @param el 元素
     * @param name 属性名
     * @return 属性值（不存在返回空串；元素为 null 返回 null）
     */
    public static String getAttr(Element el, String name) { return el==null?null:el.getAttribute(name); }

    /**
     * 设置元素属性
     * @param el 元素（为 null 时忽略）
     * @param name 属性名
     * @param value 属性值
     */
    public static void setAttr(Element el, String name, String value) { if(el!=null) el.setAttribute(name, value); }

    /**
     * 添加子元素并设置文本
     * @param parent 父元素
     * @param tag 子元素标签名
     * @param text 文本（可为 null）
     * @return 新增的子元素
     */
    public static Element addChild(Element parent, String tag, String text) { Document d=parent.getOwnerDocument(); Element child=d.createElement(tag); if(text!=null) child.setTextContent(text); parent.appendChild(child); return child; }

    /**
     * 移除指定标签的所有子元素
     * @param parent 父元素
     * @param tag 标签名
     * @return 移除的元素数量
     */
    public static int removeElementsByTag(Element parent, String tag) { NodeList nl=parent.getElementsByTagName(tag); int c=0; for(int i=nl.getLength()-1;i>=0;i--){ parent.removeChild(nl.item(i)); c++; } return c; }

    /**
     * 导入节点（深拷贝）到文档
     * @param doc 目标文档
     * @param node 源节点（来自其他文档或同文档）
     * @return 新节点（属于目标文档）
     */
    public static Node importNode(Document doc, Node node) { return doc.importNode(node, true); }

    /**
     * 获取元素的命名空间 URI
     * @param el 元素
     * @return 命名空间 URI（可能为 null）
     */
    public static String getNamespaceURI(Element el) { return el==null?null:el.getNamespaceURI(); }

    /**
     * 获取元素的限定名称
     * @param el 元素
     * @return 限定名称（含前缀）；元素为 null 返回 null
     */
    public static String getQualifiedName(Element el) { return el==null?null:el.getTagName(); }

    /**
     * 设置（或创建）直接子元素文本
     *
     * 行为：若存在首个同名子元素则更新其文本，否则创建后设置文本。
     * @param parent 父元素
     * @param tag 子元素标签名
     * @param text 文本
     * @return 被更新或新建的子元素
     */
    public static Element setChildText(Element parent, String tag, String text) {
        Element first=null; NodeList nl=parent.getElementsByTagName(tag); if(nl.getLength()>0) first=(Element)nl.item(0); else first=addChild(parent, tag, null); setTextContent(first,text); return first;
    }

    /**
     * 统计标签元素数量
     * @param doc DOM 文档
     * @param tag 标签名
     * @return 数量
     */
    public static int countElementsByTag(Document doc, String tag) { return doc.getElementsByTagName(tag).getLength(); }

    /**
     * 获取直接子元素列表（按标签过滤）
     * @param parent 父元素
     * @param tag 标签名（为 null 则返回所有直接子元素）
     * @return 直接子元素列表
     */
    public static List<Element> getChildrenByTag(Element parent, String tag) { List<Element> out=new ArrayList<>(); NodeList nl=parent.getChildNodes(); for(int i=0;i<nl.getLength();i++){ Node n=nl.item(i); if(n instanceof Element){ Element e=(Element)n; if(tag==null||e.getTagName().equals(tag)) out.add(e);} } return out; }

    /**
     * 获取所有后代元素列表（按标签过滤）
     * @param parent 父元素
     * @param tag 标签名
     * @return 后代元素列表
     */
    public static List<Element> getDescendantsByTag(Element parent, String tag) { List<Element> out=new ArrayList<>(); NodeList nl=parent.getElementsByTagName(tag); for(int i=0;i<nl.getLength();i++) out.add((Element)nl.item(i)); return out; }

    /**
     * 是否存在指定标签元素
     * @param doc DOM 文档
     * @param tag 标签名
     * @return 是否存在至少一个匹配元素
     */
    public static boolean hasElementByTag(Document doc, String tag) { return doc.getElementsByTagName(tag).getLength()>0; }
}
