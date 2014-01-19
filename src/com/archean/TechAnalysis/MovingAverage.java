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
import lombok.*;
import lombok.experimental.NonFinal;

import java.math.BigDecimal;
import java.util.*;

public class MovingAverage {
    public enum MovingAverageType {
        SMA, EMA, SMMA
    }
    @Value
    public static class Parameters {
        private MovingAverageType type;
        private int period;
        private BigDecimal alpha;
    }
    public static final Parameters DEFAULT_PARAMETERS = new Parameters(MovingAverageType.SMA, 1, new BigDecimal(0.9));


    // Functions:
    public static BigDecimal getMovingAverageValue(@NonNull TAUtils.PriceChange priceChange, int position, @NonNull Parameters parameters, @NonNull Map<String, Object> cache) {
        int period = parameters.getPeriod();
        BigDecimal alpha = parameters.getAlpha(), bigDecimalPeriod = new BigDecimal(period);
        switch (parameters.getType()) {
            case SMA: // Simple
                return priceChange.getAbsolute().divide(bigDecimalPeriod);
            case EMA: // Exponential
                return priceChange.getFirstPrice().add(alpha).multiply(priceChange.getAbsolute());
            case SMMA: // Smoothed
                if (position == 0) {
                    BigDecimal smma1 = priceChange.getAbsolute().divide(bigDecimalPeriod);
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

    public static List<HistoryUtils.TimestampedChartData> build(@NonNull List<TAUtils.PriceChange> history, @NonNull Parameters parameters) {
        final Map<String, Object> cache = new HashMap<>();
        final List<HistoryUtils.TimestampedChartData> decimals = new ArrayList<>();
        int period = parameters.getPeriod();
        for (int i = period; i < history.size(); i++) {
            TAUtils.PriceChange firstPeriod = history.get(i - period), secondPeriod = history.get(i);
            decimals.add(new HistoryUtils.TimestampedChartData(secondPeriod.getSecondDate(), getMovingAverageValue(new TAUtils.PriceChange(firstPeriod.getSecondDate(), secondPeriod.getSecondDate(), firstPeriod.getSecondPrice(), secondPeriod.getSecondPrice()), i - period, parameters, cache)));
        }
        return decimals;
    }

    @RequiredArgsConstructor
    public static class MovingAverageBuilder {
        @Getter
        @Setter
        @NonNull
        private Parameters parameters;

        private Map<String, Object> cache = new HashMap<>();
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
