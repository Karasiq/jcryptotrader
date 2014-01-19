/*
 * jCryptoTrader trading client
 * Copyright (C) 2014 1M4SKfh83ZxsCSDmfaXvfCfMonFxMa5vvh (BTC public key)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

// In stock and securities market technical analysis, Parabolic SAR (Parabolic Stop and Reverse) is a method devised by J. Welles Wilder, Jr., to find potential reversals in the market price direction of traded goods such as securities or currency exchanges such as forex. It is a trend-following (lagging) indicator and may be used to set a trailing stop loss or determine entry or exit points based on prices tending to stay within a parabolic curve during a strong trend.

package com.archean.TechAnalysis;

import com.archean.jtradeapi.HistoryUtils;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ParabolicSAR {
    public static final BigDecimal DEFAULT_ACCELERATION_FACTOR = new BigDecimal(0.02);
    public static final BigDecimal MAXIMUM_ACCELERATION_FACTOR = new BigDecimal(0.20);

    private static BigDecimal getParabolicSarValue(BigDecimal acceleration, boolean lowTrend, @NonNull BigDecimal prevMaximum, BigDecimal prevValue) {
        BigDecimal d1 = prevValue == null ? prevMaximum : prevMaximum.subtract(prevValue);
        BigDecimal result = acceleration.multiply(d1);
        if (prevValue != null) result = lowTrend ? result.subtract(prevValue) : result.add(prevValue);
        return result;
    }

    public static List<HistoryUtils.TimestampedChartData> build(@NonNull List<TAUtils.PriceChange> priceChanges, int period, BigDecimal accelerationFactor, boolean lowTrend) {
        List<HistoryUtils.TimestampedChartData> chartDataList = new ArrayList<>();
        BigDecimal maximum = null, prevMaximum = null, prevValue = null, currentAccelerationFactor = accelerationFactor;
        int i = 0;
        for (TAUtils.PriceChange priceChange : priceChanges) {
            if (i >= period) {
                prevMaximum = null;
                i = 0;
            }
            int comp = priceChange.getSecondPrice().compareTo(maximum);
            if (maximum == null || (lowTrend && comp < 0) || (!lowTrend && comp > 0)) {
                maximum = priceChange.getSecondPrice();
                if (currentAccelerationFactor.compareTo(MAXIMUM_ACCELERATION_FACTOR) < 0) {
                    currentAccelerationFactor = currentAccelerationFactor.add(accelerationFactor);
                }
            }
            if (prevValue != null) {
                comp = priceChange.getSecondPrice().compareTo(prevValue);
                if ((lowTrend && comp > 0) || (!lowTrend && comp < 0)) {
                    currentAccelerationFactor = accelerationFactor;
                }
            }
            if (prevMaximum != null) {
                BigDecimal value = getParabolicSarValue(currentAccelerationFactor, lowTrend, prevMaximum, prevValue);
                chartDataList.add(new HistoryUtils.TimestampedChartData(priceChange.getSecondDate(), value));
                prevValue = value;
            }
            prevMaximum = maximum;
            i++;
        }
        return chartDataList;
    }

    public static List<HistoryUtils.TimestampedChartData> build(@NonNull List<TAUtils.PriceChange> priceChanges, int period) {
        return build(priceChanges, period, DEFAULT_ACCELERATION_FACTOR, false);
    }
}
