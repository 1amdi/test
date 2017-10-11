import com.yidianzixun.entity.ProxyIp;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.SocketTimeoutException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.Map.Entry;

import static com.yidianzixun.util.JsoupUtils.getProxyIpList;


/**
 * Created by gongjiao on 2017/5/17.
 */
public class HttpClientUtils {
    private static Logger log = Logger.getLogger(HttpClientUtils.class);
    public static final int connTimeout = 10000;
    public static final int readTimeout = 10000;
    public static final String charset = "UTF-8";
    private static HttpClient client = null;

    // 存放所有下载验证码的目录
    private static final String DOWNLOAD_DIR = "E:\\testimg\\shixinren";

    private static int trynums = 1;

    static {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(128);
        cm.setDefaultMaxPerRoute(128);
        client = HttpClients.custom().setConnectionManager(cm).build();
    }

    public static String postParameters(String url, String parameterStr) throws ConnectTimeoutException, SocketTimeoutException, Exception {
        return post(url, parameterStr, "application/x-www-form-urlencoded", charset, connTimeout, readTimeout);
    }

    public static String postParameters(String url, String parameterStr, String charset, Integer connTimeout, Integer readTimeout) throws ConnectTimeoutException, SocketTimeoutException, Exception {
        return post(url, parameterStr, "application/x-www-form-urlencoded", charset, connTimeout, readTimeout);
    }

    public static String postParameters(String url, Map<String, String> params) throws ConnectTimeoutException,
            SocketTimeoutException, Exception {
        return postForm(url, params, null, connTimeout, readTimeout);
    }

    public static String postParameters(String url, Map<String, String> params, Integer connTimeout, Integer readTimeout) throws ConnectTimeoutException,
            SocketTimeoutException, Exception {
        return postForm(url, params, null, connTimeout, readTimeout);
    }

    public static String get(String url) throws Exception {
        return get(url, charset, null, null);
    }

    public static String getPic(String url) throws Exception {
        return get(url, charset, null, null);
    }

    public static String get(String url, String charset) throws Exception {
        return get(url, charset, connTimeout, readTimeout);
    }

    public static String getPic(String url, String charset) throws Exception {
        return getPic(url, charset, connTimeout, readTimeout);
    }

    /**
     * 发送一个 Post 请求, 使用指定的字符集编码.
     *
     * @param url
     * @param body        RequestBody
     * @param mimeType    例如 application/xml "application/x-www-form-urlencoded" a=1&b=2&c=3
     * @param charset     编码
     * @param connTimeout 建立链接超时时间,毫秒.
     * @param readTimeout 响应超时时间,毫秒.
     * @return ResponseBody, 使用指定的字符集编码.
     * @throws ConnectTimeoutException 建立链接超时异常
     * @throws SocketTimeoutException  响应超时
     * @throws Exception
     */
    public static String post(String url, String body, String mimeType, String charset, Integer connTimeout, Integer readTimeout)
            throws ConnectTimeoutException, SocketTimeoutException, Exception {
        HttpClient client = null;
        HttpPost post = new HttpPost(url);
        String result = "";
        try {
            if (StringUtils.isNotBlank(body)) {
                HttpEntity entity = new StringEntity(body, ContentType.create(mimeType, charset));
                post.setEntity(entity);
            }
            // 设置参数
            Builder customReqConf = RequestConfig.custom();
            if (connTimeout != null) {
                customReqConf.setConnectTimeout(connTimeout);
            }
            if (readTimeout != null) {
                customReqConf.setSocketTimeout(readTimeout);
            }
            post.setConfig(customReqConf.build());

            HttpResponse res;
            if (url.startsWith("https")) {
                // 执行 Https 请求.
                client = createSSLInsecureClient();
                res = client.execute(post);
            } else {
                // 执行 Http 请求.
                client = HttpClientUtils.client;
                res = client.execute(post);
            }
            result = IOUtils.toString(res.getEntity().getContent(), charset);
        } finally {
            post.releaseConnection();
            if (url.startsWith("https") && client != null && client instanceof CloseableHttpClient) {
                ((CloseableHttpClient) client).close();
            }
        }
        return result;
    }


