package com.archean.jautotrading;

import com.archean.jtradeapi.ApiWorker;
import com.archean.jtradeapi.BaseTradeApi;
import com.archean.jtradeapi.Calculator;
import com.archean.jtradeapi.Utils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class RuleWorker implements AutoCloseable {
    abstract static class RuleCallback {
        abstract void onSatisfied();
        void onError(Exception e) {
            e.printStackTrace();
        }
    }
    public volatile RuleCallback callback = null;

    public static void makeConditionData(Map<Object, Object> mapData, BaseCondition condition, ApiWorker apiWorker) {
        if(condition instanceof PriceCondition) {
            mapData.put(ApiWorker.ApiDataType.MARKET_PRICES, apiWorker.marketInfo.price);
        }
    }
    abstract class BaseCondition {
        protected Object compareType;
        protected Object conditionType;
        protected Object value;
        public BaseCondition(Object compareType, Object conditionType, Object value) {
            this.conditionType = conditionType;
            this.compareType = compareType;
            this.value = value;
        }
        abstract boolean satisfied(Map<Object, Object> data) throws Exception;
    }
    public class PriceCondition extends BaseCondition {
        public PriceCondition(BaseTradeApi.PriceType priceType, Calculator.ArithmeticCompareCondition conditionType, BigDecimal value) {
            super(priceType, conditionType, value);
        }

        @Override boolean satisfied(Map<Object, Object> data) throws Exception {
            BigDecimal compareValue = (BigDecimal)this.value;
            BigDecimal comparePrice = BaseTradeApi.getPrice((BaseTradeApi.StandartObjects.Prices) data.get(ApiWorker.ApiDataType.MARKET_PRICES), (BaseTradeApi.PriceType) compareType);
            return Calculator.compare(compareValue, comparePrice, (Calculator.ArithmeticCompareCondition)compareType);
        }
    }

    Thread ruleThread = null;
    BaseCondition condition;
    public volatile boolean satisfied = false;
    public RuleWorker(BaseCondition condition) {
        this.condition = condition;
    }
    private void stopThread() {
        if(ruleThread != null && ruleThread.isAlive()) {
            ruleThread.interrupt();
            ruleThread = null;
        }
    }
    public void attachToApiWorker(final ApiWorker apiWorker) {
        stopThread();
        satisfied = false;
        ruleThread = new Thread(new Utils.Threads.CycledRunnable() {
            private Map<Object, Object> mapData = new HashMap<>();
            @Override protected int onError(Exception e) {
                if(callback != null) callback.onError(e);
                else e.printStackTrace();
                return STOP_CYCLE;
            }
            @Override protected int cycle() throws Exception {
                makeConditionData(mapData, condition, apiWorker);
                if(condition.satisfied(mapData)) {
                    if(callback != null) callback.onSatisfied();
                    satisfied = true;
                    return STOP_CYCLE;
                }
                return 200;
            }
        });
    }
    public void close() {
        stopThread();
    }
}