package com.archean.jtradeapi;

import java.io.IOException;
import java.util.List;

public class MtGoxTradeApi extends BaseTradeApi {
    public BaseTradeApi.StandartObjects.CurrencyPairMapper getCurrencyPairs() throws Exception {
        BaseTradeApi.StandartObjects.CurrencyPairMapper mapper = new BaseTradeApi.StandartObjects.CurrencyPairMapper();
        BaseTradeApi.StandartObjects.CurrencyPair btcUsd = new BaseTradeApi.StandartObjects.CurrencyPair();

        // Only BTC/USD :(
        btcUsd.firstCurrency = "BTC";
        btcUsd.secondCurrency = "USD";
        btcUsd.pairId = 1;
        btcUsd.pairName = "BTC/USD";

        mapper.put(btcUsd.pairId, btcUsd);
        return mapper;
    }

    // Basic info:
    public BaseTradeApi.StandartObjects.Prices getMarketPrices(Object pair) throws Exception {
        return null;
    }

    public BaseTradeApi.StandartObjects.Depth getMarketDepth(Object pair) throws Exception {
        return null;
    }

    public List<BaseTradeApi.StandartObjects.Order> getMarketHistory(Object pair) throws Exception {
        return null;
    }

    public BaseTradeApi.StandartObjects.AccountInfo.AccountBalance getAccountBalances() throws Exception {
        return null;
    }

    public List<BaseTradeApi.StandartObjects.Order> getAccountOpenOrders(Object pair) throws Exception {
        return null;
    }

    public List<BaseTradeApi.StandartObjects.Order> getAccountHistory(Object pair) throws Exception {
        return null;
    }

    // Consolidated info:
    public BaseTradeApi.StandartObjects.MarketInfo getMarketData(Object pair, boolean retrieveOrders, boolean retrieveHistory) throws Exception {
        return null;
    }

    public BaseTradeApi.StandartObjects.AccountInfo getAccountInfo(Object pair, boolean retrieveOrders, boolean retrieveHistory) throws Exception {
        return null;
    }

    // Misc
    public double getFeePercent(Object pair) throws Exception {
        return 0.60;
    }

    // Trading api:
    public long createOrder(Object pair, int orderType, double quantity, double price) throws IOException, TradeApiError {
        return 0;
    }

    public boolean cancelOrder(long orderId) throws Exception {
        return false;
    }
}
