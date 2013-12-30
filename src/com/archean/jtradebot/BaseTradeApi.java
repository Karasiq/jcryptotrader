package com.archean.jtradebot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public abstract class BaseTradeApi {
    public class TradeApiError extends Exception {
        public TradeApiError(String message) {
            super("Trading API error: " + message);
        }
    }
    public static final class Constants {
        static final int ORDER_BUY = 0;
        static final int ORDER_SELL = 1;
        static final int REQUEST_GET = 0;
        static final int REQUEST_POST = 1;
        private static final String JsonDateFormat = "yyyy-MM-dd HH:mm:ss";
    }
    protected class RequestSender {
        String requestEncoding = "UTF-8";
        List<NameValuePair> httpHeaders = new ArrayList<NameValuePair>();

        String formatGetParamString(List<NameValuePair> urlParameters) {
            String url = "";
            boolean firstParam = true;
            for(NameValuePair entry : urlParameters) { // Adding fields
                if(!firstParam) url = url + "&";
                else firstParam = false;
                try {
                    url = url + entry.getName() + "=" + URLEncoder.encode(entry.getValue(), requestEncoding);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            return url;
        }

        public HttpResponse getRequest(String url, List<NameValuePair> urlParameters) throws IOException {
            url = url + "?" + formatGetParamString(urlParameters);

            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(url);
            for(NameValuePair header : httpHeaders) { // Adding headers
                request.addHeader(header.getName(), header.getValue());
            }

            return client.execute(request);
        }
        public HttpResponse postRequest(String url, List<NameValuePair> urlParameters) throws IOException {
            url = url + "?";
            HttpClient client = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(url);
            request.setEntity(new UrlEncodedFormEntity(urlParameters));

            for(NameValuePair header : httpHeaders) { // Adding headers
                request.addHeader(header.getName(), header.getValue());
            }

            return client.execute(request);
        }
        public String getResponseString(HttpResponse response) throws IOException {
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            StringBuilder result = new StringBuilder();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        }
    }
    public static class ApiKeyPair implements Serializable {
        String publicKey;
        String privateKey;
        public ApiKeyPair() {
            // do nothing
        }
        public ApiKeyPair(String publicKey, String privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }
        public ApiKeyPair(ApiKeyPair keyPair) {
            this(keyPair.publicKey, keyPair.privateKey);
        }
        @Override public String toString() {
            return "Public=" + publicKey + "; Private=" + privateKey;
        }
    }
    class ApiStatus<ReturnType> {
        int success; // 0 - error, 1 - success
        String error; // Error message
        @SerializedName("return") ReturnType result; // Response
    }

    public static class StandartObjects { // Unified, api-independent objects
        public static class Prices {
            public double average;
            public double low;
            public double high;
            public double sell;
            public double buy;
            public double last;
        }
        public static class Order {
            public Order() {
                super();
            }
            public Order(double price, double amount) {
                this.price = price;
                this.amount = amount;
            }
            public int id;
            public Object pair;
            public double price;
            public double amount;
            public Date time;
            public int type; // ORDER_BUY/ORDER_SELL
        }
        public static class Depth {
            public List<Order> sellOrders = new ArrayList<Order>(); // Ask
            public List<Order> buyOrders = new ArrayList<Order>(); // Bid
        }
        public static class MarketInfo {
            public String pairName;
            public Object pairId;
            public Prices price = new Prices();
            public  Depth depth = new Depth();
            // public List<Order> history = new ArrayList<Order>();
        }
        public static class AccountInfo {
            public TreeMap<String, Double> balance = new TreeMap<String, Double>();
            public List<Order> orders = new ArrayList<Order>();
            // public List<Order> history = new ArrayList<Order>();
        }
    }


    ApiKeyPair apiKeyPair;
    RequestSender requestSender;
    Gson jsonParser;

    protected void addNonce(List<NameValuePair> urlParameters) {
        urlParameters.add(new BasicNameValuePair("nonce", Long.toString(System.currentTimeMillis())));
    }

    public BaseTradeApi() {
        apiKeyPair = new ApiKeyPair();
        requestSender = new RequestSender();
        jsonParser = new GsonBuilder().setDateFormat(Constants.JsonDateFormat).create();
    }

    public BaseTradeApi(ApiKeyPair apiKeyPair) {
        this();
        this.apiKeyPair = apiKeyPair;
    }

    protected String makeSign(List<NameValuePair> urlParameters) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        return Utils.Crypto.Hashing.hmacDigest(requestSender.formatGetParamString(urlParameters), apiKeyPair.privateKey, Utils.Crypto.Hashing.HMAC_SHA512);
    }

    protected void cleanAuth(List<NameValuePair> urlParameters, List<NameValuePair> httpHeaders) {
        Iterator<NameValuePair> headerIterator = httpHeaders.iterator();
        while(headerIterator.hasNext()) { // Cleaning
            NameValuePair header = headerIterator.next();
            if(header.getName().equals("Key") || header.getName().equals("Sign")) {
                headerIterator.remove();
            }
        }
        Iterator<NameValuePair> paramsIterator = urlParameters.iterator();
        while(paramsIterator.hasNext()) { // Cleaning
            NameValuePair header = paramsIterator.next();
            if(header.getName().equals("nonce")) {
                paramsIterator.remove();
            }
        }
    }
    protected void writeAuthParams(List<NameValuePair> urlParameters, List<NameValuePair> httpHeaders) {
        if(apiKeyPair == null || apiKeyPair.publicKey.isEmpty() || apiKeyPair.privateKey.isEmpty()) {
            throw new IllegalArgumentException("Invalid API key pair");
        }
        addNonce(urlParameters);
        try {
            httpHeaders.add(new BasicNameValuePair("Sign", makeSign(urlParameters)));
            httpHeaders.add(new BasicNameValuePair("Key", apiKeyPair.publicKey));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    protected String executeRequest(boolean needAuth, String url, List<NameValuePair> urlParameters, int httpRequestType) throws IOException {
        cleanAuth(urlParameters, requestSender.httpHeaders);
        if(needAuth) writeAuthParams(urlParameters, requestSender.httpHeaders);
        switch (httpRequestType) {
            case Constants.REQUEST_GET:
                return requestSender.getResponseString(requestSender.getRequest(url, urlParameters));
            case Constants.REQUEST_POST:
                return requestSender.getResponseString(requestSender.postRequest(url, urlParameters));
            default:
                throw new IllegalArgumentException("Unknown httpRequestType value");
        }
    }
    public abstract List<StandartObjects.MarketInfo> getMarketData(Object pair, boolean retrieveOrders) throws TradeApiError, IOException;
    public abstract StandartObjects.AccountInfo getAccountInfo(boolean retrieveOrders) throws TradeApiError, IOException;
    public abstract double calculateFees(int orderType, double quantity, double price) throws TradeApiError, IOException;

    public abstract int createOrder(Object pair, int orderType, double quantity, double price) throws IOException, TradeApiError;
    public abstract boolean cancelOrder(int orderId) throws TradeApiError, IOException;
}
