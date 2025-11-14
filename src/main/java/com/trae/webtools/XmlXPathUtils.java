package com.trae.webtools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;

/**
 * XML XPath 工具（不与现有 XMLUtils 重复）
 *
 * XPath 简介：
 * - XPath 是用于在 XML 文档中定位节点的查询语言，基于路径表达式与谓词（条件）选择目标节点。
 * - 适配 DOM、SAX、JDOM 等多种模型；本工具基于 W3C DOM + JDK 自带 XPath 实现。
 *
 * 节点类型与常用选择：
 * - 元素节点：/a/b/c 或 //c（任意层级）
 * - 属性节点：/a/b/@name 或 //@id（任意层级属性）
 * - 文本节点：/a/b/text()（直接文本，混合内容可使用 string() 获取拼接文本）
 * - 注释节点：//comment()（通常不建议存业务数据）
 *
 * 轴（Axes）与语义：
 * - child：子节点（隐式轴，如 a/b 等同于 child::a/child::b）
 * - descendant：后代节点（// 等价于 descendant-or-self::node() 的简写）
 * - parent/ancestor：父/祖先
 * - following-sibling/preceding-sibling：同级后/前兄弟
 * - attribute：属性（@attr 等同于 attribute::attr）
 * - self/descendant-or-self/ancestor-or-self：自身/包含自身的后代/祖先
 *
 * 基本语法与示例：
 * - 路径选择：/catalog/book/title、//book/title
 * - 谓词过滤：//book[price>10]/title、//user[@role="admin"]
 * - 索引访问：//book[1]（第一个，XPath 索引从 1 开始）
 * - 函数使用：count(//book)>0、contains(@class, "active")、normalize-space(text())
 * - 转换函数：string(.), number(.), boolean(.)
 *
 * 命名空间（Namespace）：
 * - XPath 按前缀匹配命名空间，如 ns:tag；需在 XPath 实例上配置 NamespaceContext 才能生效。
 * - 本工具未内置命名空间绑定；如需使用命名空间，请在调用方创建 XPath 并设置 NamespaceContext。
 *
 * 性能与局限：
 * - 每次调用会编译并评估表达式；高频/复杂表达式建议缓存 XPathExpression。
 * - 使用 //（descendant）可能遍历整棵树，文档较大时需谨慎。
 * - DOM 适合中小文档；超大 XML 建议考虑 SAX/StAX 或分片处理。
 *
 * 常见陷阱：
 * - 命名空间未绑定导致选择失败（ns:tag 不匹配默认命名空间）。
 * - text() 仅返回直接文本节点；混合内容需使用 string() 或 normalize-space()。
 * - 索引从 1 开始；//item[1] 是第一个而非第二个。
 * - 属性选择必须使用 @attr，不能按元素路径选择属性。
 *
 * 使用建议：
 * - 优先使用明确路径与谓词过滤，避免滥用 //。
 * - 高频表达式预编译并重用 XPathExpression。
 * - 需要跨命名空间时配置 NamespaceContext，并使用带前缀的路径。
 *
 * 安全说明：
 * - 本工具仅对 DOM 进行 XPath 操作，不涉及 XML 解析配置。
 * - 解析 XML 时请禁用外部实体与 DTD 以防 XXE（在构造 Document 时设置）。
 *
 * 典型用法：
 * <pre>
 * String t = XmlXPathUtils.evalString(doc, "/catalog/book[1]/title");
 * double p = XmlXPathUtils.evalNumber(doc, "number(//book[price>0]/price[1])");
 * boolean ok = XmlXPathUtils.evalBoolean(doc, "count(//book)>0");
 * org.w3c.dom.NodeList list = XmlXPathUtils.selectNodes(doc, "//book[@id]");
 * org.w3c.dom.Element leaf = XmlXPathUtils.ensureElementPath(root, "a/b/c");
 * XmlXPathUtils.setAttributeByXPath(doc, "//a/b", "name", "v");
 * </pre>
 */
public final class XmlXPathUtils {
    private XmlXPathUtils() {}

    /** 评估为字符串（不存在返回空串） */
    public static String evalString(Document doc, String xpath) { try{ XPath xp=XPathFactory.newInstance().newXPath(); XPathExpression ex=xp.compile(xpath); Object r=ex.evaluate(doc, XPathConstants.STRING); return r==null?"":String.valueOf(r); }catch(Exception e){ throw new RuntimeException(e);} }

    /** 评估为双精度数值（不可解析返回 NaN） */
    public static double evalNumber(Document doc, String xpath) { try{ XPath xp=XPathFactory.newInstance().newXPath(); XPathExpression ex=xp.compile(xpath); Object r=ex.evaluate(doc, XPathConstants.NUMBER); return r==null?Double.NaN:((Number)r).doubleValue(); }catch(Exception e){ throw new RuntimeException(e);} }

