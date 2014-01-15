package com.archean.jautotrading;

import com.archean.jtradeapi.ApiWorker;

import java.util.*;

public class MarketRule {
    public RuleCondition.BaseCondition condition;
    public RuleAction.BaseAction action;

    public MarketRule(RuleCondition.BaseCondition condition, RuleAction.BaseAction action) {
        this.condition = condition;
        this.action = action;
    }
    public MarketRule(RuleCondition.BaseCondition condition) {
        this(condition, null);
    }

    public static class MarketRuleList extends ArrayList<MarketRule> { // Batch checker
        public static abstract class MarketRuleListCallback {
            void onError(Exception e) {
                e.printStackTrace();
            }
            abstract void onSuccess(MarketRule rule);
        }
        public enum MarketRuleExecutionType {
            QUEUED, PARALLEL, ONLY_FIRST_SATISFIED
        }
        MarketRuleExecutionType executionType = MarketRuleExecutionType.PARALLEL;
        public MarketRuleListCallback callback = null;
        private Map<Object, Object> ruleData = new HashMap<>(); // Cache
        public void checkRules(final ApiWorker worker) {
            RuleCondition.makeConditionData(ruleData, worker);
            ListIterator<MarketRule> ruleIterator = this.listIterator();
            while(ruleIterator.hasNext()) {
                try {
                    if(executionType == MarketRuleExecutionType.QUEUED && ruleIterator.nextIndex() != 0) {
                        break;
                    }
                    MarketRule rule = ruleIterator.next();
                    if(rule.condition.isSatisfied(ruleData)) {
                        if(rule.action != null) {
                            Thread actionThread = new Thread(rule.action);
                            actionThread.run();
                        }
                        if(callback != null) {
                            callback.onSuccess(rule);
                        }
                        if(executionType == MarketRuleExecutionType.ONLY_FIRST_SATISFIED) {
                            this.clear();
                            break;
                        }
                        ruleIterator.remove(); // Rule executed
                    }
                } catch (Exception e) {
                    if(callback != null) {
                        callback.onError(e);
                    }
                    else e.printStackTrace();
                }
            }
        }
    }
}
