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
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CoinExTradeApi extends BaseTradeApi {
    public CoinExTradeApi(ApiKeyPair keyPair) {
        super(keyPair);
    }
    // Internal:
    private static class CoinExObjects {
        public static class CurrencyPair {
            // int id;
            // double buy_fee;
            // double sell_fee;
            long last_price;
            int currency_id;
            // int market_id;
            String url_slug;
            long rate_min;
            long rate_max;
            // long currency_volume;
            long market_volume;
            // Date updated_at;
        }
        public static class Trade {
            long id;
            String created_at;
            boolean bid;
            long rate;
            long amount;
            // int trade_pair_id;
        }
        public static class Order extends Trade {
            // long filled;
            boolean cancelled;
            boolean complete;
            // Date updated_at;
        }
        public static class Balance {
            int id;
            int currency_id;
            String currency_name;
            long amount;
            long held;
            String deposit_address;
            Date updated_at;
        }
    }
    private static final String COINEX_BASE_API_URL = "https://coinex.pw/api/v2/";
    private static String formatCoinExApiUrl(String method) {
        return COINEX_BASE_API_URL + method;
    }
    private static final DateFormat dateShitNormalizer = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static Date getNormalDateNotShit(String shit) {
        try {
            return dateShitNormalizer.parse(shit);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date();
        }
    }
    private static final BigDecimal shitNormalizer = new BigDecimal(0.000001, MathContext.DECIMAL64);
    private static final BigDecimal priceShitNormalizer = new BigDecimal(0.00000001, MathContext.DECIMAL64);
    private double getNormalNumberNotShit(long shit) {
        return new BigDecimal(shit, MathContext.DECIMAL64).multiply(shitNormalizer).doubleValue();
    }
    private double getNormalPriceNotShit(long shit) {
        return new BigDecimal(shit, MathContext.DECIMAL64).multiply(priceShitNormalizer).doubleValue();
    }
    private List<CoinExObjects.CurrencyPair> internalGetCurrencyPairs() throws IOException {
        String url = formatCoinExApiUrl("trade_pairs");
        String response = executeRequest(false, url, null, Constants.REQUEST_GET);
        return (((HashMap<String, List<CoinExObjects.CurrencyPair>>)jsonParser.fromJson(response, new TypeToken<HashMap<String, List<CoinExObjects.CurrencyPair>>>(){}.getType())).get("trade_pairs"));
    }

    private List<CoinExObjects.Order> internalGetMarketOrders(int marketId) throws IOException {
        String url = formatCoinExApiUrl("orders");
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("tradePair", Integer.toString(marketId)));
        String response = executeRequest(false, url, urlParameters, Constants.REQUEST_GET);
        return (((HashMap<String, List<CoinExObjects.Order>>)jsonParser.fromJson(response, new TypeToken<HashMap<String, List<CoinExObjects.Order>>>(){}.getType())).get("orders"));
    }

    private List<CoinExObjects.Trade> internalGetMarketHistory(int marketId) throws IOException {
        String url = formatCoinExApiUrl("trades");
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("tradePair", Integer.toString(marketId)));
        String response = executeRequest(false, url, urlParameters, Constants.REQUEST_GET);
        return (((HashMap<String, List<CoinExObjects.Trade>>)jsonParser.fromJson(response, new TypeToken<HashMap<String, List<CoinExObjects.Trade>>>(){}.getType())).get("trades"));
    }


    // Public:
    private StandartObjects.CurrencyPairMapper pairMapper = null;
    public StandartObjects.CurrencyPairMapper getCurrencyPairs() throws Exception {
        if(pairMapper == null) {
            pairMapper = new StandartObjects.CurrencyPairMapper();
            List<CoinExObjects.CurrencyPair> currencyPairs = internalGetCurrencyPairs();
            for(CoinExObjects.CurrencyPair currencyPair : currencyPairs) {
                StandartObjects.CurrencyPair stdCurrencyPair = new StandartObjects.CurrencyPair();
                String[] urlSlugSplit = currencyPair.url_slug.split("_");
                stdCurrencyPair.firstCurrency = urlSlugSplit[0].toUpperCase();
                stdCurrencyPair.secondCurrency = urlSlugSplit[1].toUpperCase();
                stdCurrencyPair.pairId = currencyPair.currency_id;
                stdCurrencyPair.pairName = String.format("%s/%s", stdCurrencyPair.firstCurrency, stdCurrencyPair.secondCurrency);
                pairMapper.put(stdCurrencyPair.pairId, stdCurrencyPair);
            }
        }
        return pairMapper;
    }

    // Basic info:
    public StandartObjects.Prices getMarketPrices(Object pair) throws Exception {
        StandartObjects.Prices prices = new StandartObjects.Prices();
        List<CoinExObjects.CurrencyPair> currencyPairs = internalGetCurrencyPairs();
        for(CoinExObjects.CurrencyPair currencyPair : currencyPairs) if(pair.equals(currencyPair.currency_id)) {
            prices.average = getNormalPriceNotShit((currencyPair.rate_max + currencyPair.rate_min) / 2);
            prices.buy = prices.sell = prices.last = getNormalPriceNotShit(currencyPair.last_price);
            prices.low = getNormalPriceNotShit(currencyPair.rate_min);
            prices.high = getNormalPriceNotShit(currencyPair.rate_max);
            prices.volume = currencyPair.market_volume;
            break;
        }
        return prices;
    }

    public StandartObjects.Depth getMarketDepth(Object pair) throws Exception {
        StandartObjects.Depth depth = new StandartObjects.Depth();
        List<CoinExObjects.Order> orderList = internalGetMarketOrders((Integer)pair);
        for(CoinExObjects.Order order : orderList) if(!order.cancelled && !order.complete) {
            StandartObjects.Order stdOrder = new StandartObjects.Order();
            stdOrder.amount = getNormalNumberNotShit(order.amount);
            stdOrder.price = getNormalNumberNotShit(order.rate);
            stdOrder.type = order.bid ? Constants.ORDER_BUY : Constants.ORDER_SELL;
            stdOrder.pair = pair;
            stdOrder.time = getNormalDateNotShit(order.created_at);
            if(order.bid) {
                depth.buyOrders.add(stdOrder);
            } else {
                depth.sellOrders.add(stdOrder);
            }
        }
        return depth;
    }

    public List<StandartObjects.Order> getMarketHistory(Object pair) throws Exception {
        List<StandartObjects.Order> history = new ArrayList<>();
        List<CoinExObjects.Trade> trades = internalGetMarketHistory((Integer) pair);
        for(CoinExObjects.Trade trade : trades) {
            StandartObjects.Order order = new StandartObjects.Order();
            order.amount = getNormalNumberNotShit(trade.amount);
            order.price = getNormalNumberNotShit(trade.rate);
            order.id = trade.id;
            order.pair = pair;
            order.type = trade.bid ? Constants.ORDER_BUY : Constants.ORDER_SELL;
            order.time = getNormalDateNotShit(trade.created_at);
            history.add(order);
        }
        return history;
    }

    public StandartObjects.AccountInfo.AccountBalance getAccountBalances() throws Exception {
        return null;
    }

    public List<StandartObjects.Order> getAccountOpenOrders(Object pair) throws Exception {
        return null;
    }

    public List<StandartObjects.Order> getAccountHistory(Object pair) throws Exception {
        return null;
    }

    // Misc
    public double getFeePercent(Object pair) throws Exception {
        return 0.2;
    }

    // Trading api:
    public Object createOrder(Object pair, int orderType, double quantity, double price) throws Exception {
        throw new NotImplementedException();
    }

    public boolean cancelOrder(Object orderId) throws Exception {
        throw new NotImplementedException();
    }
}
