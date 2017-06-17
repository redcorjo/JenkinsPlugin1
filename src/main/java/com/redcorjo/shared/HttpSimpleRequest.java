package com.redcorjo.shared;

//import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import static org.apache.http.HttpHeaders.USER_AGENT;

import com.redcorjo.shared.SharedAPIs;


// Intersting URL for testing http://www.jsontest.com
/**
 * Created by jordiredondo on 20/05/2017.
 */
public class HttpSimpleRequest {

    private String url;
    private String result;
    private String headers;
    private String parameters;
    private JSONObject resultjson;
    private int code;
    private String proxyhost;
    private int proxyport;
    private String noProxyHost;
    private String user;
    private String password;
    private String encodedcredentials;
    private int method;
    private String [] files;
    public final int GET = 1;
    public final int POST = 2;
    public final int PUT = 3;
    public final int DELETE = 4;

    private SharedAPIs myapis = new SharedAPIs();

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

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    private String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMethod() {
        return method;
    }

    public void setMethod(int method) {
        this.method = method;
    }

    private String getEncodedcredentials() {
        return encodedcredentials;
    }

    private void setEncodedcredentials(String encodedcredentials) {
        this.encodedcredentials = encodedcredentials;
    }

    public void setFiles(String[] files) {
        this.files = files;
    }

    private RequestConfig setMyProxy(){
        RequestConfig config = null;
        if (proxyhost != null) {
            HttpHost proxy = new HttpHost(proxyhost, proxyport, "http");
            //RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
            config = RequestConfig.custom().setProxy(proxy).build();
            myapis.logger("Using proxy: " + proxyhost + ":" + proxyport);
            return config;
        }
        return config;
    }

    private URIBuilder setMyParameters() throws MalformedURLException
    {
        URL myurl = new URL(url);
        URIBuilder builder = new URIBuilder();
        builder.setScheme(myurl.getProtocol()).setHost(myurl.getHost()).setPort(myurl.getPort()).setPath(myurl.getPath());
        if ( getMethod() == this.POST) {
            return builder;
        }
        if (! parameters.isEmpty()) {
            JSONObject myparams = new JSONObject(parameters);
            Iterator<?> paramskeys = myparams.keys();
            while (paramskeys.hasNext()) {
                String myparam = (String) paramskeys.next();
                String myvalue = myparams.getString(myparam);
                builder.addParameter(myparam, myvalue);
                myapis.logger("Parameter " + myparam + ":" + myvalue);
            }
        }
        return builder;
    }

    private String buildMyResponse(HttpResponse response) throws IOException{
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
            myapis.logger("String JSON Compatible");
        } catch (JSONException e){
            setResultjson(null);
            myapis.logger("String NOT JSON Compatible");
        }

