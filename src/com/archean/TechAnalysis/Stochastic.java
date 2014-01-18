/*
 * jCryptoTrader trading client
 * Copyright (C) 2014 1M4SKfh83ZxsCSDmfaXvfCfMonFxMa5vvh (BTC public key)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

// In technical analysis of securities trading, the stochastic oscillator is a momentum indicator that uses support and resistance levels. Dr. George Lane promoted this indicator in the 1950s. The term stochastic refers to the location of a current price in relation to its price range over a period of time.[1] This method attempts to predict price turning points by comparing the closing price of a security to its price range.

package com.archean.TechAnalysis;

import com.archean.jtradeapi.HistoryUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Stochastic {
    private static BigDecimal calculateStoch(BigDecimal closePrice, BigDecimal low, BigDecimal high) {
        return closePrice.subtract(low).divide(high.subtract(low)).multiply(new BigDecimal(100));
    }

    public static List<HistoryUtils.TimestampedChartData> build(List<TAUtils.PriceChange> pricePeriods, int period) {
        List<HistoryUtils.TimestampedChartData> result = new ArrayList<>();
        int i = 0;
        BigDecimal low = null, high = null;
        for (TAUtils.PriceChange pricePeriod : pricePeriods) {
            BigDecimal currentPrice = pricePeriod.getSecondPrice();
            if (i >= period) {
                result.add(new HistoryUtils.TimestampedChartData(pricePeriod.getSecondDate(), calculateStoch(currentPrice, low, high)));
                i = 0;
                low = null;
                high = null;
            }
            if (high == null || currentPrice.compareTo(high) > 0) {
                high = currentPrice;
            }
            if (low == null || currentPrice.compareTo(low) < 0) {
                low = currentPrice;
            }
            i++;
        }
        return result;
    }
}