    /**
     * 提交form表单
     *
     * @param url
     * @param params
     * @param connTimeout
     * @param readTimeout
     * @return
     * @throws ConnectTimeoutException
     * @throws SocketTimeoutException
     * @throws Exception
     */
    public static String postForm(String url, Map<String, String> params, Map<String, String> headers, Integer connTimeout, Integer readTimeout) throws ConnectTimeoutException,
            SocketTimeoutException, Exception {

        HttpClient client = null;
        HttpPost post = new HttpPost(url);
        try {
            if (params != null && !params.isEmpty()) {
                List<NameValuePair> formParams = new ArrayList<NameValuePair>();
                Set<Entry<String, String>> entrySet = params.entrySet();
                for (Entry<String, String> entry : entrySet) {
                    formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, Consts.UTF_8);
                post.setEntity(entity);
            }

            if (headers != null && !headers.isEmpty()) {
                for (Entry<String, String> entry : headers.entrySet()) {
                    post.addHeader(entry.getKey(), entry.getValue());
                }
            }
            // 设置参数
            Builder customReqConf = RequestConfig.custom();
            if (connTimeout != null) {
                customReqConf.setConnectTimeout(connTimeout);
            }
            if (readTimeout != null) {
                customReqConf.setSocketTimeout(readTimeout);
            }
            post.setConfig(customReqConf.build());
            HttpResponse res = null;
            if (url.startsWith("https")) {
                // 执行 Https 请求.
                client = createSSLInsecureClient();
                res = client.execute(post);
            } else {
                // 执行 Http 请求.
                client = HttpClientUtils.client;
                res = client.execute(post);
            }
            return IOUtils.toString(res.getEntity().getContent(), "UTF-8");
        } catch (SocketTimeoutException e) {
            if (trynums < 5) {
                trynums++;
                Thread.sleep(10000);
                return postForm(url, params, headers, connTimeout, readTimeout);
            } else {
                log.error(e);
                return null;
            }
        } finally {
            post.releaseConnection();
            if (url.startsWith("https") && client != null
                    && client instanceof CloseableHttpClient) {
                ((CloseableHttpClient) client).close();
            }
        }
    }

    /**
     * 提交form表单
     * 设置代理IP
     *
     * @param url
     * @param params
     * @param connTimeout
     * @param readTimeout
     * @param proxyIp
     * @return
     * @throws ConnectTimeoutException
     * @throws SocketTimeoutException
     * @throws Exception
     */
    public static String postForm(String url, Map<String, String> params, Map<String, String> headers, Integer connTimeout, Integer readTimeout, ProxyIp proxyIp) throws ConnectTimeoutException,
            SocketTimeoutException, Exception {

        HttpClient client = null;
        HttpPost post = new HttpPost(url);
        try {
            if (params != null && !params.isEmpty()) {
                List<NameValuePair> formParams = new ArrayList<NameValuePair>();
                Set<Entry<String, String>> entrySet = params.entrySet();
                for (Entry<String, String> entry : entrySet) {
                    formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, Consts.UTF_8);
                post.setEntity(entity);
            }

            if (headers != null && !headers.isEmpty()) {
                for (Entry<String, String> entry : headers.entrySet()) {
                    post.addHeader(entry.getKey(), entry.getValue());
                }
            }
            // 设置参数
            Builder customReqConf = RequestConfig.custom();
            if (connTimeout != null) {
                customReqConf.setConnectTimeout(connTimeout);
            }
            if (readTimeout != null) {
                customReqConf.setSocketTimeout(readTimeout);
            }

            //设置代理IP
            HttpHost proxy = null;
            try {
                proxy = new HttpHost(proxyIp.getIp(), Integer.parseInt(proxyIp.getPort()));
            } catch (IllegalArgumentException e) {
//                return null;
            }
            if (proxyIp != null) {
                customReqConf.setProxy(proxy);
            }
            post.setConfig(customReqConf.build());
            HttpResponse res = null;
            if (url.startsWith("https")) {
                // 执行 Https 请求.
                client = createSSLInsecureClient();
                res = client.execute(post);
            } else {
                // 执行 Http 请求.
                client = HttpClientUtils.client;
                res = client.execute(post);
            }
            return IOUtils.toString(res.getEntity().getContent(), "UTF-8");
        } catch (SocketTimeoutException e) {
            if (trynums < 5) {
                trynums++;
                Thread.sleep(10000);
                return postForm(url, params, headers, connTimeout, readTimeout);
            } else {
                log.error(e);
                return null;
            }
        } finally {
            post.releaseConnection();
            if (url.startsWith("https") && client != null
                    && client instanceof CloseableHttpClient) {
                ((CloseableHttpClient) client).close();
            }
        }
    }


