/*
 * jCryptoTrader trading client
 * Copyright (C) 2014 1M4SKfh83ZxsCSDmfaXvfCfMonFxMa5vvh (BTC public key)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package com.archean.TechAnalysis;

import com.archean.jtradeapi.BaseTradeApi;
import com.archean.jtradeapi.HistoryUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TAUtils {
    public static class PriceChange {
        private BigDecimal absolute;
        private BigDecimal firstPrice; // "yesterday"
        private BigDecimal secondPrice; // "today"
        private Date firstDate;
        private Date secondDate;

        // Getters:
        public Date getFirstDate() {
            return firstDate;
        }

        public Date getSecondDate() {
            return secondDate;
        }

        public BigDecimal getAbsolute() {
            return absolute;
        }

        public BigDecimal getFirstPrice() {
            return firstPrice;
        }

        public BigDecimal getSecondPrice() {
            return secondPrice;
        }

        public boolean isPositive() {
            return absolute.compareTo(BigDecimal.ZERO) >= 0;
        }

        public BigDecimal getGain() {
            if (isPositive()) {
                return absolute;
            } else {
                return BigDecimal.ZERO;
            }
        }

        public BigDecimal getLoss() {
            if (!isPositive()) {
                return absolute.negate();
            } else {
                return BigDecimal.ZERO;
            }
        }

        // Constructors:
        public PriceChange(Date firstDate, Date secondDate, BigDecimal price1, BigDecimal price2) {
            this.firstPrice = price1;
            this.secondPrice = price2;
            this.firstDate = firstDate;
            this.secondDate = secondDate;
            absolute = price2.subtract(price1); // old - new
        }

        public PriceChange(HistoryUtils.Candle candle) {
            this(candle.start, candle.end, new BigDecimal(candle.open, MathContext.DECIMAL64), new BigDecimal(candle.close, MathContext.DECIMAL64));
        }
    }

    public static List<PriceChange> buildPriceMovingHistory(List<HistoryUtils.Candle> candles, int period) { // Only by candle open/close
        List<PriceChange> priceChangeList = new ArrayList<>();
        for (HistoryUtils.Candle candle : candles) {
            priceChangeList.add(new PriceChange(candle));
        }
        return priceChangeList;
    }

    public static List<PriceChange> buildPriceMovingHistory(List<HistoryUtils.Candle> candles) {
        return buildPriceMovingHistory(candles, 1);
    }

    public static List<PriceChange> buildTickHistory(List<BaseTradeApi.StandartObjects.Order> trades, int period) { // Tick data
        List<PriceChange> priceChangeList = new ArrayList<>();
        for (int i = period; i < trades.size(); i++) {
            BaseTradeApi.StandartObjects.Order firstTrade = trades.get(i - period), secondTrade = trades.get(i);
            priceChangeList.add(new PriceChange(firstTrade.time, secondTrade.time, new BigDecimal(firstTrade.price), new BigDecimal(secondTrade.price)));
        }
        return priceChangeList;
    }

    public static List<PriceChange> buildTickHistory(List<BaseTradeApi.StandartObjects.Order> trades) {
        return buildTickHistory(trades, 1);
    }

    public static List<PriceChange> reducePriceMovingHistory(final List<PriceChange> priceChangeList, int period) { // Increase period
        List<PriceChange> reducedList = new ArrayList<>();
        for (int i = period; i < priceChangeList.size(); i += period) {
            PriceChange firstPoint = priceChangeList.get(i - period), secondPoint = priceChangeList.get(i);
            reducedList.add(new PriceChange(firstPoint.firstDate, secondPoint.secondDate, firstPoint.firstPrice, secondPoint.secondPrice));
        }
        return reducedList;
    }
}
