package com.archean.jtradeapi;

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
            super(message);
        }
    }

    public static final class Constants {
        public static final int ORDER_BUY = 0;
        public static final int ORDER_SELL = 1;
        public static final int REQUEST_GET = 0;
        public static final int REQUEST_POST = 1;
        protected static final String JsonDateFormat = "yyyy-MM-dd HH:mm:ss";
    }

    public static class RequestSender {
        String requestEncoding = "UTF-8";

        String formatGetParamString(List<NameValuePair> urlParameters) {
            String url = "";
            boolean firstParam = true;
            for (NameValuePair entry : urlParameters) { // Adding fields
                if (!firstParam) url = url + "&";
                else firstParam = false;
                try {
                    url = url + entry.getName() + "=" + URLEncoder.encode(entry.getValue(), requestEncoding);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            return url;
        }

        public HttpResponse getRequest(String url, List<NameValuePair> urlParameters, List<NameValuePair> httpHeaders) throws IOException {
            url = url + "?" + formatGetParamString(urlParameters);

            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(url);
            for (NameValuePair header : httpHeaders) { // Adding headers
                request.addHeader(header.getName(), header.getValue());
            }

            return client.execute(request);
        }

        public HttpResponse postRequest(String url, List<NameValuePair> urlParameters, List<NameValuePair> httpHeaders) throws IOException {
            url = url + "?";
            HttpClient client = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(url);
            request.setEntity(new UrlEncodedFormEntity(urlParameters));

            for (NameValuePair header : httpHeaders) { // Adding headers
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

        @Override
        public String toString() {
            return "Public=" + publicKey + "; Private=" + privateKey;
        }
    }

    class ApiStatus<ReturnType> {
        int success; // 0 - error, 1 - success
        String error; // Error message
        @SerializedName("return")
        ReturnType result; // Response
    }

    public static class StandartObjects { // Unified, api-independent objects

        public static class Prices {
            public double average;
            public double low;
            public double high;
            public double sell;
            public double buy;
            public double last;
            public double volume;
        }

        public static class Order {
            public Order() {
                super();
            }

            public Order(double price, double amount) {
                this.price = price;
                this.amount = amount;
            }

            public long id;
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

        public static class CurrencyPair {
            public String pairName;
            public Object pairId;
            public String firstCurrency;
            public String secondCurrency;
        }

        public static class MarketInfo extends CurrencyPair {
            public Prices price = new Prices();
            public Depth depth = new Depth();
            public List<Order> history = new ArrayList<Order>();
        }

        public static class AccountInfo {
            public static class AccountBalance extends TreeMap<String, Double> {
                public AccountBalance() {
                    super();
                }

                public AccountBalance(TreeMap<String, Double> stringDoubleTreeMap) {
                    super(stringDoubleTreeMap);
                }

                public double getBalance(String currencyName) {
                    return this.containsKey(currencyName) ? get(currencyName) : 0.0;
                }
            }

            public AccountBalance balance = new AccountBalance();
            public List<Order> orders = new ArrayList<Order>();
            public List<Order> history = new ArrayList<Order>();
        }

        public static class CurrencyPairMapper extends TreeMap<Object, CurrencyPair> {
            public CurrencyPairMapper() {
                super();
            }

            public Map<String, CurrencyPair> makeNameInfoMap() {
                Map<String, CurrencyPair> nameKeyMap = new TreeMap<String, CurrencyPair>();
                for (Map.Entry<Object, CurrencyPair> entry : this.entrySet()) {
                    nameKeyMap.put(entry.getValue().pairName, entry.getValue());
                }
                return nameKeyMap;
            }
        }
    }


    public ApiKeyPair apiKeyPair;
    protected RequestSender requestSender;
    protected Gson jsonParser;
    protected int nonce = 0;

    protected void addNonce(List<NameValuePair> urlParameters) {
        nonce++;
        urlParameters.add(new BasicNameValuePair("nonce", Long.toString(System.currentTimeMillis() / 100 + nonce)));
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
        return Utils.Crypto.Hashing.hmacDigest(requestSender.formatGetParamString(urlParameters), apiKeyPair.privateKey, Utils.Crypto.Hashing.SHA512);
    }

    protected void cleanAuth(List<NameValuePair> urlParameters, List<NameValuePair> httpHeaders) {
        Iterator<NameValuePair> headerIterator = httpHeaders.iterator();
        while (headerIterator.hasNext()) { // Cleaning
            NameValuePair header = headerIterator.next();
            if (header.getName().equals("Key") || header.getName().equals("Sign")) {
                headerIterator.remove();
            }
        }
        Iterator<NameValuePair> paramsIterator = urlParameters.iterator();
        while (paramsIterator.hasNext()) { // Cleaning
            NameValuePair header = paramsIterator.next();
            if (header.getName().equals("nonce")) {
                paramsIterator.remove();
            }
        }
    }

    protected void writeAuthParams(List<NameValuePair> urlParameters, List<NameValuePair> httpHeaders) {
        if (apiKeyPair == null || apiKeyPair.publicKey.isEmpty() || apiKeyPair.privateKey.isEmpty()) {
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
        if (urlParameters == null) urlParameters = new ArrayList<NameValuePair>(); // empty list
        List<NameValuePair> httpHeaders = new ArrayList<>();
        cleanAuth(urlParameters, httpHeaders);
        if (needAuth) writeAuthParams(urlParameters, httpHeaders);
        switch (httpRequestType) {
            case Constants.REQUEST_GET:
                return requestSender.getResponseString(requestSender.getRequest(url, urlParameters, httpHeaders));
            case Constants.REQUEST_POST:
                return requestSender.getResponseString(requestSender.postRequest(url, urlParameters, httpHeaders));
            default:
                throw new IllegalArgumentException("Unknown httpRequestType value");
        }
    }

    public abstract StandartObjects.CurrencyPairMapper getCurrencyPairs() throws Exception;

    // Basic info:
    public abstract StandartObjects.Prices getMarketPrices(Object pair) throws Exception;

    public abstract StandartObjects.Depth getMarketDepth(Object pair) throws Exception;

    public abstract List<StandartObjects.Order> getMarketHistory(Object pair) throws Exception;

    public abstract StandartObjects.AccountInfo.AccountBalance getAccountBalances() throws Exception;

    public abstract List<StandartObjects.Order> getAccountOpenOrders(Object pair) throws Exception;

    public abstract List<StandartObjects.Order> getAccountHistory(Object pair) throws Exception;

    // Consolidated info:
    public abstract StandartObjects.MarketInfo getMarketData(Object pair, boolean retrieveOrders, boolean retrieveHistory) throws Exception;

    public abstract StandartObjects.AccountInfo getAccountInfo(Object pair, boolean retrieveOrders, boolean retrieveHistory) throws Exception;

    // Misc
    public abstract double getFeePercent(Object pair) throws Exception;

    // Trading api:
    public abstract long createOrder(Object pair, int orderType, double quantity, double price) throws IOException, TradeApiError;

    public abstract boolean cancelOrder(long orderId) throws Exception;
}
