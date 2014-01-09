package com.archean.jautotrading;

import com.archean.jtradeapi.ApiWorker;
import com.archean.jtradeapi.BaseTradeApi;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

public class RuleWorker implements AutoCloseable {
    abstract class RuleCallback {
        abstract void onSatisfied();
        void onError(Exception e) {
            e.printStackTrace();
        }
    }
    public RuleCallback callback = null;

    enum ArithmeticCompareCondition {
        EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL
    }
    enum PriceType {
        LAST, ASK, BID, HIGH, LOW, AVG
    }
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
        public PriceCondition(PriceType priceType, ArithmeticCompareCondition conditionType, BigDecimal value) {
            super(priceType, conditionType, value);
        }
        private BigDecimal getPrice(BaseTradeApi.StandartObjects.Prices prices, PriceType priceType) {
            double price;
            switch((PriceType)compareType) {
                case LAST:
                    price = prices.last;
                    break;
                case ASK:
                    price = prices.buy;
                    break;
                case BID:
                    price = prices.sell;
                    break;
                case HIGH:
                    price = prices.high;
                    break;
                case LOW:
                    price = prices.low;
                    break;
                case AVG:
                    price = prices.average;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown price type");
            }
            return new BigDecimal(price, MathContext.DECIMAL64);
        }
        @Override boolean satisfied(Map<Object, Object> data) throws Exception {
            BigDecimal compareValue = (BigDecimal)this.value;
            BigDecimal comparePrice = getPrice((BaseTradeApi.StandartObjects.Prices)data.get(ApiWorker.ApiDataType.MARKET_PRICES), (PriceType)compareType);
            int compareResult = compareValue.compareTo(comparePrice);
            switch ((ArithmeticCompareCondition)conditionType) {
                case EQUAL:
                    return compareResult == 0;
                case GREATER:
                    return compareResult == 1;
                case GREATER_OR_EQUAL:
                    return compareResult == 0 || compareResult == 1;
                case LESS:
                    return compareResult == -1;
                case LESS_OR_EQUAL:
                    return compareResult == -1 || compareResult == 0;
                default:
                    throw new UnknownError();
            }
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
        ruleThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Map<Object, Object> mapData = new HashMap<>();
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        makeConditionData(mapData, condition, apiWorker);
                        if(condition.satisfied(mapData)) {
                            if(callback != null) callback.onSatisfied();
                            satisfied = true;
                            break;
                        }
                        Thread.sleep(200);
                    } catch(InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        if(callback != null) callback.onError(e);
                        else e.printStackTrace();
                        break;
                    }
                }
            }
        });
    }
    public void close() {
        stopThread();
    }
}
