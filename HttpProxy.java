package com.yidianzixun.util;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HttpProxy {
    // 代理服务器
    final static String proxyHost = "proxy.abuyun.com";
    final static Integer proxyPort = 9020;

    // 代理隧道验证信息
    final static String proxyUser = "HL9946L10868L0ID";
    final static String proxyPass = "A329CEFABD0A7067";

    // IP切换协议头
    final static String switchIpHeaderKey = "Proxy-Switch-Ip";
    final static String switchIpHeaderVal = "yes";

    private static PoolingHttpClientConnectionManager cm = null;
    private static HttpRequestRetryHandler httpRequestRetryHandler = null;
    private static HttpHost proxy = null;

    private static CredentialsProvider credsProvider = null;
    private static RequestConfig reqConfig = null;

    static {
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();

        Registry registry = RegistryBuilder.create()
                .register("http", plainsf)
                .register("https", sslsf)
                .build();

        cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(20);
        cm.setDefaultMaxPerRoute(5);

        proxy = new HttpHost(proxyHost, proxyPort, "http");

        credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(proxyUser, proxyPass));

        reqConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(20000)
                .setConnectTimeout(20000)
                .setSocketTimeout(20000)
                .setExpectContinueEnabled(false)
                .setProxy(new HttpHost(proxyHost, proxyPort))
                .build();
    }

    public static String doRequest(HttpRequestBase httpReq) {
        CloseableHttpResponse httpResp = null;

        int statusCode = 0;
        try {
            setHeaders(httpReq);

            httpReq.setConfig(reqConfig);

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(cm)
                    .setDefaultCredentialsProvider(credsProvider)
                    .build();

            AuthCache authCache = new BasicAuthCache();
            authCache.put(proxy, new BasicScheme());

            HttpClientContext localContext = HttpClientContext.create();
            localContext.setAuthCache(authCache);

            httpResp = httpClient.execute(httpReq, localContext);

            statusCode = httpResp.getStatusLine().getStatusCode();

            HttpEntity entity = httpResp.getEntity();
            String jsonStr = EntityUtils.toString(entity);

            if (statusCode == 200) return jsonStr;
            else return null;

        } catch (Exception e) {
            System.out.println("当前 statusCode --> " + statusCode);
            System.out.println("ssl问题，重跑一次");
            return doRequest(httpReq);
        } finally {
            try {

                if (httpResp != null) {
                    httpResp.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置请求头
     *
     * @param httpReq
     */
    private static void setHeaders(HttpRequestBase httpReq) {
        httpReq.setHeader("Accept-Encoding", null);
        httpReq.setHeader(switchIpHeaderKey, switchIpHeaderVal);
    }

    public static void doPostRequest() {
        try {
            // 要访问的目标页面
            HttpPost httpPost = new HttpPost("https://test.abuyun.com/proxy.php");

            // 设置表单参数
            List params = new ArrayList();
            params.add(new BasicNameValuePair("method", "next"));
            params.add(new BasicNameValuePair("params", "{\"broker\":\"abuyun\":\"site\":\"https://www.abuyun.com\"}"));

            httpPost.setEntity(new UrlEncodedFormEntity(params, "utf-8"));

            doRequest(httpPost);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String doGetRequest(String targetUrl) {
        // 要访问的目标页面
        //String targetUrl = "https://api.douban.com/v2/book/search?tag=小说&start="+start+"&count=100";
        //String targetUrl = "http://proxy.abuyun.com/switch-ip";
        //String targetUrl = "http://proxy.abuyun.com/current-ip";

        try {
            HttpGet httpGet = new HttpGet(targetUrl);

            return doRequest(httpGet);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
