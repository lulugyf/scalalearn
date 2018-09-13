package gyf.test.inoreader;

import java.io.StringReader;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Article {

	public String title;
	public String content;
	public String source;
	public String href;
	public String id;

	public Article(String content) throws Exception {
		source = null;
		InputSource src = new InputSource(new StringReader(content));
    	DOMParser parser = new DOMParser();
    	parser.parse(src);
    	
    	Document root = parser.getDocument();
    	NodeList nl = root.getElementsByTagName("A");
    	if(nl.getLength() > 0) {
    		Node n = nl.item(0);
    		title = n.getTextContent();
    		href = n.getAttributes().getNamedItem("href").getNodeValue();
    	}else {
    		title = null;
    		href = null;
    	}

        StringBuilder sb = new StringBuilder();
        extractText(root, sb);
        
        this.content = sb.toString();
	}
	
	private String lastNodeName = null;
	public void extractText(Node node, StringBuilder sb) {
		final String nname = node.getNodeName();
		if("#text".equals(nname) && (
				"P".equals(lastNodeName) 
				||"STRONG".equals(lastNodeName)
				||"BR".equals(lastNodeName)
				)) {
//			Utils.log("---["+lastNodeName+"]");
			String val = node.getNodeValue();
			val = val.trim();
			if(val.length() > 0) {
				sb.append(val);
				sb.append("\n");
			}
		}
		if(!"#text".equals(nname) && !"A".equals(nname))
			lastNodeName = nname;
		Node child = node.getFirstChild();
        while (child != null) {
        	extractText(child, sb);
            child = child.getNextSibling();
        }
	}
	
    public static void main(String[] argv) throws Exception {
    	String content = Utils.fileToString("v.js", null);
    	Article a = new Article(content);
    	
    	Utils.log(a.title);
    	Utils.log(a.href);
    	Utils.log(a.content);
    	
    	print_html(content);
    }
    public static void print_html(String content) throws Exception {
//    	String content = "<div dir=\"ltr\"><a style=\"text-decoration:none;font-weight:bold;font-size:1.1em;\" class=\"bluelink\" target=\"_blank\" rel=\"noopener\" href=\"https://botanwang.com/articles/201807/%E4%BB%A5%E8%89%B2%E5%88%97%E9%80%9A%E8%BF%87%E6%B0%91%E6%97%8F%E5%9B%BD%E5%AE%B6%E6%B3%95%E5%94%AF%E7%8A%B9%E5%A4%AA%E4%BA%BA%E5%8F%AF%E4%BA%AB%E8%87%AA%E6%B2%BB%E6%9D%83.html\">以色列通过民族国家法唯犹太人可享自治权</a> <div class=\"article_author\">发表于 12:43 由  <span style=\"font-style:italic\">daying</span> 通过 <a class=\"bluelink boldlink\" style=\"font-style:normal !important;text-decoration:none !important;\" href=\"?list_articles=1&filter_type=subscription&filter_id=22541990\">博谈网</a> </div></div><div id=\"article_contents_inner_16573942101\" class=\"article_content\"><div><div>来源:?</div><div><div>美国之音</div></div></div><div><div><div><p>以色列议会星期四通过一项有高度争议的法律。根据新通过的法律，只有以色列的犹太人才享有自治权，并鼓励建立犹太人定居点。</p> ";
//    	String content = Utils.fileToString("v.js", null);

    	Utils.log("-------print_html parser:");
		InputSource src = new InputSource(new StringReader(content));
    	DOMParser parser = new DOMParser();
    	
    	parser.parse(src);
    	
    	Document root = parser.getDocument();
    	NodeList nl = root.getElementsByTagName("A");
    	for(int i=0; i<nl.getLength(); i++) {
    		Node n = nl.item(i);
    		Utils.log(n.getNodeName() + " " + n.getTextContent() + " " + n.getAttributes().getNamedItem("href").getNodeValue());
    	}
        print(parser.getDocument(), "  ");

    }
    public static void print(Node node, String indent) {
        System.out.println(indent+node.getNodeName() + " " + node.getNodeValue());
        Node child = node.getFirstChild();
        while (child != null) {
            print(child, indent+" ");
            child = child.getNextSibling();
        }
    }
}
