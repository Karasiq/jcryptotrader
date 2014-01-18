/*
 * jCryptoTrader trading client
 * Copyright (C) 2014 1M4SKfh83ZxsCSDmfaXvfCfMonFxMa5vvh (BTC public key)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

// MACD (moving average convergence/divergence) is a technical analysis indicator created by Gerald Appel in the late 1970s. It is used to spot changes in the strength, direction, momentum, and duration of a trend in a stock's price.

package com.archean.TechAnalysis;

import com.archean.jtradeapi.HistoryUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MACD {

    public static List<HistoryUtils.TimestampedChartData> build(List<TAUtils.PriceChange> priceChanges, int shortPeriod, int longPeriod, int smaPeriod) {
        List<HistoryUtils.TimestampedChartData> result = new ArrayList<>();
        MovingAverage shortEMA = new MovingAverage(), longEMA = new MovingAverage();
        MovingAverage.MovingAverageBuilder smaBuilder = null;
        if (smaPeriod != 0) {
            smaBuilder = new MovingAverage.MovingAverageBuilder(MovingAverage.MovingAverageType.SMA, smaPeriod, null);
        }
        shortEMA.setType(MovingAverage.MovingAverageType.EMA);
        shortEMA.setPeriod(shortPeriod);
        longEMA.setType(MovingAverage.MovingAverageType.EMA);
        longEMA.setPeriod(longPeriod);

        List<HistoryUtils.TimestampedChartData> emaS = shortEMA.build(priceChanges), emaL = longEMA.build(priceChanges);
        for (int i = 0; i < priceChanges.size(); i++) {
            HistoryUtils.TimestampedChartData emaSdata = emaS.get(i), emaLdata = emaL.get(i);
            TAUtils.PriceChange priceChange = priceChanges.get(i);
            HistoryUtils.TimestampedChartData decimal = new HistoryUtils.TimestampedChartData(priceChange.getSecondDate(), emaSdata.value.subtract(emaLdata.value));
            if (smaBuilder != null) {
                decimal = smaBuilder.get(smaBuilder.put(priceChange.getFirstDate(), priceChange.getSecondDate(), BigDecimal.ZERO, decimal.value));
            }
            result.add(decimal);
        }
        return result;
    }

    public static List<HistoryUtils.TimestampedChartData> build(List<TAUtils.PriceChange> priceChanges, boolean signal) {
        return build(priceChanges, 12, 26, signal ? 9 : 0);
    }
}
