package com.archean.jautotrading;

import com.archean.jtradeapi.ApiWorker;
import com.archean.jtradeapi.BaseTradeApi;
import com.archean.jtradeapi.Calculator;

import java.math.BigDecimal;
import java.util.Map;

public class RuleCondition {
    public static void makeConditionData(Map<Object, Object> mapData, ApiWorker apiWorker) {
        mapData.put(ApiWorker.ApiDataType.MARKET_PRICES, apiWorker.marketInfo.price);
    }
    public static abstract class BaseCondition {
        protected Object compareType;
        protected Object conditionType;
        protected Object value;
        public BaseCondition(Object compareType, Object conditionType, Object value) {
            this.conditionType = conditionType;
            this.compareType = compareType;
            this.value = value;
        }
        abstract boolean isSatisfied(Map<Object, Object> data) throws Exception;
    }
    public static class PriceCondition extends BaseCondition {
        public PriceCondition(BaseTradeApi.PriceType priceType, Calculator.ArithmeticCompareCondition conditionType, BigDecimal value) {
            super(priceType, conditionType, value);
        }

        @Override boolean isSatisfied(Map<Object, Object> data) throws Exception {
            BigDecimal compareValue = (BigDecimal)this.value;
            BigDecimal comparePrice = BaseTradeApi.getPrice((BaseTradeApi.StandartObjects.Prices) data.get(ApiWorker.ApiDataType.MARKET_PRICES), (BaseTradeApi.PriceType) compareType);
            return Calculator.compare(compareValue, comparePrice, (Calculator.ArithmeticCompareCondition)compareType);
        }
    }
}
