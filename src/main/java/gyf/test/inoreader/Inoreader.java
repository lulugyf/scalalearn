package gyf.test.inoreader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.DefaultCookieSpec;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EntityUtils;

public class Inoreader {
	private static String start_url = "https://www.inoreader.com/";
	private HttpClientContext context;
	private CloseableHttpClient httpclient;
	private CookieStore cookieStore;
	private String cookie_file = "cookies.obj";
	private DefaultCookieSpec cookieSpec = new DefaultCookieSpec();

	private String rootDir;
	private String tmpDir;
	private boolean cookieChg = false; //is new cookie set
	private String seen_ids;

	public static void main(String[] args) throws Exception {
		new Inoreader("./tmp").start();
//		log(System.currentTimeMillis());
	}

	public Inoreader(String rootdir) {
		this.rootDir = rootdir;
		File f = new File(rootDir);
		if(!f.exists()){
			f.mkdirs();
		}
		cookie_file = rootDir + File.separator + "cookies_file";
		tmpDir = rootDir + File.separator + "tmp" + File.separator;
		f = new File(tmpDir);
		if(!f.exists() ) f.mkdirs();

		buildClient(false);
		try {
			restoreCookie();
		}catch(Exception ex){
		}
	}

	public void start() throws Exception{
//		System.out.println("begin");

		String content = get(start_url, null);
		if(content != null && content.indexOf("landing_signin") > 0) {  //未登录的首页内容中， 有下面这个
			// <div class="pull_left" id="landing_signin">
//<a href="javascript:void(0);" id="login_air" class="underlink_hover">Sign in</a>
//</div>
			login();
		}
		//get(start_url, null, "index_2");


//		getIndex();
//		login();
//	//	step3();
//		step4();

//		m_first();
//		m_old("");
		// https://www.inoreader.com/m/ajax.php?ajax=1&mark_read=16577750434&force_read=1  // 标记已经读过
		// POST https://www.inoreader.com/m/ajax.php?list_articles=1&ajax=1
		//      seen_ids: 16577128332,16575866313,...        //加载更旧的标题

//		printCookie();

		saveCookie();
	}

	public void m_old(String seen_ids) throws Exception {
		// extract seen_ids from file
		String content = Utils.fileToString("mcontent.js.1", null);
		List<Article> la = JsonUtil.extractArticles(content);
		seen_ids = JsonUtil.seen_ids;

		HttpPost post = new HttpPost("https://www.inoreader.com/m/ajax.php?list_articles=1&ajax=1");
		addDefHeaders(post);
		post.addHeader("referer", "https://www.inoreader.com/m/?list_articles=1");
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair("seen_ids", seen_ids));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
		post.setEntity(entity);

