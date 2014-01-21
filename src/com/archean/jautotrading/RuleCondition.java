/*
 * jCryptoTrader trading client
 * Copyright (C) 2014 1M4SKfh83ZxsCSDmfaXvfCfMonFxMa5vvh (BTC public key)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package com.archean.jautotrading;

import com.archean.jtradeapi.ApiWorker;
import com.archean.jtradeapi.BaseTradeApi;
import com.archean.jtradeapi.Calculator;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

public class RuleCondition {
    public static void makeConditionData(Map<Object, Object> mapData, ApiWorker apiWorker) {
        mapData.put(ApiWorker.ApiDataType.MARKET_PRICES, apiWorker.marketInfo.price);
    }

    public static abstract class BaseCondition implements Serializable {
        public Object compareType = null;
        public Object conditionType = null;
        public Object value = null;

        public BaseCondition(Object conditionType, Object compareType, Object value) {
            this.conditionType = conditionType;
            this.compareType = compareType;
            this.value = value;
        }

        abstract boolean isSatisfied(Map<Object, Object> data) throws Exception;
    }

    public enum ConditionSatisfyingType {
        ONLY_ONE, ALL, QUEUE
    }
    public static class ConditionList extends ArrayList<BaseCondition> {
        private @Getter @Setter ConditionSatisfyingType conditionSatisfyingType = ConditionSatisfyingType.ALL;
        private final Map<Object, Object> data = new HashMap<>();
        public boolean isSatisfied(ApiWorker apiWorker) throws Exception {
            makeConditionData(data, apiWorker); // Refresh data
            int satisfied = 0;
            ListIterator<BaseCondition> iterator = this.listIterator();
            while(iterator.hasNext()) {
                BaseCondition condition = iterator.next();
                if(condition.isSatisfied(data)) switch (conditionSatisfyingType) {
                    case ONLY_ONE: // first rule satisfied
                        return true;
                    case ALL:
                        satisfied++;
                        break;
                    case QUEUE:
                        iterator.remove();
                        break;
                } else switch (conditionSatisfyingType) {
                    case ALL:
                    case QUEUE:
                        return false;
                    default:
                        break;
                }
            }
            return conditionSatisfyingType == ConditionSatisfyingType.ALL ? satisfied == this.size() : this.size() == 0; // ALL/QUEUE
        }
    }

    public static class PriceCondition extends BaseCondition {
        public PriceCondition(BaseTradeApi.PriceType priceType, Calculator.ArithmeticCompareCondition conditionType, BigDecimal value) {
            super(priceType, conditionType, value);
        }

        @Override
        boolean isSatisfied(Map<Object, Object> data) throws Exception {
            BigDecimal compareValue = (BigDecimal) this.value;
            BigDecimal comparePrice = BaseTradeApi.getPrice((BaseTradeApi.StandartObjects.Prices) data.get(ApiWorker.ApiDataType.MARKET_PRICES), (BaseTradeApi.PriceType) conditionType);
            return Calculator.compare(compareValue, comparePrice, (Calculator.ArithmeticCompareCondition) compareType);
        }
    }
}
