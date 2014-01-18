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

import com.archean.jtradeapi.HistoryUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

public class TAUtils {
    public static class PriceChange {
        private BigDecimal absolute;
        private BigDecimal firstPrice; // "yesterday"
        private BigDecimal secondPrice; // "today"

        // Getters:
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
            if(isPositive()) {
                return absolute;
            } else {
                return BigDecimal.ZERO;
            }
        }
        public BigDecimal getLoss() {
            if(!isPositive()) {
                return absolute.negate();
            } else {
                return BigDecimal.ZERO;
            }
        }

        // Constructors:
        public PriceChange(BigDecimal price1, BigDecimal price2) {
            this.firstPrice = price1;
            this.secondPrice = price2;
            absolute = price2.subtract(price1); // old - new
        }

        public PriceChange(HistoryUtils.Candle candle1, HistoryUtils.Candle candle2) {
            this(new BigDecimal(candle1.close, MathContext.DECIMAL64), new BigDecimal(candle2.close, MathContext.DECIMAL64));
        }
    }
    public static List<PriceChange> buildPriceMovingHistory(List<HistoryUtils.Candle> candles, int period) {
        List<PriceChange> priceChangeList = new ArrayList<>();
        for(int i = period; i < candles.size(); i += period) {
            priceChangeList.add(new PriceChange(candles.get(i - period), candles.get(period)));
        }
        return priceChangeList;
    }
    public static List<PriceChange> buildPriceMovingHistory(List<HistoryUtils.Candle> candles) {
        return buildPriceMovingHistory(candles, 1);
    }
    public static List<PriceChange> reducePriceMovingHistory(final List<PriceChange> priceChangeList, int period) {
        List<PriceChange> reducedList = new ArrayList<>();
        for(int i = period; i < priceChangeList.size(); i += period) {
            reducedList.add(new PriceChange(priceChangeList.get(i - period).firstPrice, priceChangeList.get(i).secondPrice));
        }
        return reducedList;
    }
}