		CloseableHttpResponse resp = httpclient.execute(post);
		saveResp(resp, "m_old.json");
	}

	public String m_first() throws Exception {
		String murl = "https://www.inoreader.com/m/";
		String url = "https://www.inoreader.com/m/?list_articles=1";
		String content = get(url, murl);

		StringBuilder sb = new StringBuilder();

		int p1 = content.indexOf(murl);
		if(p1 > 0) {
			int p2 = content.indexOf("\"", p1+10);
			String loc = content.substring(p1, p2);
			log("=====loc:"+loc);
			get(loc, url, "mcontent.js");
			List<Article> la = JsonUtil.extractArticles(Utils.fileToString("mcontent.js", null));
			Utils.log("seen_ids: "+JsonUtil.seen_ids);
			this.seen_ids = JsonUtil.seen_ids;
			for(Article a: la) {
				sb.append("====" + a.id + ":" + a.title);
				//Utils.log(a.href);
				sb.append(a.content);
				sb.append("\n\n");
			}
		}
		return sb.toString();
	}


	public void step4() throws Exception {
		log("----step4");
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair("xjxfun", "print_articles"));
		formparams.add(new BasicNameValuePair("xjxr", String.valueOf(System.currentTimeMillis())));
		formparams.add(new BasicNameValuePair("xjxargs[]", "Bfalse"));
		formparams.add(new BasicNameValuePair("xjxargs[]", "N0"));
		formparams.add(new BasicNameValuePair("xjxargs[]", "{\"articles_order\":0,\"view_style\":1,\"global_articles_order\":0}"));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
		URI uri = new URI(start_url);
		HttpPost post = new HttpPost(uri);
		post.setEntity(entity);

		CloseableHttpResponse resp = httpclient.execute(post);
		printResp(resp);
		if(resp.getStatusLine().getStatusCode() == 200) {
			parseCookie(resp, post.getURI());
		}
		saveResp(resp, "step4");
	}

	public void step3() throws Exception {
		get(start_url, start_url, "step3");
	}

	public void login() throws Exception {
		log("----login");
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair("warp_action", "login"));
		formparams.add(new BasicNameValuePair("hash_action", ""));
		formparams.add(new BasicNameValuePair("sendback", ""));
		formparams.add(new BasicNameValuePair("username", "gyf_freedom@sohu.com"));
		formparams.add(new BasicNameValuePair("password", "helloworld"));
		formparams.add(new BasicNameValuePair("remember_me", "on"));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
		URI uri = new URI(start_url);
		HttpPost post = new HttpPost(uri);
		post.setEntity(entity);
		addDefHeaders(post);
		//warp_action=login&hash_action=&sendback=&username=gyf_freedom%40sohu.com&password=helloworld&remember_me=on

		CloseableHttpResponse resp = httpclient.execute(post);
		int statusCode = resp.getStatusLine().getStatusCode();
		if(302 == statusCode) {
			parseCookie(resp, uri);
			String location = resp.getFirstHeader("location").getValue();
			log("login success!!, redirect to "+location);
			resp.close();
			get(location, start_url, "login");
		}else {
			log("login failed");
		}
	}

	public void getIndex() throws Exception {
		log("---getIndex");
		URI uri = new URIBuilder()
				.setScheme("https")
				.setHost("www.inoreader.com")
				.setPath("/")
				.build();
		HttpGet get = new HttpGet(uri);
		addDefHeaders(get);
		CloseableHttpResponse response1 = httpclient.execute(get, context);
		parseCookie(response1, uri);

		printResp(response1);
		saveResp(response1, "getIndex.html");
	}

	private void printCookie() {
		log("===all Cookies");
		BasicCookieStore cs = (BasicCookieStore)cookieStore;
		for(Cookie c: cs.getCookies() ) {
			System.out.printf("cookie: name=%s; value=%s; path=%s; domain=%s\n",
					c.getName(), c.getValue(), c.getPath(), c.getDomain());
		}
	}

	private void printResp(HttpResponse resp) {
		log("==response: "+resp.getStatusLine());
		for(Header h: resp.getAllHeaders()) {
			log("   "+ h.getName() + ": " + h.getValue());
		}
	}

	private void get(String url, String referer, String fname) throws Exception {
		HttpGet get = new HttpGet(url);
		addDefHeaders(get);
		if(referer != null)
			get.addHeader("referer", referer);
		CloseableHttpResponse resp = httpclient.execute(get, context);
		saveResp(resp, fname);
	}

	private String get(String url, String referer) throws Exception {
		HttpGet get = new HttpGet(url);
		addDefHeaders(get);
		if(referer != null)
			get.addHeader("referer", referer);
		CloseableHttpResponse resp = httpclient.execute(get, context);
		parseCookie(resp, get.getURI());
		if(200 == resp.getStatusLine().getStatusCode())
			return EntityUtils.toString(resp.getEntity());
		else {
			log("get failed:" + resp.getStatusLine());
			return "";
		}
	}

	private void saveResp(CloseableHttpResponse resp, String fname) throws Exception {
		if(200 != resp.getStatusLine().getStatusCode()) {
			log("saveResp failed, request failed with:"+resp.getStatusLine());
			return;
		}
		fname = tmpDir + fname;

		OutputStream fw = new FileOutputStream(fname);
		String ctype = resp.getFirstHeader("Content-Type").getValue();
		if(ctype.indexOf("application/json") > 0 && !fname.endsWith(".json"))
			fname = fname + ".json";
		else if(ctype.indexOf("text/html") > 0 && !fname.endsWith(".html"))
			fname = fname + ".html";
		else if(ctype.indexOf("application/javascript") > 0 && !fname.endsWith(".js"))
			fname = fname + ".js";
		try {
//	        log(resp.getStatusLine());
			HttpEntity entity = resp.getEntity();
//	        log("isChunked="+entity.isChunked());

			entity.writeTo(fw);
		} finally {
			resp.close();
			fw.close();
		}
	}

	private HttpClientContext getHttpClientContext() {
		HttpClientContext context = null;
		context = HttpClientContext.create();
//	    Registry<CookieSpecProvider> registry = RegistryBuilder
//	            .<CookieSpecProvider>create()
//	            .register(CookieSpecs.DEFAULT, new DefaultCookieSpecProvider())
//	            .build();
//	    context.setCookieSpecRegistry(registry);
		return context;
	}

	private void buildClient(boolean useProxy) {
		DefaultProxyRoutePlanner routePlanner = null;
		if(useProxy) {
			HttpHost proxy = new HttpHost("172.22.0.23", 8989);
			routePlanner = new DefaultProxyRoutePlanner(proxy);
		}

//		Lookup<CookieSpecProvider> cookieSpecReg = RegistryBuilder.<CookieSpecProvider>create().build();

		context = getHttpClientContext();
		cookieStore = new BasicCookieStore();

		context.setCookieStore(cookieStore);

		httpclient = HttpClients.custom()
				.setRoutePlanner(routePlanner)
				.setDefaultCookieStore(cookieStore)
				.build();
//		return httpclient;

		//return HttpClients.createDefault();
	}

	private void parseCookie(HttpResponse response, URI uri) throws Exception {
		CookieOrigin co = new CookieOrigin(uri.getHost(), uri.getPort()>0?uri.getPort():443, "/", true);  //uri.getPath()
		for(Header h: response.getHeaders("Set-Cookie")) {
			log("---" + h.getValue());
			List<Cookie> l = cookieSpec.parse(h, co);
			if(l != null) {
				for(Cookie c: l) {
					cookieStore.addCookie(c);
					cookieChg = true;
				}
			}else {
				log("failed to parse cookies");
			}
		}
	}
	private static void log(Object msg) {
		System.out.println(msg);
	}

	private void addDefHeaders(HttpRequestBase req) {
//		httpget.addHeader(":authority", "www.inoreader.com");
//		httpget.addHeader(":method", "GET");
//		httpget.addHeader(":path", "/");
//		httpget.addHeader(":scheme", "https");
		req.addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		req.addHeader("accept-encoding", "gzip, deflate, br");
		req.addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
		req.addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");

	}


	private void saveCookie() throws Exception {
		if(cookieChg) {
			ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream(cookie_file));
			oo.writeObject(cookieStore);
			oo.close();
			log("saveCookie done!");
		}
	}

	private void restoreCookie() throws Exception {
		File f = new File(cookie_file);
		if(f.exists()) {
			ObjectInputStream oi = new ObjectInputStream(new FileInputStream(cookie_file));
			BasicCookieStore cs = (BasicCookieStore)oi.readObject();
			oi.close();

			if(context != null) {
				context.setCookieStore(cs);
			}
			for(Cookie c: cs.getCookies() ) {
//				System.out.printf("restore cookie: name=%s; value=%s; path=%s\n",
//						c.getName(), c.getValue(), c.getPath());
				cookieStore.addCookie(c);
			}
		}
		// screen_pixel_ratio=1; screen_resolution=1366x768; device_type=normal; window_dimensions=1345x635
		boolean has_screen = false;
		for(Cookie c: cookieStore.getCookies()) {
			if("screen_pixel_ratio".equals(c.getName())) {
				has_screen = true;
				break;
			}
		}
		if(!has_screen) {
			log("--- add screen cookies");
			_addCookie("screen_pixel_ratio", "1", "/" );
			_addCookie("screen_resolution", "1366x768", "/" );
			_addCookie("device_type", "normal", "/");
			_addCookie("window_dimensions", "1345x635", "/");
		}
	}
	private void _addCookie(String n, String v, String p) {
		BasicClientCookie bcc = new BasicClientCookie(n, v);
		bcc.setPath(p);
		bcc.setDomain("www.inoreader.com");
		cookieStore.addCookie(bcc);
	}


}
