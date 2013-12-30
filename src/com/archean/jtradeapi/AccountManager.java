package com.archean.jtradeapi;

public class AccountManager {
    public static class AccountType {
        public static final int ACCOUNT_CRYPTSY = 0;
        public static final int ACCOUNT_BTCE = 1;
    }
    public static BaseTradeApi tradeApiInstance(int accountType, BaseTradeApi.ApiKeyPair pair) {
        switch(accountType) {
            case AccountType.ACCOUNT_CRYPTSY:
                return new CryptsyTradeApi(pair);
            case AccountType.ACCOUNT_BTCE:
                return new BtceTradeApi(pair);
            default:
                throw new IllegalArgumentException("Unknown trade account type");
        }
    }
}