    /** 评估为布尔（不存在返回 false） */
    public static boolean evalBoolean(Document doc, String xpath) { try{ XPath xp=XPathFactory.newInstance().newXPath(); XPathExpression ex=xp.compile(xpath); Object r=ex.evaluate(doc, XPathConstants.BOOLEAN); return r!=null && (Boolean)r; }catch(Exception e){ throw new RuntimeException(e);} }

    /** 选择节点列表（若不存在返回空列表） */
    public static NodeList selectNodes(Document doc, String xpath) { try{ XPath xp=XPathFactory.newInstance().newXPath(); XPathExpression ex=xp.compile(xpath); Object r=ex.evaluate(doc, XPathConstants.NODESET); return (NodeList)r; }catch(Exception e){ throw new RuntimeException(e);} }

    /** 选择首个节点（若不存在返回 null） */
    public static Node selectFirstNode(Document doc, String xpath) { NodeList nl=selectNodes(doc,xpath); return (nl==null||nl.getLength()==0)?null:nl.item(0); }

    /** 设置属性值（按 XPath 定位到元素） */
    public static void setAttributeByXPath(Document doc, String xpath, String attrName, String attrValue) { Node n=selectFirstNode(doc,xpath); if(n instanceof Element) ((Element)n).setAttribute(attrName, attrValue); }

    /** 读取属性值（按 XPath 定位到元素） */
    public static String getAttributeByXPath(Document doc, String xpath, String attrName, String def) { Node n=selectFirstNode(doc,xpath); return (n instanceof Element)?((Element)n).getAttribute(attrName):def; }

    /** 设置节点文本（按 XPath 定位到元素） */
    public static void setTextByXPath(Document doc, String xpath, String text) { Node n=selectFirstNode(doc,xpath); if(n!=null) n.setTextContent(text); }

    /** 读取节点文本（按 XPath 定位到元素） */
    public static String getTextByXPath(Document doc, String xpath, String def) { Node n=selectFirstNode(doc,xpath); return n==null?def:n.getTextContent(); }

    /** 确保路径元素存在（如 a/b/c），逐级创建缺失元素并返回末级元素 */
    public static Element ensureElementPath(Element parent, String path) { String[] parts=path.split("/"); Element cur=parent; for(String p:parts){ NodeList nl=cur.getElementsByTagName(p); Element next=null; for(int i=0;i<nl.getLength();i++){ Node n=nl.item(i); if(n.getParentNode()==cur){ next=(Element)n; break; } } if(next==null){ next=cur.getOwnerDocument().createElement(p); cur.appendChild(next);} cur=next; } return cur; }

    /** 按 XPath 移除匹配节点（返回移除数量） */
    public static int removeByXPath(Document doc, String xpath) { NodeList nl=selectNodes(doc,xpath); int c=0; if(nl!=null) for(int i=0;i<nl.getLength();i++){ Node n=nl.item(i); if(n.getParentNode()!=null){ n.getParentNode().removeChild(n); c++; } } return c; }

    /** 按 XPath 重命名匹配元素标签（仅元素） */
    public static int renameElements(Document doc, String xpath, String newTag) { NodeList nl=selectNodes(doc,xpath); int c=0; if(nl!=null) for(int i=0;i<nl.getLength();i++){ Node n=nl.item(i); if(n instanceof Element){ Element e=(Element)n; Element ne=doc.createElement(newTag); while(e.hasChildNodes()) ne.appendChild(e.getFirstChild()); org.w3c.dom.NamedNodeMap attrs=e.getAttributes(); for(int j=0;j<attrs.getLength();j++){ Node a=attrs.item(j); ne.setAttribute(a.getNodeName(), a.getNodeValue()); } Node p=e.getParentNode(); p.replaceChild(ne, e); c++; } } return c; }

    /** 节点是否存在 */
    public static boolean nodeExists(Document doc, String xpath) { return selectFirstNode(doc,xpath)!=null; }

    /** 统计匹配节点数量 */
    public static int countNodes(Document doc, String xpath) { NodeList nl=selectNodes(doc,xpath); return nl==null?0:nl.getLength(); }

    /** 在指定 XPath 元素下添加子元素并设置文本 */
    public static Element addElementAtXPath(Document doc, String xpath, String tag, String text) { Node n=selectFirstNode(doc,xpath); if(!(n instanceof Element)) return null; Element child=doc.createElement(tag); if(text!=null) child.setTextContent(text); n.appendChild(child); return child; }

    /** 替换匹配元素的文本内容 */
    public static int replaceTextByXPath(Document doc, String xpath, String text) { NodeList nl=selectNodes(doc,xpath); int c=0; if(nl!=null) for(int i=0;i<nl.getLength();i++){ Node n=nl.item(i); n.setTextContent(text); c++; } return c; }
}
