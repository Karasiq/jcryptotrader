package com.archean.jtradeapi;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class ApiWorker implements Runnable {
    public abstract static class Callback {
        public abstract void onUpdate(ApiWorker worker);
        public void onError(Exception exc) {
            exc.printStackTrace();
        }
    }

    volatile public BaseTradeApi.StandartObjects.AccountInfo accountInfo;
    volatile public List<BaseTradeApi.StandartObjects.MarketInfo> marketInfo;
    volatile private boolean updateAccountInfo = false;
    volatile private boolean updateMarketInfo = false;
    volatile private boolean retrieveMarketOrders = false;
    volatile private boolean retrieveMarketHistory = false;
    volatile private boolean retrieveAccountOrders = false;
    volatile private boolean retrieveAccountHistory = false;
    volatile BaseTradeApi tradeApi = null;
    volatile long timeInterval = 200;
    volatile Object pair = null;
    volatile Callback callback = null;

    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            try {
                if(updateMarketInfo) {
                    marketInfo = tradeApi.getMarketData(pair, retrieveMarketOrders, retrieveMarketHistory);
                }
                if(updateAccountInfo) {
                    accountInfo = tradeApi.getAccountInfo(pair, retrieveAccountOrders, retrieveAccountHistory);
                }
                if(callback != null) callback.onUpdate(this);
                Thread.sleep(timeInterval);
            } catch (Exception e) {
                if(callback != null) callback.onError(e);
                else e.printStackTrace();
            }
        }
    }

    // Construction:
    public ApiWorker() {
        // do nothing
    }
    public ApiWorker setCallBack(Callback callback) {
        this.callback = callback;
        return this;
    }
    public ApiWorker setPair(Object pair) {
        this.pair = pair;
        return this;
    }
    public ApiWorker setAccountInfoUpdate(boolean updateAccountInfo, boolean retrieveOrders, boolean retrieveHistory) {
        this.updateAccountInfo = updateAccountInfo;
        this.retrieveAccountOrders = retrieveOrders;
        this.retrieveAccountHistory = retrieveHistory;
        return this;
    }
    public ApiWorker setMarketInfoUpdate(boolean updateMarketInfo, boolean retrieveOrders, boolean retrieveHistory) {
        this.updateMarketInfo = updateMarketInfo;
        this.retrieveMarketOrders = retrieveOrders;
        this.retrieveMarketHistory = retrieveHistory;
        return this;
    }
    public ApiWorker setTradeApi(BaseTradeApi tradeApi) {
        this.tradeApi = tradeApi;
        return this;
    }
    public ApiWorker initTradeApiInstance(int accountType, BaseTradeApi.ApiKeyPair apiKeyPair) {
        this.tradeApi = AccountManager.tradeApiInstance(accountType, apiKeyPair);
        return this;
    }
    public ApiWorker setTimeInterval(long ms) {
        this.timeInterval = ms;
        return this;
    }
}
