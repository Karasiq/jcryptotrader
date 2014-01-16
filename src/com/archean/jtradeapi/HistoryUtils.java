package com.archean.jtradeapi;


import java.math.BigDecimal;
import java.util.*;

public class HistoryUtils {
    public static final long PERIOD_1M = 60 * 1000;
    public static final long PERIOD_15M = PERIOD_1M * 15;
    public static final long PERIOD_30M = PERIOD_15M * 2;
    public static class Candle {
        public enum CandleType {
            BULL, BEAR
        }
        public CandleType getType() {
            return close - open >= 0 ? Candle.CandleType.BULL : Candle.CandleType.BEAR;
        }
        public Date start;
        public Date end;
        public Date update;
        public double open;
        public double close;
        public double low;
        public double high;
        public double volume = 0;
    }
    public static BaseTradeApi.StandartObjects.Order getNearestTrade(List<BaseTradeApi.StandartObjects.Order> history, Date targetDate){
        BaseTradeApi.StandartObjects.Order result = history.get(0);
        for (BaseTradeApi.StandartObjects.Order order : history) {
            // if the current iteration's date is "before" the target date
            if (order.time.compareTo(targetDate) <= 0) {
                // if the current iteration's date is "after" the current return date
                if (order.time.compareTo(result.time) > 0){
                    result = order;
                }
            }
        }
        return result;
    }
    public static List<Candle> buildCandles(List<BaseTradeApi.StandartObjects.Order> history, Date limit, long period) {
        List<Candle> candles = new ArrayList<>();
        Collections.sort(history, Collections.reverseOrder());

        int i = 0;
        while(i < history.size()) {
            if(history.get(i).time.before(limit)) {
                i++;
            } else {
                break;
            }
        }

        Candle candle = new Candle();
        candle.start = history.get(i).time; // first
        candle.open = candle.close = candle.high = candle.low = history.get(i).price;
        i++;
        while(i < history.size()) {
            BaseTradeApi.StandartObjects.Order order = history.get(i);
            candle.update = order.time;
            candle.volume += order.amount;
            candle.close = order.price;
            if(order.price < candle.low) {
                candle.low = order.price;
            }
            if(order.price > candle.high) {
                candle.high = order.price;
            }
            if(order.time.getTime() - candle.start.getTime() > period) { // Next candle
                candle.end = order.time;
                candles.add(candle);
                candle = new Candle();
                candle.start = order.time;
                candle.open = candle.close = candle.high = candle.low = order.price;
            }
            i++;
        }
        candles.add(candle); // last
        return candles;
    }
    public static void refreshCandles(List<Candle> candles, List<BaseTradeApi.StandartObjects.Order> history, long period) { // fast update
        Candle candle = candles.get(candles.size() - 1);
        for(BaseTradeApi.StandartObjects.Order order : history) {
            if(order.time.before(candle.update) || order.time.equals(candle.update)) continue;
            candle.update = order.time;
            candle.volume += order.amount;
            candle.close = order.price;
            if(order.price < candle.low) {
                candle.low = order.price;
            }
            if(order.price > candle.high) {
                candle.high = order.price;
            }
            if(order.time.getTime() - candle.start.getTime() > period) { // Next candle
                candle.end = order.time;
                candles.add(candle);
                candle = new Candle();
                candle.start = order.time;
                candle.open = candle.close = candle.high = candle.low = order.price;
            }
        }
        candles.add(candle); // last
    }
}