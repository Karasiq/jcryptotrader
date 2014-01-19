/*
 * jCryptoTrader trading client
 * Copyright (C) 2014 1M4SKfh83ZxsCSDmfaXvfCfMonFxMa5vvh (BTC public key)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package com.archean.coinmarketcap;

import com.archean.jtradeapi.BaseTradeApi;
import com.archean.jtradeapi.Utils;
import org.apache.http.NameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoinMarketCapParser {
    public static class CoinCapitalization {
        public String coinCode;
        public String coinName;
        public long usdCap;
        public long btcCap;
        public double usdPrice;
        public double btcPrice;
        public long totalSupply;
        public long usdVolume;
        public long btcVolume;
        public double change;
    }

    private final static String coinMarketCapUrl = "http://coinmarketcap.com/mineable.html";
    private final static Pattern coinMarketCapRegex = Pattern.compile("<tr id=\"(?:[\\w]{3,5})\">(?:[\\s]*)<td>(?:[0-9]*)</td>(?:[\\s]*)<td class=\"(?:no-wrap |)currency-name\">.*?target=\"_blank\">([\\w\\s]*)</a></td>(?:[\\s]*)<td class=\"(?:no-wrap |)market-cap\" data-usd=\"([0-9,]*)\" data-btc=\"([0-9,]*)\">.*?</td>(?:[\\s]*)<td(?: class=\"no-wrap\"|)><(?:.*?)class=\"price\" data-usd=\"([0-9.e-]*)\" data-btc=\"([0-9.e-]*)\">.*?</a></td>(?:[\\s]*)<td(?: class=\"no-wrap\"|)>(?:<a href=\".*?\">|)([0-9,]*) ([\\w]{3,5})(?:</a>|)</td>(?:[\\s]*)<td class=\"(?:no-wrap |)volume\" data-usd=\"([0-9,]*)\" data-btc=\"([0-9,]*)\">.*?</td>(?:[\\s]*)<td class=\"(?:no-wrap |)(?:positive|negative)_change\">((?:\\+?|-)[0-9.]*?) %</td>(?:[\\s]*).*?(?:[\\s]*?)</tr>");

    private String requestPage() throws IOException {
        BaseTradeApi.RequestSender requestSender = new BaseTradeApi.RequestSender();
        return requestSender.getResponseString(requestSender.getRequest(coinMarketCapUrl, new ArrayList<NameValuePair>(), new ArrayList<NameValuePair>()));
    }

    public List<CoinCapitalization> getData() throws IOException {
        List<CoinCapitalization> coinCapitalizationList = new ArrayList<>();
        String response = requestPage();
        Matcher regexMatcher = coinMarketCapRegex.matcher(response);
        while (regexMatcher.find()) {
            CoinCapitalization capitalization = new CoinCapitalization();
            capitalization.coinName = regexMatcher.group(1);
            capitalization.usdCap = Long.parseLong(regexMatcher.group(2).replaceAll(",", ""));
            capitalization.btcCap = Long.parseLong(regexMatcher.group(3).replaceAll(",", ""));
            capitalization.usdPrice = Double.parseDouble(regexMatcher.group(4));
            capitalization.btcPrice = Double.parseDouble(regexMatcher.group(5));
            capitalization.totalSupply = Long.parseLong(regexMatcher.group(6).replaceAll(",", ""));
            capitalization.coinCode = regexMatcher.group(7);
            capitalization.usdVolume = Long.parseLong(regexMatcher.group(8).replaceAll(",", ""));
            capitalization.btcVolume = Long.parseLong(regexMatcher.group(9).replaceAll(",", ""));
            capitalization.change = Double.parseDouble(regexMatcher.group(10));
            coinCapitalizationList.add(capitalization);
        }
        return coinCapitalizationList;
    }

    // Worker:
    public interface CoinMarketCapEvent {
        public void onDataUpdate(final List<CoinCapitalization> data);
        public void onError(final Exception e);
    }

    public static class CoinMarketCapWorker implements AutoCloseable {
        Map<Integer, CoinMarketCapEvent> eventHandlers = new HashMap<>();
        private void onErrorEvent(final Exception e) {
            for(CoinMarketCapEvent event : eventHandlers.values()) {
                event.onError(e);
            }
        }
        private void onDataUpdateEvent(final List<CoinCapitalization> data) {
            for(CoinMarketCapEvent event : eventHandlers.values()) {
                event.onDataUpdate(data);
            }
        }
        public void addEventHandler(int id, CoinMarketCapEvent event) {
            eventHandlers.put(id, event);
        }
        public void removeEventHandler(int id) {
            eventHandlers.remove(id);
        }
        private final Runnable marketCapUpdateTask = new Utils.Threads.CycledRunnable() {
            private CoinMarketCapParser parser = new CoinMarketCapParser();

            @Override
            protected int onError(Exception e) {
                onErrorEvent(new Exception("Error retrieving market cap data", e));
                return 10 * 1000;
            }

            @Override
            protected int cycle() throws Exception {
                onDataUpdateEvent(parser.getData());
                return 2 * 60 * 1000; // 2m
            }
        };
        private Thread marketCapUpdateThread;
        public void start() {
            stop();
            marketCapUpdateThread = new Thread(marketCapUpdateTask);
            marketCapUpdateThread.start();
        }
        public void stop() {
            if(marketCapUpdateThread != null && marketCapUpdateThread.isAlive())
                marketCapUpdateThread.interrupt();
        }
        public void close() {
            stop();
        }
    }

    public static final CoinMarketCapWorker coinMarketCapRetriever = new CoinMarketCapWorker(); // Singleton
    static {
        coinMarketCapRetriever.start();
    }
}
