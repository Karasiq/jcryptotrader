package com.archean.jtradeapi;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Exchanger;

public class ApiWorker implements AutoCloseable {
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

    private class ApiWorkerTask extends Utils.Threads.CycledRunnable {
        ApiDataType apiDataType;

        public ApiWorkerTask(ApiDataType dataType) {
            this.apiDataType = dataType;
        }

        private Object retrieveData() throws Exception {
            switch (apiDataType) {
                case MARKET_PRICES:
                    return tradeApi.getMarketPrices(pair);
                case MARKET_DEPTH:
                    return tradeApi.getMarketDepth(pair);
                case MARKET_HISTORY:
                    return tradeApi.getMarketHistory(pair);
                case ACCOUNT_BALANCES:
                    return tradeApi.getAccountBalances();
                case ACCOUNT_ORDERS:
                    return tradeApi.getAccountOpenOrders(pair);
                case ACCOUNT_HISTORY:
                    return tradeApi.getAccountHistory(pair);
                default:
                    throw new IllegalArgumentException();
            }
        }

        private void updateWorkerData(Object data) {
            switch (apiDataType) {
                case MARKET_PRICES:
                    marketInfo.price = (BaseTradeApi.StandartObjects.Prices) data;
                    break;
                case MARKET_DEPTH:
                    marketInfo.depth = (BaseTradeApi.StandartObjects.Depth) data;
                    break;
                case MARKET_HISTORY:
                    marketInfo.history = (List<BaseTradeApi.StandartObjects.Order>) data;
                    break;
                case ACCOUNT_BALANCES:
                    accountInfo.balance = (BaseTradeApi.StandartObjects.AccountInfo.AccountBalance) data;
                    break;
                case ACCOUNT_ORDERS:
                    accountInfo.orders = (List<BaseTradeApi.StandartObjects.Order>) data;
                    break;
                case ACCOUNT_HISTORY:
                    accountInfo.history = (List<BaseTradeApi.StandartObjects.Order>) data;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override protected int onError(Exception e) {
            if (callback != null)
                callback.onError(new Exception("ApiWorker error: " + e.getLocalizedMessage(), e));
            else
                e.printStackTrace();
            return 10 * 1000; // 10s
        }

        private Object data = null;
        @Override protected int cycle() throws Exception {
            data = retrieveData();
            if (data != null && !Thread.currentThread().isInterrupted()) {
                updateWorkerData(data);
                if (callback != null) {
                    callback.onUpdate(apiDataType, data);
                }
                return timeInterval;
            } else {
                return STOP_CYCLE;
            }
        }
    }

    volatile public BaseTradeApi.StandartObjects.AccountInfo accountInfo = new BaseTradeApi.StandartObjects.AccountInfo();
    volatile public BaseTradeApi.StandartObjects.MarketInfo marketInfo = new BaseTradeApi.StandartObjects.MarketInfo();
    volatile public BaseTradeApi tradeApi = null;
    volatile int timeInterval = 200;
    volatile Object pair = null;
    volatile public Callback callback = null;
    private Map<ApiDataType, Thread> threadMap = new HashMap<>();

    synchronized public boolean isThreadRunning(final ApiDataType dataType) {
        return threadMap.containsKey(dataType) && threadMap.get(dataType).isAlive();
    }

    synchronized public void stopThread(final ApiDataType dataType) {
        Thread workerThread = threadMap.get(dataType);
        if (workerThread != null) {
            if (workerThread.isAlive()) {
                workerThread.interrupt();
            }
            threadMap.remove(dataType);
        }
    }

    synchronized public void startThread(final ApiDataType dataType) {
        stopThread(dataType);
        Thread workerThread = new Thread(new ApiWorkerTask(dataType));
        threadMap.put(dataType, workerThread);
        workerThread.start();
    }

    synchronized public void setActiveThreads(final List<ApiDataType> activeThreads) {
        Map<ApiDataType, Thread> newMap = new HashMap<>();
        for (Map.Entry<ApiDataType, Thread> entry : threadMap.entrySet()) {
            if (!activeThreads.contains(entry.getKey())) {
                entry.getValue().interrupt();
            } else {
                newMap.put(entry.getKey(), entry.getValue());
            }
        }
        for (ApiDataType dataType : activeThreads) {
            if (!newMap.containsKey(dataType)) {
                Thread thread = new Thread(new ApiWorkerTask(dataType));
                newMap.put(dataType, thread);
                thread.start();
            }
        }
        threadMap = newMap;
    }

    synchronized public void setActiveThreads(final ApiDataType[] activeThreads) {
        setActiveThreads(new ArrayList<>(Arrays.asList(activeThreads)));
    }

    synchronized public void stopAllThreads() {
        for (ApiDataType dataType : threadMap.keySet()) {
            threadMap.get(dataType).interrupt();
        }
        threadMap.clear();
    }

    public void close() {
        stopAllThreads();
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

    public final Object getPair() {
        return this.pair;
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

    public ApiWorker setTimeInterval(int ms) {
        this.timeInterval = ms;
        return this;
    }
}
