/*
 * jCryptoTrader trading client
 * Copyright (C) 2014 1M4SKfh83ZxsCSDmfaXvfCfMonFxMa5vvh (BTC public key)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

// Developed by Tushard Chande and Stanley Kroll, StochRSI is an oscillator that measures the level of RSI relative to its high-low range over a set time period. StochRSI applies the Stochastics formula to RSI values, instead of price values. This makes it an indicator of an indicator. The result is an oscillator that fluctuates between 0 and 1.

package com.archean.TechAnalysis;

import com.archean.jtradeapi.HistoryUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class StochRSI {
    private static BigDecimal stochRsi(BigDecimal current, BigDecimal low, BigDecimal high) {
        return current.subtract(low).divide(high.subtract(low));
    }

    public static List<HistoryUtils.TimestampedChartData> build(List<HistoryUtils.TimestampedChartData> rsi, int period) {
        List<HistoryUtils.TimestampedChartData> result = new ArrayList<>();
        BigDecimal low = null, high = null;
        int i = 0;
        for (HistoryUtils.TimestampedChartData rsiPoint : rsi) {
            if (i > period) {
                result.add(new HistoryUtils.TimestampedChartData(rsiPoint.date, stochRsi(rsiPoint.value, low, high)));
                i = 0;
                high = null;
                low = null;
            }
            if (high == null || rsiPoint.value.compareTo(high) > 0) {
                high = rsiPoint.value;
            }
            if (low == null || rsiPoint.value.compareTo(low) < 0) {
                low = rsiPoint.value;
            }
            i++;
        }
        return result;
    }
}
