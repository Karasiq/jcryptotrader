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

import java.math.BigDecimal;
import java.util.*;

public class MovingAverage {
    public enum MovingAverageType {
        SMA, EMA, SMMA, LWMA
    }

    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class Parameters {
        private final MovingAverageType type;
        private final int period;
        private BigDecimal alpha;
    }

    public static final Parameters DEFAULT_PARAMETERS = new Parameters(MovingAverageType.SMA, 1);

    public static BigDecimal getEMAMultiplier(int period) {
        return new BigDecimal(2).divide(new BigDecimal(period + 1), TAUtils.ROUNDING_PRECISION, TAUtils.ROUNDING_MODE);
    }


    // Functions:
    public static BigDecimal getMovingAverageValue(BigDecimal priceSum, @NonNull TAUtils.PriceChange currentPeriod, int position, @NonNull Parameters parameters, @NonNull Map<String, Object> cache) {
        int period = parameters.getPeriod();
        BigDecimal alpha = parameters.getAlpha(), bigDecimalPeriod = new BigDecimal(period);
        switch (parameters.getType()) {
            case SMA: // Simple
                return priceSum.divide(bigDecimalPeriod, TAUtils.ROUNDING_PRECISION, TAUtils.ROUNDING_MODE);
            case EMA: // Exponential
                BigDecimal prevEma = (BigDecimal) cache.get("prevEma");
                if(prevEma == null) {
                    prevEma = priceSum.divide(bigDecimalPeriod, TAUtils.ROUNDING_PRECISION, TAUtils.ROUNDING_MODE);
                }
                BigDecimal emaValue = currentPeriod.getSecondPrice().subtract(prevEma).multiply(alpha).add(prevEma);
                cache.put("prevEma", emaValue);
                return emaValue;
            case SMMA: // Smoothed
                if (position == 0) {
                    BigDecimal smma1 = priceSum.divide(bigDecimalPeriod);
                    cache.put("smma1", smma1);
                    return smma1;
                } else {
                    BigDecimal smma1 = (BigDecimal) cache.get("smma1");
                    BigDecimal prevSum = (BigDecimal) cache.get("smmaPrevSum");
                    if (prevSum != null) {
                        BigDecimal smmaI = prevSum.multiply(new BigDecimal(position)).subtract(prevSum).add(currentPeriod.getSecondPrice()).divide(bigDecimalPeriod);
                        cache.put("smmaPrevSum", smmaI);
                        return smmaI;
                    } else {
                        prevSum = smma1.multiply(bigDecimalPeriod.subtract(BigDecimal.ONE)).add(currentPeriod.getSecondPrice()).divide(bigDecimalPeriod);
                        cache.put("smmaPrevSum", prevSum);
                        return prevSum;
                    }
                }
            case LWMA: // Linear Weighted
                BigDecimal weight = (BigDecimal) cache.get("weight");
                if (weight == null) weight = new BigDecimal(position);
                else weight = weight.add(new BigDecimal(position));
                cache.put("weight", weight);
                return priceSum.divide(weight.add(bigDecimalPeriod), TAUtils.ROUNDING_PRECISION, TAUtils.ROUNDING_MODE);
            default:
                throw new IllegalArgumentException();
        }
    }

    public static List<HistoryUtils.TimestampedChartData> build(@NonNull List<TAUtils.PriceChange> history, @NonNull Parameters parameters) {
        if(parameters.getAlpha() == null) {
            switch (parameters.getType()) {
                case EMA:
                    parameters.setAlpha(getEMAMultiplier(parameters.getPeriod()));
                    break;
                case SMMA:
                    parameters.setAlpha(new BigDecimal(0.9));
                    break;
            }
        }
        final Map<String, Object> cache = new HashMap<>();
        final List<HistoryUtils.TimestampedChartData> decimals = new ArrayList<>();
        int period = parameters.getPeriod();
        for (int i = period; i < history.size(); i += period) {
            final TAUtils.PriceChange priceChange = history.get(i);
            decimals.add(new HistoryUtils.TimestampedChartData(priceChange.getSecondDate(), getMovingAverageValue(TAUtils.priceSum(history, i - period, i), priceChange, i - period, parameters, cache)));
        }
        return decimals;
    }

    @RequiredArgsConstructor
    public static class MovingAverageBuilder {
        private
        @NonNull
        @Getter
        Parameters parameters;

        private Map<String, Object> cache = new HashMap<>();
        private List<HistoryUtils.TimestampedChartData> data = new ArrayList<>();
        private final List<TAUtils.PriceChange> periodList = new ArrayList<>();
        private int currentPos = 0;

        public int put(Date date, BigDecimal value) {
            Date firstDate;
            BigDecimal firstValue;
            if (periodList.size() > 0) {
                TAUtils.PriceChange prevPeriod = periodList.get(periodList.size() - 1);
                firstDate = prevPeriod.getSecondDate();
                firstValue = prevPeriod.getSecondPrice();
            } else {
                firstDate = date;
                firstValue = BigDecimal.ZERO;
            }
            TAUtils.PriceChange period = new TAUtils.PriceChange(firstDate, date, firstValue, value);
            periodList.add(period);

            data.add(new HistoryUtils.TimestampedChartData(date, getMovingAverageValue(TAUtils.priceSum(periodList, currentPos, periodList.size()), period, data.size(), parameters, cache)));

            if (currentPos > parameters.getPeriod()) {
                currentPos = 0;
            } else {
                currentPos++;
            }

            return data.size() - 1;
        }

        public HistoryUtils.TimestampedChartData get(int index) {
            return data.get(index);
        }

        public HistoryUtils.TimestampedChartData getLast() {
            return data.size() > 0 ? data.get(data.size() - 1) : null;
        }

        public List<HistoryUtils.TimestampedChartData> getData() {
            return data;
        }
    }
}