    public static List<Object> getPageSourceByIPlistHttp(String url, Map<String, String> headers, Map<String, String> params, int start) {
        List<ProxyIp> proxyIpList = getProxyIpList();
        List<Object> result = new ArrayList<>();
        Document document = null;
        Elements complist = null;
        Elements totalcount = null;
        String pagesource = null;
        if (start == proxyIpList.size()) {
            start = 0;
        }


        for (int i = start; i < proxyIpList.size(); i++) {
            ProxyIp proxyPo = proxyIpList.get(i);
            try {
                pagesource = postForm(url, params, headers, 60000, 60000, proxyIpList.get(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
            start = i;
            if (pagesource != null && !pagesource.contains("过于频繁") && pagesource.contains("recordsTotal")) {
//                String text = Jsoup.parse(pagesource).text();
                break;
            }

        }
        result.add(pagesource);
        result.add(start);
        return result;
    }

    public static List<Object> getPageSourceByIPlistHttpGet(String url, Map<String, String> headers, int start) {
        List<ProxyIp> proxyIpList = getProxyIpList();
        List<Object> result = new ArrayList<>();
        Document document = null;
        Elements complist = null;
        Elements totalcount = null;
        String pagesource = null;
        if (start == proxyIpList.size()) {
            start = 0;
        }


        for (int i = start; i < proxyIpList.size(); i++) {
            ProxyIp proxyPo = proxyIpList.get(i);
            try {
                pagesource = get(url, headers, proxyIpList.get(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
            start = i;
            if (pagesource != null&&!pagesource.equals("")) {
//                String text = Jsoup.parse(pagesource).text();

                document = Jsoup.parse(pagesource);
                break;
            }

        }
        result.add(document);
        result.add(start);
        return result;
    }


    /**
     * 发送一个 GET 请求
     *
     * @param url
     * @param charset
     * @param connTimeout 建立链接超时时间,毫秒.
     * @param readTimeout 响应超时时间,毫秒.
     * @return
     * @throws ConnectTimeoutException 建立链接超时
     * @throws SocketTimeoutException  响应超时
     * @throws Exception
     */
    public static String get(String url, String charset, Integer connTimeout, Integer readTimeout)
            throws ConnectTimeoutException, SocketTimeoutException, Exception {

        HttpClient client = null;
        HttpGet get = new HttpGet(url);
        get.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        get.setHeader("Accept-Encoding", "gzip, deflate, sdch, br");
        get.setHeader("Accept-Language", "zh-CN,zh;q=0.8");
        get.setHeader("Host", "www.chinatax.com");
//        get.setHeader("Cookie","d_c0=\"AFDC8UYRdAuPTvuaLgt37jBOAwNduCJAat0=|1489547718\"; _zap=a9d23f61-4bb5-409d-b96b-b41bb3639f6f; q_c1=1e36e2d349a04f959e248c501ed670fc|1492646232000|1492646232000; capsion_ticket=\"2|1:0|10:1492646553|14:capsion_ticket|44:ODY1YTNlNjY2YmU4NDc5MmE3OTAzNjA0MGZiNGFkZDI=|0de266deb5c4c1824cb64a9d5d9c2294ee3697046b7a26af491f7a47cad9021d\"; _xsrf=32a00e30e470e110d9298bf66887921d; aliyungf_tc=AQAAAGvEs3/kXAgAsj5uJAs2MlB8/O9J; acw_tc=AQAAAFJwfHLxwwoAsj5uJGsaoeaah2Ei; l_n_c=1; r_cap_id=\"NDg2NWY3NDI3MDJlNDJkNTliYjY3ZWI3ZTc1NjNhYWU=|1494988301|cc377766b8c8ece473c98f36fa7a91858a2a9d7d\"; cap_id=\"ZGU0ZDkzNjkwMWZmNDZlMzgxZTkyMmY0MTc4YTk2ZTY=|1494988301|a7672f909b4af0edd385f0825b342f19801f21b2\"; l_cap_id=\"ZTZjNmQxMTRlMTlhNGE1Y2ExMGM5ODYzZjk1ZWFmMTQ=|1494988301|db56cbe4cea7ef86435384ee166b26dd6e26ffe9\"; n_c=1");
        get.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36");
        String result = "";
        try {
            // 设置参数
            Builder customReqConf = RequestConfig.custom();
            if (connTimeout != null) {
                customReqConf.setConnectTimeout(connTimeout);
            }
            if (readTimeout != null) {
                customReqConf.setSocketTimeout(readTimeout);
            }
            get.setConfig(customReqConf.build());

            HttpResponse res = null;

            if (url.startsWith("https")) {
                // 执行 Https 请求.
                client = createSSLInsecureClient();
                res = client.execute(get);
            } else {
                // 执行 Http 请求.
                client = HttpClientUtils.client;
                res = client.execute(get);
            }

            result = IOUtils.toString(res.getEntity().getContent(), charset);
        } finally {
            get.releaseConnection();
            if (url.startsWith("https") && client != null && client instanceof CloseableHttpClient) {
                ((CloseableHttpClient) client).close();
            }
        }
        return result;
    }

    /**
     * 发送一个 GET 请求
     *
     * @param url
     *
     * @return
     * @throws ConnectTimeoutException 建立链接超时
     * @throws SocketTimeoutException  响应超时
     * @throws Exception
     */
    public static String get(String url, Map<String, String> headers, ProxyIp proxyIp)
            throws ConnectTimeoutException, SocketTimeoutException, Exception {

       Integer connTimeout =60000;
       Integer readTimeout =60000;
        HttpClient client = null;
        HttpGet get = new HttpGet(url);

        for (Map.Entry<String, String> header : headers.entrySet()) {
            get.setHeader(header.getKey(), header.getValue());
        }
        String result = "";
        try {
            // 设置参数
            Builder customReqConf = RequestConfig.custom();
            if (connTimeout != null) {
                customReqConf.setConnectTimeout(connTimeout);
            }
            if (readTimeout != null) {
                customReqConf.setSocketTimeout(readTimeout);
            }

            //设置代理IP
            HttpHost proxy = null;
            try {
                proxy = new HttpHost(proxyIp.getIp(), Integer.parseInt(proxyIp.getPort()));
            } catch (IllegalArgumentException e) {
//                return null;
            }
            if (proxyIp != null) {
                customReqConf.setProxy(proxy);
            }

            get.setConfig(customReqConf.build());

            HttpResponse res = null;

            if (url.startsWith("https")) {
                // 执行 Https 请求.
                client = createSSLInsecureClient();
                res = client.execute(get);
            } else {
                // 执行 Http 请求.
                client = HttpClientUtils.client;
                res = client.execute(get);
            }

            result = IOUtils.toString(res.getEntity().getContent(), charset);
        } finally {
            get.releaseConnection();
            if (url.startsWith("https") && client != null && client instanceof CloseableHttpClient) {
                ((CloseableHttpClient) client).close();
            }
        }
        return result;
    }

    /**
     * 发送一个 GET 请求
     *
     * @param url
     * @param charset
     * @param connTimeout 建立链接超时时间,毫秒.
     * @param readTimeout 响应超时时间,毫秒.
     * @return
     * @throws ConnectTimeoutException 建立链接超时
     * @throws SocketTimeoutException  响应超时
     * @throws Exception
     */
    public static String getPic(String url, String charset, Integer connTimeout, Integer readTimeout)
            throws ConnectTimeoutException, SocketTimeoutException, Exception {

        HttpClient client = null;
        HttpGet get = new HttpGet(url);
//        Accept:text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8
//Accept-Encoding:gzip, deflate, sdch
//Accept-Language:zh-CN,zh;q=0.8
//Cache-Control:max-age=0
//Connection:keep-alive
//Cookie:JSESSIONID=F32DB474C1C8241E6CC43940F49C3356; _gscbrs_2025930969=1; _gscu_2025930969=967162265rhn2718
//Host:shixin.court.gov.cn
//Upgrade-Insecure-Requests:1
//User-Agent:Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36
        get.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        get.setHeader("Accept-Encoding", "gzip, deflate, sdch");
        get.setHeader("Accept-Language", "zh-CN,zh;q=0.8");
        get.setHeader("Host", "shixin.court.gov.cn");
//        get.setHeader("Cookie","d_c0=\"AFDC8UYRdAuPTvuaLgt37jBOAwNduCJAat0=|1489547718\"; _zap=a9d23f61-4bb5-409d-b96b-b41bb3639f6f; q_c1=1e36e2d349a04f959e248c501ed670fc|1492646232000|1492646232000; capsion_ticket=\"2|1:0|10:1492646553|14:capsion_ticket|44:ODY1YTNlNjY2YmU4NDc5MmE3OTAzNjA0MGZiNGFkZDI=|0de266deb5c4c1824cb64a9d5d9c2294ee3697046b7a26af491f7a47cad9021d\"; _xsrf=32a00e30e470e110d9298bf66887921d; aliyungf_tc=AQAAAGvEs3/kXAgAsj5uJAs2MlB8/O9J; acw_tc=AQAAAFJwfHLxwwoAsj5uJGsaoeaah2Ei; l_n_c=1; r_cap_id=\"NDg2NWY3NDI3MDJlNDJkNTliYjY3ZWI3ZTc1NjNhYWU=|1494988301|cc377766b8c8ece473c98f36fa7a91858a2a9d7d\"; cap_id=\"ZGU0ZDkzNjkwMWZmNDZlMzgxZTkyMmY0MTc4YTk2ZTY=|1494988301|a7672f909b4af0edd385f0825b342f19801f21b2\"; l_cap_id=\"ZTZjNmQxMTRlMTlhNGE1Y2ExMGM5ODYzZjk1ZWFmMTQ=|1494988301|db56cbe4cea7ef86435384ee166b26dd6e26ffe9\"; n_c=1");
        get.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36");
        String result = "";
        try {
            // 设置参数
            Builder customReqConf = RequestConfig.custom();
            if (connTimeout != null) {
                customReqConf.setConnectTimeout(connTimeout);
            }
            if (readTimeout != null) {
                customReqConf.setSocketTimeout(readTimeout);
            }
            get.setConfig(customReqConf.build());

            HttpResponse res = null;

            if (url.startsWith("https")) {
                // 执行 Https 请求.
                client = createSSLInsecureClient();
                res = client.execute(get);
            } else {
                // 执行 Http 请求.
                client = HttpClientUtils.client;
                res = client.execute(get);
            }

            HttpEntity entity = res.getEntity();
            InputStream instream = entity.getContent();
            OutputStream outstream = new FileOutputStream(new File(DOWNLOAD_DIR, System.currentTimeMillis() + ".jpg"));
            int l = -1;
            byte[] tmp = new byte[2048];
            while ((l = instream.read(tmp)) != -1) {
                outstream.write(tmp);
            }
            outstream.close();

//            result = IOUtils.toString(res.getEntity().getContent(), charset);
        } finally {
            get.releaseConnection();
            if (url.startsWith("https") && client != null && client instanceof CloseableHttpClient) {
                ((CloseableHttpClient) client).close();
            }
        }
        return result;
    }


    /**
     * 从 response 里获取 charset
     *
     * @param ressponse
     * @return
     */
    @SuppressWarnings("unused")
    private static String getCharsetFromResponse(HttpResponse ressponse) {
        // Content-Type:text/html; charset=GBK
        if (ressponse.getEntity() != null && ressponse.getEntity().getContentType() != null && ressponse.getEntity().getContentType().getValue() != null) {
            String contentType = ressponse.getEntity().getContentType().getValue();
            if (contentType.contains("charset=")) {
                return contentType.substring(contentType.indexOf("charset=") + 8);
            }
        }
        return null;
    }


    /**
     * 创建 SSL连接
     *
     * @return
     * @throws GeneralSecurityException
     */
    private static CloseableHttpClient createSSLInsecureClient() throws GeneralSecurityException {
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();

            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, new X509HostnameVerifier() {

                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }

                @Override
                public void verify(String host, SSLSocket ssl)
                        throws IOException {
                }

                @Override
                public void verify(String host, X509Certificate cert)
                        throws SSLException {
                }

                @Override
                public void verify(String host, String[] cns,
                                   String[] subjectAlts) throws SSLException {
                }

            });

            return HttpClients.custom().setSSLSocketFactory(sslsf).build();

        } catch (GeneralSecurityException e) {
            throw e;
        }
    }

    public static void main(String[] args) {
        try {
//            String str= post("https://localhost:443/ssl/test.shtml","name=12&page=34","application/x-www-form-urlencoded", "UTF-8", 10000, 10000);
//            String str= get("https://www.zhihu.com/r/search?q=%E9%99%95%E8%A5%BF%E6%8D%B7%E9%82%A6%E8%BD%AF%E4%BB%B6%E6%8A%80%E6%9C%AF%E5%BC%80%E5%8F%91%E6%9C%89%E9%99%90%E8%B4%A3%E4%BB%BB%E5%85%AC%E5%8F%B8&correction=0&type=people&offset=10","UTF-8");
            Map<String, String> params = new HashMap<>();
//            articleField01:
//            articleField02:
//            articleField03:2016
//            articleField06:
//            taxCode:120000
//            cPage:
//            randCode:24ve8laj
//            flag:1
            params.put("articleField01", "");
            params.put("articleField02", "");
            params.put("articleField03", "2016");
            params.put("articleField06", "");
            params.put("taxCode", "110000");
            params.put("cPage", "1");
            params.put("randCode", "4ky6q839");
            params.put("flag", "1");
            Map<String, String> headers = new HashMap<>();
//            Host:hd.chinatax.gov.cn
//            Origin:http://hd.chinatax.gov.cn
//            Referer:http://hd.chinatax.gov.cn/fagui/action/InitCredit.do
//            Accept:text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8
//Accept-Encoding:gzip, deflate
//Accept-Language:zh-CN,zh;q=0.8
//Cache-Control:max-age=0
//Connection:keep-alive
//Content-Length:114
//Content-Type:application/x-www-form-urlencoded
//            User-Agent:Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.put("Accept-Encoding", "gzip, deflate");
            headers.put("Accept-Language", "zh-CN,zh;q=0.8");
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36");
            headers.put("Host", "hd.chinatax.gov.cn");
            headers.put("Origin", "http://hd.chinatax.gov.cn");
            headers.put("Referer", "http://hd.chinatax.gov.cn/fagui/action/InitCredit.do");
            String str = postForm("http://hd.chinatax.gov.cn/fagui/action/InitCredit.do", params, headers, 10000, 10000);
            System.out.println(str);
        } catch (ConnectTimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SocketTimeoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