        return result.toString();
    }

    public String myRequest() throws IOException{

        if ( getMethod() == this.GET) {
            myapis.logger("Using GET method");
            return myRequestGet();
        } else if ( getMethod() == this.POST) {
            myapis.logger("Using POST method");
            return myRequestPost();
        } else if ( getMethod() == this.PUT) {
            myapis.logger("Using PUT method");
            return myRequestPut();
        } else if ( getMethod() == this.DELETE) {
            myapis.logger("Using DELETE method");
            return myRequestDelete();
        } else {
            myapis.logger("Using GET default method");
            return myRequestGet();
        }
    }

    public String myRequest(String[] files) throws IOException{
        this.files = files;
        return this.myRequest();
    }

    public String myRequestGet() throws IOException{

        HttpClient client = HttpClientBuilder.create().build();

        HttpGet request = new HttpGet();

        if (proxyhost != null) {
            request.setConfig(setMyProxy());
        }

        // add request header
        request.addHeader("User-Agent", USER_AGENT);
        JSONObject myheaders = new JSONObject(headers);
        Iterator<?> headerkeys = myheaders.keys();
        while (headerkeys.hasNext()){
            String myheader = (String)headerkeys.next();
            String myvalue = myheaders.getString(myheader);
            request.addHeader(myheader, myvalue);
            myapis.logger("Header "+myheader+":"+myvalue);
        }


        URIBuilder builder = setMyParameters();


        URI uri;
        try {
            uri = builder.build();
            myapis.logger("URL:" + uri.toString());
        } catch (Exception e){
            this.code = 500;
            this.result = "";
            return this.result;
        }

        request.setURI(uri);

        HttpResponse response;
        try {
            response = client.execute(request);
        } catch (Exception e){
            this.code = 500;
            this.result = "";
            return this.result;
        }

        if (response.getStatusLine().getStatusCode() == 401){
            if ( user != null && password != null ) {
                request.addHeader("Authorization", getEncodedcredentials());
            } else {
                this.code = response.getStatusLine().getStatusCode();
                this.result = "Authentication failed";
                return this.result;
            }
            try {
                response = client.execute(request);
            } catch (Exception e){
                this.code = 500;
                this.result = "";
                return this.result;
            }
        }

        return buildMyResponse(response);
    }

    public String myRequestPost() throws IOException{

        HttpClient client = HttpClientBuilder.create().build();

        HttpPost request = new HttpPost();

        if (proxyhost != null) {
            request.setConfig(setMyProxy());
        }

        // add request header
        request.addHeader("User-Agent", USER_AGENT);
        JSONObject myheaders = new JSONObject(headers);
        Iterator<?> headerkeys = myheaders.keys();
        while (headerkeys.hasNext()){
            String myheader = (String)headerkeys.next();
            String myvalue = myheaders.getString(myheader);
            request.addHeader(myheader, myvalue);
            myapis.logger("Header "+myheader+":"+myvalue);
            myapis.logger("Header "+myheader+":"+myvalue);
        }

        JSONObject myparameters = new JSONObject(parameters);
        Iterator<?> parameterkeys = myparameters.keys();
        List<BasicNameValuePair> postParameters = new ArrayList<>();
        while (parameterkeys.hasNext()){
            String myparameter = (String)parameterkeys.next();
            String myvalue = myparameters.getString(myparameter);
            postParameters.add(new BasicNameValuePair(myparameter, myvalue));

        }
        request.setEntity(new UrlEncodedFormEntity(postParameters));

        URIBuilder builder = setMyParameters();


        URI uri;
        try {
            uri = builder.build();
            myapis.logger("URL:" + uri.toString());
        } catch (Exception e){
            this.code = 500;
            this.result = "";
            return this.result;
        }

        request.setURI(uri);
        if ( this.files != null) {
            MultipartEntityBuilder multipart = attachFiles();
            request.setEntity(multipart.build());
        }

        HttpResponse response;
        HttpEntity entity;
        try {
            response = client.execute(request);
            entity = response.getEntity();
        } catch (Exception e){
            this.code = 500;
            this.result = "Error";
            return this.result;
        }

        if (response.getStatusLine().getStatusCode() == 401){
            if ( user != null && password != null ) {
                request.addHeader("Authorization", getEncodedcredentials());
            } else {
                this.code = response.getStatusLine().getStatusCode();
                this.result = "Authentication failed";
                return this.result;
            }
            try {
                response = client.execute(request);
                entity = response.getEntity();
            } catch (Exception e){
                this.code = 500;
                this.result = "Error";
                return this.result;
            }
        }
        return buildMyResponse(response);
    }

    private MultipartEntityBuilder attachFiles(){
        MultipartEntityBuilder builderfile = null;
        if (this.files != null) {
            builderfile = MultipartEntityBuilder.create();
            builderfile.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            for (String myfile: files){
                final File file = new File(myfile);
                FileBody fb = new FileBody(file);
                builderfile.addPart(Paths.get(myfile).getFileName().toString(), fb);
                myapis.logger("Adding file to upload: " + myfile);
            }
        }
        return builderfile;
    }

    public String myRequestPut() throws IOException{

        HttpClient client = HttpClientBuilder.create().build();

        HttpPut request = new HttpPut();

        if (proxyhost != null) {
            request.setConfig(setMyProxy());
        }

        // add request header
        request.addHeader("User-Agent", USER_AGENT);
        JSONObject myheaders = new JSONObject(headers);
        Iterator<?> headerkeys = myheaders.keys();
        while (headerkeys.hasNext()){
            String myheader = (String)headerkeys.next();
            String myvalue = myheaders.getString(myheader);
            request.addHeader(myheader, myvalue);
            myapis.logger("Header "+myheader+":"+myvalue);
        }

        URIBuilder builder = setMyParameters();

        URI uri;
        try {
            uri = builder.build();
            myapis.logger("URL:" + uri.toString());
        } catch (Exception e){
            this.code = 500;
            this.result = "";
            return this.result;
        }

        request.setURI(uri);

        HttpResponse response;
        try {
            response = client.execute(request);
        } catch (Exception e){
            this.code = 500;
            this.result = "";
            return this.result;
        }

        if (response.getStatusLine().getStatusCode() == 401){
            if ( user != null && password != null ) {
                request.addHeader("Authorization", getEncodedcredentials());
            } else {
                this.code = response.getStatusLine().getStatusCode();
                this.result = "Authentication failed";
                return this.result;
            }
            try {
                response = client.execute(request);
            } catch (Exception e){
                this.code = 500;
                this.result = "";
                return this.result;
            }
        }

        return buildMyResponse(response);
    }

    public String myRequestDelete() throws IOException{

        HttpClient client = HttpClientBuilder.create().build();

        HttpDelete request = new HttpDelete();

        if (proxyhost != null) {
            request.setConfig(setMyProxy());
        }

        // add request header
        request.addHeader("User-Agent", USER_AGENT);
        JSONObject myheaders = new JSONObject(headers);
        Iterator<?> headerkeys = myheaders.keys();
        while (headerkeys.hasNext()){
            String myheader = (String)headerkeys.next();
            String myvalue = myheaders.getString(myheader);
            request.addHeader(myheader, myvalue);
            myapis.logger("Header "+myheader+":"+myvalue);
        }

        URIBuilder builder = setMyParameters();

        URI uri;
        try {
            uri = builder.build();
            myapis.logger("URL:" + uri.toString());
        } catch (Exception e){
            this.code = 500;
            this.result = "";
            return this.result;
        }

        request.setURI(uri);

        HttpResponse response;
        try {
            response = client.execute(request);
        } catch (Exception e){
            this.code = 500;
            this.result = "";
            return this.result;
        }

        if (response.getStatusLine().getStatusCode() == 401){
            if ( user != null && password != null ) {
                request.addHeader("Authorization", getEncodedcredentials());
            } else {
                this.code = response.getStatusLine().getStatusCode();
                this.result = "Authentication failed";
                return this.result;
            }
            try {
                response = client.execute(request);
            } catch (Exception e){
                this.code = 500;
                this.result = "";
                return this.result;
            }
        }

        return buildMyResponse(response);
    }

    JSONObject convertToJson(String mystring){
        JSONObject jsonObj = new JSONObject(mystring);
        return jsonObj;
    }

    public void setCredentials(String user, String password){
        setUser(user);
        setPassword(password);
        final String credentials = user+":"+password;
        final byte[] encodedBytes = Base64.encodeBase64(credentials.getBytes());
        final String encodedcredentials = "Basic " + encodedBytes.toString();
        setEncodedcredentials(encodedcredentials);
    }

    public static void main(String[] args){

        SharedAPIs myapis = new SharedAPIs();
        String myresult = null;
        myapis.logger("args = [" + args + "]");
        String myurl = "http://www.google.com/search?q=httpClient";
        //myurl = "http://ip.jsontest.com/";
        HttpSimpleRequest myrequest = new HttpSimpleRequest(myurl);

        try {
            //myresult = myrequest.myRequest();
            myrequest.myRequest();
        } catch (IOException e) {
            e.printStackTrace();
        }
        myapis.logger("Myresult:" + myrequest.getResult());
        myapis.logger("Mycode:" + myrequest.getCode());
    }
}
