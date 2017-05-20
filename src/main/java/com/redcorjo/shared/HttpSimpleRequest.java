package com.redcorjo.shared;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import static org.apache.http.HttpHeaders.USER_AGENT;

/**
 * Created by jordiredondo on 20/05/2017.
 */
public class HttpSimpleRequest {

    private String url;
    private String result;
    private String headers;
    private JSONObject resultjson;
    private int code;
    private String proxyhost;
    private int proxyport;
    private String noProxyHost;

    public HttpSimpleRequest(){
        super();
    }

    public HttpSimpleRequest(String url){
        this.url = url;
    }

    public void setProxyhost(String proxyhost) {
        this.proxyhost = proxyhost;
    }

    public void setProxyport(int proxyport) {
        this.proxyport = proxyport;
    }

    public void setNoProxyHost(String noProxyHost) {
        this.noProxyHost = noProxyHost;
    }

    public JSONObject getResultjson() {
        return resultjson;
    }

    public void setResultjson(JSONObject resultjson) {
        this.resultjson = resultjson;
    }

    public String getResult() {
        return result;
    }

    public int getCode() {
        return code;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }


    public String myRequest() throws IOException{

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);

        if (proxyhost != null) {
            HttpHost proxy = new HttpHost(proxyhost, proxyport, "http");
            RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
            request.setConfig(config);
            System.out.println("Using proxy: " + proxyhost + ":" + proxyport);
        }

        // add request header
        request.addHeader("User-Agent", USER_AGENT);
        JSONObject myheaders = new JSONObject(headers);
        Iterator<?> keys = myheaders.keys();
        while (keys.hasNext()){
            String myheader = (String)keys.next();
            String myvalue = myheaders.getString(myheader);
            request.addHeader(myheader, myvalue);
            System.out.println("Header "+myheader+":"+myvalue);
        }

        HttpResponse response = client.execute(request);


        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        this.result = result.toString();
        this.code = response.getStatusLine().getStatusCode();

        try {
            setResultjson(new JSONObject(result.toString()));
            System.out.println("String JSON Compatible");
        } catch (JSONException e){
            setResultjson(null);
            System.out.println("String NOT JSON Compatible");
        }

        return result.toString();
    }

    JSONObject convertToJson(String mystring){
        JSONObject jsonObj = new JSONObject(mystring);
        return jsonObj;
    }

    public static void main(String[] args){

        String myresult = null;
        System.out.println("args = [" + args + "]");
        String myurl = "http://www.google.com/search?q=httpClient";
        //myurl = "http://ip.jsontest.com/";
        HttpSimpleRequest myrequest = new HttpSimpleRequest(myurl);

        try {
            //myresult = myrequest.myRequest();
            myrequest.myRequest();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Myresult:" + myrequest.getResult());
        System.out.println("Mycode:" + myrequest.getCode());
    }
}
