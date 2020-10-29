package com.zkt.dcs.common.util;

import com.zkt.common.exception.BusinessException;
import com.zkt.dcs.common.enums.RespEnum;
import com.zkt.log.LogUtil;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;

public class HttpClientUtil {
    private static PoolingHttpClientConnectionManager cm;
    private static final String EMPTY_STR = "";
    private static final String UTF8 = "UTF-8";
    private static Integer connectTimeout = 10000;
    private static Integer connectionRequestTimeout = 3000;
    private static Integer socketTimeout = 300000;
    /**
     * 初始化超时时间
     *
     * @param connectTimeout           设置创建连接的最长时间,默认10000
     * @param connectionRequestTimeout 从连接池中获取到连接的最长时间,默认3000
     * @param socketTimeout            数据传输的最长时间,默认30000
     */
    public static void initTimeout(Integer connectTimeout, Integer connectionRequestTimeout, Integer socketTimeout) {
        if (connectTimeout != 0) {
            HttpClientUtil.connectTimeout = connectTimeout;
        }
        if (connectionRequestTimeout != 0) {
            HttpClientUtil.connectionRequestTimeout = connectionRequestTimeout;
        }
        if (socketTimeout != 0) {
            HttpClientUtil.socketTimeout = socketTimeout;
        }
    }

    /**
     * 初始化线程池
     */
    private static void init() {
        if (cm == null) {
            cm = new PoolingHttpClientConnectionManager();
            // 整个连接池最大连接数
            cm.setMaxTotal(500);
            // 每路由最大连接数，默认值是2
            cm.setDefaultMaxPerRoute(100);
        }
    }

    /**
     * 通过线程池获取HttpClient
     *
     * @return
     */
    private static CloseableHttpClient getHttpClient() {
        init();
        return HttpClients.custom().setConnectionManager(cm).build();
    }

    /**
     * 设置请求超时时间
     *
     * @param request
     */
    private static void setRequestConfig(HttpRequestBase request) {
        RequestConfig requestConfig = RequestConfig
                .custom()
                .setConnectTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .setSocketTimeout(socketTimeout)
                .build();
        request.setConfig(requestConfig);
    }

    /**
     * 处理Http请求
     *
     * @param request
     * @return
     */
    private static String getResult(HttpRequestBase request, Map<String, Object> headers) {
        CloseableHttpClient httpClient = getHttpClient();
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, Object> param : headers.entrySet()) {
                request.addHeader(param.getKey(), String.valueOf(param.getValue()));
            }
        }
        setRequestConfig(request);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return EntityUtils.toString(entity);
            }
        } catch (IOException e) {
            Throwable throwable = ExceptionUtil.getEarmarkCause(e, SocketTimeoutException.class);
            if (throwable instanceof SocketTimeoutException) {
                LogUtil.logApplicationError(RespEnum.NETWORK_TIMEOUT.getMsg(), e);
                throw new BusinessException(RespEnum.NETWORK_TIMEOUT.getCode());
            } else {
                LogUtil.logApplicationError(RespEnum.NETWORK_EXCEPTION.getMsg(), e);
                throw new BusinessException(RespEnum.NETWORK_EXCEPTION.getCode());
            }
        }
        return EMPTY_STR;
    }

    private static ArrayList<NameValuePair> covertParams2NVPS(Map<String, Object> params) {
        ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();
        for (Map.Entry<String, Object> param : params.entrySet()) {
            pairs.add(new BasicNameValuePair(param.getKey(), String.valueOf(param.getValue())));
        }
        return pairs;
    }

    /**
     * GET请求
     *
     * @param url
     * @return
     */
    public static String doGet(String url) {
        HttpGet httpGet = new HttpGet(url);
        return getResult(httpGet, null);
    }

    /**
     * GET请求
     *
     * @param url
     * @param params
     * @return
     * @throws URISyntaxException
     */
    public static String doGet(String url, Map<String, Object> params) {
        return doGet(url, params, null);
    }

    public static String doGet(String url, Map<String, Object> params, Map<String, Object> headers) {
        try {
            URIBuilder ub = new URIBuilder();
            ub.setPath(url);

            ArrayList<NameValuePair> pairs = covertParams2NVPS(params);
            ub.setParameters(pairs);

            HttpGet httpGet = new HttpGet(ub.build());

            return getResult(httpGet, headers);
        } catch (Exception e) {
            LogUtil.logApplicationError(RespEnum.NETWORK_EXCEPTION.getMsg(), e);
            throw new BusinessException(RespEnum.NETWORK_EXCEPTION.getCode());
        }
    }

    public static String doPost(String url) {
        HttpPost httpPost = new HttpPost(url);
        return getResult(httpPost, null);
    }

    public static String doPost(String url, Map<String, Object> params) {
        return doPost(url, params, null);
    }

    public static String doPost(String url, Map<String, Object> params, Map<String, Object> headers) {
        try {
            HttpPost httpPost = new HttpPost(url);

            ArrayList<NameValuePair> pairs = covertParams2NVPS(params);

            httpPost.setEntity(new UrlEncodedFormEntity(pairs, UTF8));

            return getResult(httpPost, headers);
        } catch (Exception e) {
            LogUtil.logApplicationError(RespEnum.NETWORK_EXCEPTION.getMsg(), e);
            throw new BusinessException(RespEnum.NETWORK_EXCEPTION.getCode());
        }
    }

    public static String doJsonPost(String url, String jsonStr) {
        return HttpClientUtil.doJsonPost(url, jsonStr, null);
    }

    public static String doJsonPost(String url, String jsonStr, Map<String, Object> headers) {
        HttpPost httpPost = new HttpPost(url);

        StringEntity stringEntity = new StringEntity(jsonStr, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);

        return getResult(httpPost, headers);
    }

    public static String doJsonPut(String url, String jsonStr, Map<String, Object> headers) {
        HttpPut httpPut = new HttpPut(url);

        StringEntity stringEntity = new StringEntity(jsonStr, ContentType.APPLICATION_JSON);
        httpPut.setEntity(stringEntity);

        return getResult(httpPut, headers);
    }

    public static void main(String[] args) {
        String s =  HttpClientUtil.doGet("http://test5-bg-api-gateway.zhiketong.net/dcs-api/swagger-ui.html");
        System.out.println(s);
    }
}
