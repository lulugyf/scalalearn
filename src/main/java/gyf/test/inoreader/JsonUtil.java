package gyf.test.inoreader;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.DefaultJSONParser;

public class JsonUtil {
	public static void main(String[] args) throws Exception {
//		String content = Utils.fileToString("m_old.json", null);
		String content = Utils.fileToString("mcontent.js.1", null);
		
		List<Article> la = extractArticles(content);
		Utils.log("seen_ids: "+JsonUtil.seen_ids);
		for(Article a: la) {
			Utils.log("====" + a.id + ":" + a.title);
	    	Utils.log(a.href);
	    	Utils.log(a.content);
		}
	}
	public static void main1(String[] args) throws Exception {
		String content = "<div dir=\"ltr\"><a style=\"text-decoration:none;font-weight:bold;font-size:1.1em;\" class=\"bluelink\" target=\"_blank\" rel=\"noopener\" href=\"https://botanwang.com/articles/201807/%E4%BB%A5%E8%89%B2%E5%88%97%E9%80%9A%E8%BF%87%E6%B0%91%E6%97%8F%E5%9B%BD%E5%AE%B6%E6%B3%95%E5%94%AF%E7%8A%B9%E5%A4%AA%E4%BA%BA%E5%8F%AF%E4%BA%AB%E8%87%AA%E6%B2%BB%E6%9D%83.html\">以色列通过民族国家法唯犹太人可享自治权</a> <div class=\"article_author\">发表于 12:43 由  <span style=\"font-style:italic\">daying</span> 通过 <a class=\"bluelink boldlink\" style=\"font-style:normal !important;text-decoration:none !important;\" href=\"?list_articles=1&filter_type=subscription&filter_id=22541990\">博谈网</a> </div></div><div id=\"article_contents_inner_16573942101\" class=\"article_content\"><div><div>来源:?</div><div><div>美国之音</div></div></div><div><div><div><p>以色列议会星期四通过一项有高度争议的法律。根据新通过的法律，只有以色列的犹太人才享有自治权，并鼓励建立犹太人定居点。</p> ";
		Pattern r = Pattern.compile("<a style=\".+href=\"([^\"]+)\"");
		Matcher m = r.matcher(content);
		if (m.find( )) {
			log("Found value: " + m.group(1) );
//			log("Found value: " + m.group(2) );
		}else {
			log("NO MATCH");
		}
	}
	private static String _parseArticleJS(String content) throws Exception {
		Pattern r = Pattern.compile("var seen_ids *= *\\[([\\d\\, ]+)\\]\\;");
		Matcher m = r.matcher(content);
		if (m.find( )) {
			//log("Found value: " + m.group(1) );
			seen_ids = m.group(1);
		}else {
			log("seen_ids found");
		}
		
		r = Pattern.compile("var article_contents *= *(\\{.+\\})\\;[\\n\\r\\t]*var links_cache *=");
		m = r.matcher(content);
		if(m.find()) {
			return m.group(1);
		}else {
			log("article_contents not found");
		}
		return null;
	}
	private static String _parseArticleJSON(String content) throws Exception {
		JSONObject jo = JSON.parseObject(content);
		JSONArray ja = jo.getJSONArray("seen_ids");
		String s = ja.toString();
		seen_ids = s.substring(1, s.length()-1).replaceAll("\"", "");
		return jo.getString("articles_contents");
	}
	
	public static String seen_ids = null;
	public static List<Article> extractArticles(String content) throws Exception {
//		String content = Utils.fileToString("mcontent.js", null);
		String json_cont = null;
		if(content.startsWith("{")) {
			json_cont = _parseArticleJSON(content);
		}else {
			json_cont = _parseArticleJS(content);
		}
		
		LinkedList<Article> ret = new LinkedList<>();
		if(json_cont == null)
			return ret;
		JSONObject jo = JSON.parseObject(json_cont);
		for(String k: jo.keySet()) {
			String v = jo.getString(k);
			if("16575474001".equals(k))
				Utils.stringToFile("v.js", v, null);
			Article a = new Article(v);
			a.id = k;
			ret.add(a);
		}
		return ret;
	}
	
	
	
	public static void test1(String[] args) throws Exception {
		JSONObject jo = null;
		
		InputStream ji = new FileInputStream("step4.json");
		jo = JSON.parseObject(ji, JSONObject.class);
		ji.close();
		
		for(String k: jo.keySet()) {
			Object v = jo.get(k);
			if(v instanceof JSONArray) {
				JSONArray a = (JSONArray)v;
				for(int i=0; i<a.size(); i++) {
					log(k+" ["+a.get(i));
				}
			}else {
				log(k + "==" + v.toString());
			}
		}
	}
	
	private static void log(Object msg) {
		System.out.println(msg);
	}
}
