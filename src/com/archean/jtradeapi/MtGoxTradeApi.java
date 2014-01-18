/*
 * jCryptoTrader trading client
 * Copyright (C) 2014 1M4SKfh83ZxsCSDmfaXvfCfMonFxMa5vvh (BTC public key)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package com.archean.jtradeapi;

import com.google.gson.reflect.TypeToken;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class MtGoxTradeApi extends BaseTradeApi {
    public MtGoxTradeApi(ApiKeyPair pair) {
        super(pair);
    }

    // MtGox compatibility:
    class ApiStatus<ReturnType> {
        String result; // "success" / ???
        String error; // Error message
        ReturnType data; // Response
    }

    private static final String MtGoxBaseApiUrl = "http://data.mtgox.com/api/2/";

    protected String makeSign(String url, List<NameValuePair> urlParameters) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String signString = url.split(MtGoxBaseApiUrl)[1] + "\0" + requestSender.formatGetParamString(urlParameters);
        return Base64.encodeBase64String(Utils.Crypto.Hashing.hmacByteDigest(signString.getBytes("ASCII"), Base64.decodeBase64(apiKeyPair.privateKey), Utils.Crypto.Hashing.SHA512));
    }

    protected void cleanAuth(List<NameValuePair> urlParameters, List<NameValuePair> httpHeaders) {
        Iterator<NameValuePair> headerIterator = httpHeaders.iterator();
        while (headerIterator.hasNext()) { // Cleaning
            NameValuePair header = headerIterator.next();
            if (header.getName().equals("Rest-Key") || header.getName().equals("Rest-Sign")) {
                headerIterator.remove();
            }
        }
        Iterator<NameValuePair> paramsIterator = urlParameters.iterator();
        while (paramsIterator.hasNext()) { // Cleaning
            NameValuePair header = paramsIterator.next();
            if (header.getName().equals("nonce") || header.getName().equals("tonce")) {
                paramsIterator.remove();
            }
        }
    }

    @Override
    protected void addNonce(List<NameValuePair> urlParameters) {
        urlParameters.add(new BasicNameValuePair("tonce", Long.toString(System.currentTimeMillis() * 1000)));
    }

    protected void writeAuthParams(String url, List<NameValuePair> urlParameters, List<NameValuePair> httpHeaders) {
        if (apiKeyPair == null || apiKeyPair.publicKey.isEmpty() || apiKeyPair.privateKey.isEmpty()) {
            throw new IllegalArgumentException("Invalid API key pair");
        }
        addNonce(urlParameters);
        try {
            httpHeaders.add(new BasicNameValuePair("Rest-Sign", makeSign(url, urlParameters)));
            httpHeaders.add(new BasicNameValuePair("Rest-Key", apiKeyPair.publicKey));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String executeRequest(boolean needAuth, String url, List<NameValuePair> urlParameters, int httpRequestType) throws IOException {
        if (urlParameters == null) urlParameters = new ArrayList<>(); // empty list
        List<NameValuePair> httpHeaders = new ArrayList<>();
        cleanAuth(urlParameters, httpHeaders);
        if (needAuth) writeAuthParams(url, urlParameters, httpHeaders);
        switch (httpRequestType) {
            case Constants.REQUEST_GET:
                return requestSender.getResponseString(requestSender.getRequest(url, urlParameters, httpHeaders));
            case Constants.REQUEST_POST:
                return requestSender.getResponseString(requestSender.postRequest(url, urlParameters, httpHeaders));
            default:
                throw new IllegalArgumentException("Unknown httpRequestType value");
        }
    }

    // Internal:
    private static class MtGoxObjects {
        class TickerFast {
            class TickerData {
                double value;
                String currency; // USD, EUR, JPY
            }

            TickerData last_local; // ???
            TickerData last_orig; // ???
            TickerData last_all; // ???
            TickerData last;
            TickerData buy;
            TickerData sell;
            long now; // Timestamp
        }

        class Ticker extends TickerFast {
            TickerData high;
            TickerData low;
            TickerData avg;
            TickerData vwap; // ???
            TickerData vol; // Volume
            String item; // "BTC"
        }

        class Depth {
            class DepthEntry {
                double price;
                double amount;
                long stamp;
            }

            long now; // Timestamp
            long cached; // Timestamp
            List<DepthEntry> asks = new ArrayList<>();
            List<DepthEntry> bids = new ArrayList<>();
            Ticker.TickerData filter_min_price;
            Ticker.TickerData filter_max_price;
        }

        class Trade {
            long date; // Timestamp
            double price;
            double amount;
            long tid; // trade id
            String trade_type; // "bid"/"ask"
        }
        class CurrencyObject {
            String currency;
            double value;
        }
        class AccountInfo {
            class Wallet {
                CurrencyObject Balance;
                CurrencyObject Open_Orders;
                int Operations;
            }
            double Trade_Fee;
            List<String> Rights = new ArrayList<>();
            Map<String, Wallet> Wallets = new HashMap<>();
        }
        class Order {
            String oid;
            String currency;
            String item;
            String type; // bid/ask
            CurrencyObject amount;
            CurrencyObject effective_amount;
            CurrencyObject invalid_amount;
            CurrencyObject price;
            String status; // Statuses may be: pending, executing, post-pending, open, stop, and invalid
            long date;
        }
    }

    private String formatMtGoxApiUrl(String pair, String method) {
        if (pair != null && !pair.equals(""))
            return MtGoxBaseApiUrl + (pair + "/money/" + method).toUpperCase();
        else
            return MtGoxBaseApiUrl + ("money/" + method).toUpperCase();
    }

    // Ticker (prices):
    @Deprecated
    private ApiStatus<MtGoxObjects.TickerFast> internalGetFastTicker(String pair) throws Exception {
        String url = formatMtGoxApiUrl(pair, "ticker_fast");
        return jsonParser.fromJson(executeRequest(false, url, null, Constants.REQUEST_GET), new TypeToken<ApiStatus<MtGoxObjects.TickerFast>>() {
        }.getType());
    }

    private ApiStatus<MtGoxObjects.Ticker> internalGetTicker(String pair) throws Exception {
        String url = formatMtGoxApiUrl(pair, "ticker");
        return jsonParser.fromJson(executeRequest(false, url, null, Constants.REQUEST_GET), new TypeToken<ApiStatus<MtGoxObjects.Ticker>>() {
        }.getType());
    }

    // Depth:
    private ApiStatus<MtGoxObjects.Depth> internalGetDepth(String pair) throws Exception {
        String url = formatMtGoxApiUrl(pair, "depth/fetch");
        return jsonParser.fromJson(executeRequest(false, url, null, Constants.REQUEST_GET), new TypeToken<ApiStatus<MtGoxObjects.Depth>>() {
        }.getType());
    }

    // History:
    private ApiStatus<List<MtGoxObjects.Trade>> internalGetTrades(String pair) throws Exception {
        String url = formatMtGoxApiUrl(pair, "trades/fetch");
        return jsonParser.fromJson(executeRequest(false, url, null, Constants.REQUEST_GET), new TypeToken<ApiStatus<List<MtGoxObjects.Trade>>>() {
        }.getType());
    }

    // Account info:
    private ApiStatus<MtGoxObjects.AccountInfo> internalGetAccountInfo(String pair) throws IOException {
        String url = formatMtGoxApiUrl(pair, "info");
        return jsonParser.fromJson(executeRequest(true, url, null, Constants.REQUEST_POST), new TypeToken<ApiStatus<MtGoxObjects.AccountInfo>>() {}.getType());
    }

    // Account orders
    private ApiStatus<List<MtGoxObjects.Order>> internalGetAccountOrders(String pair) throws IOException {
        String url = formatMtGoxApiUrl(pair, "orders");
        return jsonParser.fromJson(executeRequest(true, url, null, Constants.REQUEST_POST), new TypeToken<ApiStatus<List<MtGoxObjects.Order>>>() {}.getType());
    }

    // Trading:
    private ApiStatus<String> internalCreateOrder(String pair, int orderType, double amount, double price) throws IOException {
        List<NameValuePair> httpParameters = new ArrayList<>();
        httpParameters.add(new BasicNameValuePair("type", orderType == Constants.ORDER_SELL ? "ask" : "bid"));
        httpParameters.add(new BasicNameValuePair("amount", Utils.Strings.formatNumber(amount)));
        httpParameters.add(new BasicNameValuePair("price", Utils.Strings.formatNumber(price)));
        String url = formatMtGoxApiUrl(pair, "order/add");
        return jsonParser.fromJson(executeRequest(true, url, httpParameters, Constants.REQUEST_POST), new TypeToken<ApiStatus<String>>() {}.getType());
    }

    private ApiStatus<String> internalCancelOrder(String pair, String orderId) throws IOException {
        List<NameValuePair> httpParameters = new ArrayList<>();
        httpParameters.add(new BasicNameValuePair("oid", orderId));
        String url = formatMtGoxApiUrl(pair, "order/cancel");
        return jsonParser.fromJson(executeRequest(true, url, httpParameters, Constants.REQUEST_POST), new TypeToken<ApiStatus<String>>() {}.getType());
    }


    // Public:
    private final BaseTradeApi.StandartObjects.CurrencyPairMapper pairMapper = new StandartObjects.CurrencyPairMapper();
    public BaseTradeApi.StandartObjects.CurrencyPairMapper getCurrencyPairs() throws Exception {
        if(pairMapper.size() == 0) {
            StandartObjects.CurrencyPair btcUsd = new StandartObjects.CurrencyPair();
            // Only BTC/USD :(
            btcUsd.firstCurrency = "BTC";
            btcUsd.secondCurrency = "USD";
            btcUsd.pairId = "BTCUSD";
            btcUsd.pairName = "BTC/USD";
            pairMapper.put(btcUsd.pairId, btcUsd);
        }
        return pairMapper;
    }

    // Basic info:
    public BaseTradeApi.StandartObjects.Prices getMarketPrices(Object pair) throws Exception {
        ApiStatus<MtGoxObjects.Ticker> tickerApiStatus = internalGetTicker((String) pair);
        StandartObjects.Prices prices = new StandartObjects.Prices();
        if (!tickerApiStatus.result.equals("success")) {
            throw new TradeApiError("Error retrieving prices data (" + tickerApiStatus.error + ")");
        } else if (tickerApiStatus.data != null) {
            prices.average = tickerApiStatus.data.avg.value;
            prices.last = tickerApiStatus.data.last.value;
            prices.high = tickerApiStatus.data.high.value;
            prices.low = tickerApiStatus.data.low.value;
            prices.buy = tickerApiStatus.data.buy.value;
            prices.sell = tickerApiStatus.data.sell.value;
            prices.volume = tickerApiStatus.data.vol.value;
        }
        return prices;
    }

    public BaseTradeApi.StandartObjects.Depth getMarketDepth(Object pair) throws Exception {
        ApiStatus<MtGoxObjects.Depth> depthApiStatus = internalGetDepth((String) pair);
        StandartObjects.Depth depth = new StandartObjects.Depth();
        if (!depthApiStatus.result.equals("success")) {
            throw new TradeApiError("Error retrieving depth data (" + depthApiStatus.error + ")");
        } else {
            if (depthApiStatus.data.asks != null) for (MtGoxObjects.Depth.DepthEntry entry : depthApiStatus.data.asks) {
                StandartObjects.Order order = new StandartObjects.Order();
                order.amount = entry.amount;
                order.price = entry.price;
                order.pair = pair;
                order.time = new Date(entry.stamp);
                order.type = Constants.ORDER_SELL;
                depth.sellOrders.add(order);
            }
            if (depthApiStatus.data.bids != null) for (MtGoxObjects.Depth.DepthEntry entry : depthApiStatus.data.bids) {
                StandartObjects.Order order = new StandartObjects.Order();
                order.amount = entry.amount;
                order.price = entry.price;
                order.pair = pair;
                order.time = new Date(entry.stamp);
                order.type = Constants.ORDER_BUY;
                depth.buyOrders.add(order);
            }
        }
        return depth;
    }

    public List<BaseTradeApi.StandartObjects.Order> getMarketHistory(Object pair) throws Exception {
        ApiStatus<List<MtGoxObjects.Trade>> historyApiStatus = internalGetTrades((String) pair);
        List<StandartObjects.Order> history = new ArrayList<>();
        if (!historyApiStatus.result.equals("success")) {
            throw new TradeApiError("Error retrieving history data (" + historyApiStatus.error + ")");
        } else if (historyApiStatus.data != null) for (MtGoxObjects.Trade entry : historyApiStatus.data) {
            StandartObjects.Order trade = new StandartObjects.Order();
            trade.amount = entry.amount;
            trade.price = entry.price;
            trade.id = entry.tid;
            trade.pair = pair;
            trade.type = entry.trade_type.equals("ask") ? Constants.ORDER_SELL : Constants.ORDER_BUY;
            trade.time = new Date(entry.date * 1000);
            history.add(trade);
        }
        return history;
    }

    public BaseTradeApi.StandartObjects.AccountInfo.AccountBalance getAccountBalances() throws Exception {
        ApiStatus<MtGoxObjects.AccountInfo> accountInfoApiStatus = internalGetAccountInfo((String) getCurrencyPairs().firstEntry().getValue().pairId);
        BaseTradeApi.StandartObjects.AccountInfo.AccountBalance balances = new BaseTradeApi.StandartObjects.AccountInfo.AccountBalance();
        if(!accountInfoApiStatus.result.equals("success")) {
            throw new TradeApiError("Error retrieving account balances data (" + accountInfoApiStatus.error + ")");
        } else if (accountInfoApiStatus.data != null) for(Map.Entry<String, MtGoxObjects.AccountInfo.Wallet> entry : accountInfoApiStatus.data.Wallets.entrySet()) {
            balances.put(entry.getKey(), entry.getValue().Balance.value);
        }
        return balances;
    }

    public List<BaseTradeApi.StandartObjects.Order> getAccountOpenOrders(Object pair) throws Exception {
        ApiStatus<List<MtGoxObjects.Order>> ordersApiStatus = internalGetAccountOrders((String) pair);
        List<StandartObjects.Order> orders = new ArrayList<>();
        if(!ordersApiStatus.result.equals("success")) {
            throw new TradeApiError("Error retrieving account orders data (" + ordersApiStatus.error + ")");
        } else if(ordersApiStatus.data != null) for(MtGoxObjects.Order order : ordersApiStatus.data) {
            StandartObjects.Order stdOrder = new StandartObjects.Order();
            stdOrder.amount = order.amount.value;
            stdOrder.id = order.oid;
            stdOrder.pair = pair;
            stdOrder.price = order.price.value;
            stdOrder.time = new Date(order.date * 1000);
            stdOrder.type = order.type.equals("ask") ? Constants.ORDER_SELL : Constants.ORDER_BUY;
            orders.add(stdOrder);
        }
        return orders;
    }

    public List<BaseTradeApi.StandartObjects.Order> getAccountHistory(Object pair) throws Exception {
        return null; // I can not find api for this
    }

    // Misc
    public double getFeePercent(Object pair) throws Exception {
        return 0.60;
    }

    // Trading api:
    public Object createOrder(Object pair, int orderType, double quantity, double price) throws IOException, TradeApiError {
        ApiStatus<String> createOrderApiStatus = internalCreateOrder((String)pair, orderType, quantity, price);
        if(!createOrderApiStatus.result.equals("success")) {
            throw new TradeApiError("Failed to create order (" + createOrderApiStatus.error + ")");
        } else {
            return createOrderApiStatus.data;
        }
    }

    public boolean cancelOrder(Object orderId) throws Exception {
        ApiStatus<String> createOrderApiStatus = internalCancelOrder((String) pairMapper.firstEntry().getValue().pairId, (String) orderId);
        if(!createOrderApiStatus.result.equals("success")) {
            throw new TradeApiError("Failed to cancel order (" + createOrderApiStatus.error + ")");
        } else {
            return true;
        }
    }
}
