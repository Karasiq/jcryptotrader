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
import java.util.*;

public class MovingAverage {
    public enum MovingAverageType {
        SMA, EMA, SMMA
    }

    private Map<String, Object> parameters = new HashMap<>();

    // Constructor:
    public MovingAverage() {
        setPeriod(1);
        setAlpha(new BigDecimal(0.9));
        setType(MovingAverageType.SMA);
    }

    // Setters:
    public void setPeriod(final int period) {
        parameters.put("period", new BigDecimal(period));
    }

    public void setAlpha(final BigDecimal alpha) {
        parameters.put("alpha", alpha);
    }

    public void setType(final MovingAverageType type) {
        parameters.put("type", type);
    }

    // Getters:
    public int getPeriod() {
        return ((BigDecimal) parameters.get("period")).intValue();
    }

    public BigDecimal getAlpha() {
        return (BigDecimal) parameters.get("alpha");
    }

    public MovingAverageType getType() {
        return (MovingAverageType) parameters.get("type");
    }

    // Functions:
    public static BigDecimal getMovingAverageValue(TAUtils.PriceChange priceChange, int position, Map<String, Object> parameters, Map<String, Object> cache) {
        BigDecimal period = (BigDecimal) parameters.get("period"), alpha = (BigDecimal) parameters.get("alpha");
        switch ((MovingAverageType) parameters.get("type")) {
            case SMA: // Simple
                return priceChange.getAbsolute().divide(period);
            case EMA: // Exponential
                return priceChange.getFirstPrice().add(alpha).multiply(priceChange.getAbsolute());
            case SMMA: // Smoothed
                if (position == 0) {
                    BigDecimal smma1 = priceChange.getAbsolute().divide(period);
                    cache.put("smma1", smma1);
                    return smma1;
                } else {
                    BigDecimal smma1 = (BigDecimal) cache.get("smma1");
                    BigDecimal prevSum = (BigDecimal) cache.get("smmaPrevSum");
                    if (prevSum != null) {
                        BigDecimal smmaI = prevSum.multiply(new BigDecimal(position)).subtract(prevSum).add(priceChange.getSecondPrice()).divide(alpha);
                        cache.put("smmaPrevSum", smmaI);
                        return smmaI;
                    } else {
                        prevSum = smma1.multiply(alpha.subtract(BigDecimal.ONE)).add(priceChange.getSecondPrice()).divide(alpha);
                        cache.put("smmaPrevSum", prevSum);
                        return prevSum;
                    }
                }

            default:
                throw new IllegalArgumentException();
        }
    }

    public List<HistoryUtils.TimestampedChartData> build(List<TAUtils.PriceChange> history) {
        final Map<String, Object> cache = new HashMap<>();
        final List<HistoryUtils.TimestampedChartData> decimals = new ArrayList<>();
        int period = getPeriod();
        for (int i = period; i < history.size(); i++) {
            TAUtils.PriceChange firstPeriod = history.get(i - period), secondPeriod = history.get(i);
            decimals.add(new HistoryUtils.TimestampedChartData(secondPeriod.getSecondDate(), getMovingAverageValue(new TAUtils.PriceChange(firstPeriod.getSecondDate(), secondPeriod.getSecondDate(), firstPeriod.getSecondPrice(), secondPeriod.getSecondPrice()), i - period, parameters, cache)));
        }
        return decimals;
    }

    public static class MovingAverageBuilder {
        public MovingAverageBuilder(MovingAverageType type, int period, BigDecimal alpha) {
            parameters.put("period", period);
            parameters.put("type", type);
            parameters.put("alpha", alpha);
        }

        private Map<String, Object> cache = new HashMap<>();
        private Map<String, Object> parameters = new HashMap<>();
        private List<HistoryUtils.TimestampedChartData> data = new ArrayList<>();

        public int put(Date firstDate, Date secondDate, BigDecimal firstValue, BigDecimal secondValue) {
            data.add(new HistoryUtils.TimestampedChartData(secondDate, getMovingAverageValue(new TAUtils.PriceChange(firstDate, secondDate, firstValue, secondValue), data.size(), parameters, cache)));
            return data.size() - 1;
        }

        public HistoryUtils.TimestampedChartData get(int index) {
            return data.get(index);
        }

        public List<HistoryUtils.TimestampedChartData> getData() {
            return data;
        }
    }
}
