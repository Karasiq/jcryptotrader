package com.archean.jtradeapi;

import java.util.List;

public class ApiWorker implements Runnable {
    public abstract static class Callback {
        public abstract void onUpdate(final ApiWorker.ApiDataType dataType, final Object data);

        public void onError(Exception exc) {
            exc.printStackTrace();
        }
    }
    public static enum ApiDataType {
        MARKET_PRICES, MARKET_DEPTH, MARKET_HISTORY,
        ACCOUNT_BALANCES, ACCOUNT_ORDERS, ACCOUNT_HISTORY
    }

    volatile public BaseTradeApi.StandartObjects.AccountInfo accountInfo;
    volatile public List<BaseTradeApi.StandartObjects.MarketInfo> marketInfo;
    volatile private boolean updateAccountInfo = false;
    volatile private boolean updateMarketInfo = false;
    volatile private boolean retrieveMarketOrders = false;
    volatile private boolean retrieveMarketHistory = false;
    volatile private boolean retrieveAccountOrders = false;
    volatile private boolean retrieveAccountHistory = false;
    volatile public BaseTradeApi tradeApi = null;
    volatile long timeInterval = 100;
    volatile public boolean paused = false;
    volatile Object pair = null;
    volatile Callback callback = null;

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (!paused) {
                    if (updateMarketInfo) {
                        marketInfo = tradeApi.getMarketData(pair, retrieveMarketOrders, retrieveMarketHistory);
                    }
                    if (updateAccountInfo) {
                        accountInfo = tradeApi.getAccountInfo(pair, retrieveAccountOrders, retrieveAccountHistory);
                    }
                    if (callback != null) callback.onUpdate(this);
                }
                Thread.sleep(timeInterval);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
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

    public ApiWorker initTradeApiInstance(AccountManager.Account account) {
        this.tradeApi = AccountManager.tradeApiInstance(account);
        return this;
    }

    public ApiWorker setTimeInterval(long ms) {
        this.timeInterval = ms;
        return this;
    }
}
