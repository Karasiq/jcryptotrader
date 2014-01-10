package com.archean.jautotrading;

import com.archean.jtradeapi.ApiWorker;
import com.archean.jtradeapi.BaseTradeApi;
import com.archean.jtradeapi.Calculator;

import java.math.BigDecimal;
import java.math.MathContext;

public class RuleAction {
    public static abstract class BaseAction implements Runnable {
        ApiWorker apiWorker;
        public BaseAction(ApiWorker apiWorker) {
            this.apiWorker = apiWorker;
        }
    }
    public static class TradeAction extends BaseAction {
        public abstract static class Callback {
            abstract public void onSuccess(long orderId);
            abstract public void onError(Exception e);
        }
        Callback callback = null;

        public static int AMOUNT_TYPE_CONSTANT = 0;
        public static int AMOUNT_TYPE_BALANCE_PERCENT = 1;

        public TradeAction(ApiWorker apiWorker) {
            super(apiWorker);
        }

        public int tradeType = BaseTradeApi.Constants.ORDER_BUY;
        public BaseTradeApi.PriceType priceType;
        public int amountType = AMOUNT_TYPE_CONSTANT;
        public BigDecimal priceCustom = null;
        public BigDecimal amount;
        public double feePercent;

        @Override public void run() {
            if(priceCustom == null) {
                priceCustom = BaseTradeApi.getPrice(apiWorker.marketInfo.price, priceType);
            }
            BaseTradeApi.StandartObjects.CurrencyPair pair = null;
            try {
                pair = apiWorker.tradeApi.getCurrencyPairs().get(apiWorker.getPair());
            } catch (Exception e) {
                e.printStackTrace();
                if(callback != null) {
                    callback.onError(e);
                }
                return;
            }
            if(amountType == AMOUNT_TYPE_BALANCE_PERCENT) {
                amount = new BigDecimal(Calculator.balancePercentAmount(apiWorker.accountInfo.balance.getBalance(tradeType == BaseTradeApi.Constants.ORDER_SELL ? pair.firstCurrency : pair.secondCurrency), amount.doubleValue(), tradeType, priceCustom.doubleValue(), feePercent), MathContext.DECIMAL64);
            }
            try {
                long orderId = apiWorker.tradeApi.createOrder(apiWorker.getPair(), tradeType, amount.doubleValue(), priceCustom.doubleValue());
                callback.onSuccess(orderId);
            } catch (Exception e) {
                e.printStackTrace();
                if(callback != null) {
                    callback.onError(e);
                }
            }
        }
    }
}
