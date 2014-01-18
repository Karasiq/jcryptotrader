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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovingAverage {
    public enum MovingAverageType {
        SMA, EMA, SMMA
    }
    private Map<String, Object> parameters = new HashMap<>();

    // Constructor:
    public MovingAverage() {
        setPeriod(1);
        setAlpha(BigDecimal.ONE);
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
    public BigDecimal getMovingAverageValue(TAUtils.PriceChange priceChange, int position, Map<String, Object> cache) {
        BigDecimal period = (BigDecimal) parameters.get("period");
        switch (getType()) {
            case SMA: // Simple
                return priceChange.getAbsolute().divide(period);
            case EMA: // Exponential
                return priceChange.getFirstPrice().add(getAlpha()).multiply(priceChange.getAbsolute());
            case SMMA: // Smoothed
                if(position == 0) {
                    BigDecimal smma1 = priceChange.getAbsolute().divide(period);
                    cache.put("smma1", smma1);
                    return smma1;
                } else  {
                    BigDecimal smma1 = (BigDecimal) cache.get("smma1");
                    BigDecimal prevSum = (BigDecimal) cache.get("smmaPrevSum");
                    if(prevSum != null) {
                        BigDecimal smmaI = prevSum.multiply(new BigDecimal(position)).subtract(prevSum).add(priceChange.getSecondPrice()).divide(getAlpha());
                        cache.put("smmaPrevSum", smmaI);
                        return smmaI;
                    } else {
                        prevSum = smma1.multiply(getAlpha().subtract(BigDecimal.ONE)).add(priceChange.getSecondPrice()).divide(getAlpha());
                        cache.put("smmaPrevSum", prevSum);
                        return prevSum;
                    }
                }

            default:
                throw new IllegalArgumentException();
        }
    }
    public List<BigDecimal> build(List<TAUtils.PriceChange> history) {
        final Map<String, Object> cache = new HashMap<>();
        final List<BigDecimal> decimals = new ArrayList<>();
        for(int i = 0; i < history.size(); i++) {
            decimals.add(getMovingAverageValue(history.get(i), i, cache));
        }
        return decimals;
    }
}
