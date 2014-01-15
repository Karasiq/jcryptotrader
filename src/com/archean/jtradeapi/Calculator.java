package com.archean.jtradeapi;

import java.math.BigDecimal;

public class Calculator {
    public static double MINIMAL_AMOUNT = 0.00000001; // 1 Satoshi

    public static double totalWithFee(int orderType, double price, double amount, double feePercent) {
        return orderType == BaseTradeApi.Constants.ORDER_BUY ? amount * price * ((100.0 + feePercent) / 100.0) : amount * price / ((100.0 + feePercent) / 100.0);
    }
    public static double balancePercentAmount(double balance, double balancePercent, int orderType, double price, double feePercent) {
        return orderType == BaseTradeApi.Constants.ORDER_BUY ? (balance / price) * (balancePercent * 1.0 / 100.0) / ((100.0 + feePercent) / 100.0) : (balance * balancePercent * 1.0 / 100.0);
    }
    public static double priceChangePercent(double p1, double p2) {
        return ((p2 - p1) / p1) * 100.0;
    }

    public enum ArithmeticCompareCondition {
        EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL
    }

    public static boolean compare(BigDecimal compareWant, BigDecimal compareActual, ArithmeticCompareCondition conditionType) {
        int compareResult = compareActual.compareTo(compareWant);
        switch (conditionType) {
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
