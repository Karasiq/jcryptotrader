package com.archean.jtradebot;

import com.google.gson.reflect.TypeToken;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.security.Timestamp;
import java.util.*;

public class BtceTradeApi extends BaseTradeApi { // BTC-E trade api
    private StandartObjects.CurrencyPairMapper pairMapper = new StandartObjects.CurrencyPairMapper();
    private void addPairToMapper(String first, String second) {
        StandartObjects.CurrencyPair pair = new StandartObjects.CurrencyPair();
        pair.firstCurrency = first.toUpperCase();
        pair.secondCurrency = second.toUpperCase();
        pair.pairId = (first + "_" + second).toLowerCase();
        pair.pairName = (first + "/" + second).toUpperCase();
        pairMapper.put(pair.pairId, pair);
    }
    public BtceTradeApi() {
        super();
        // Unfortunately i don't know how to retrieve markets list dynamically, so...
        // Crypto/fiat:
        addPairToMapper("BTC", "USD");
        addPairToMapper("BTC", "EUR");
        addPairToMapper("BTC", "RUR");

        addPairToMapper("LTC", "USD");
        addPairToMapper("LTC", "RUR");
        addPairToMapper("LTC", "EUR");

        addPairToMapper("NMC", "USD");
        addPairToMapper("NVC", "USD");
        addPairToMapper("PPC", "USD");

        // Crypto/crypto:
        addPairToMapper("LTC", "BTC");
        addPairToMapper("NMC", "BTC");
        addPairToMapper("NVC", "BTC");
        addPairToMapper("PPC", "BTC");
        addPairToMapper("TRC", "BTC");
        addPairToMapper("FTC", "BTC");
        addPairToMapper("XPM", "BTC");

        // Fiat/fiat:
        addPairToMapper("USD", "RUR");
        addPairToMapper("EUR", "USD");
    }
    private static class BtceObjects {
        static class TickerData { // Current market data
            double high;
            double low;
            double avg;
            double vol;
            double vol_cur;
            double last;
            double buy;
            double sell;
            long updated;
            long server_time;
        }
        /* class Trade { // History
            long date;
            double price;
            double amount;
            long tid;
            String price_currency;
            String item;
            String trade_type; // ask/bid
        } */
        static class Depth {
            List<List<Double>> asks = new ArrayList<List<Double>>(); // [0] - price, [1] - amount
            List<List<Double>> bids = new ArrayList<List<Double>>();
        }
        static class AccountInfo {
            TreeMap<String, Double> funds = new TreeMap<String, Double>();
            private class Rights {
                boolean info;
                boolean trade;
            }
            Rights rights = new Rights();
            int transaction_count;
            int open_orders;
            long server_time;
        }
        private static class Order {
            String pair;
            String type; // sell/buy
            double amount;
            double rate;
            long timestamp_created;
            int status;
        }
        private static class OpenOrderStatus {
            double received;
            double remains;
            long order_id;
            TreeMap<String, Double> funds = new TreeMap<String, Double>();
        }
        private static class CancelOrderStatus {
            long order_id;
            TreeMap<String, Double> funds = new TreeMap<String, Double>();
        }
    }

    // Internal:
    private String publicApiFormatUrl(String pair, String method) {
        return "https://btc-e.com/api/2/" + pair + "/" + method;
    }

    private BtceObjects.TickerData internalGetTicker(String pair) throws IOException {
        class TickerStub {
            BtceObjects.TickerData ticker = new BtceObjects.TickerData();
        }
        String url = publicApiFormatUrl(pair, "ticker");
        String json = executeRequest(false, url, null, Constants.REQUEST_GET);
        TreeMap<String, BtceObjects.TickerData> map = jsonParser.fromJson(json, new TypeToken<TreeMap<String, BtceObjects.TickerData>>(){}.getType());
        return map.get("ticker");
    }

    private BtceObjects.Depth internalGetDepth(String pair) throws IOException {
        String url = publicApiFormatUrl(pair, "depth");
        String json = executeRequest(false, url, null, Constants.REQUEST_GET);
        return jsonParser.fromJson(json, new TypeToken<BtceObjects.Depth>(){}.getType());
    }

    private StandartObjects.MarketInfo internalUnifiedGetMarketData(Object pair, boolean retrieveOrders) throws IOException {
        BtceObjects.TickerData tickerData = internalGetTicker((String)pair);
        StandartObjects.MarketInfo marketInfo = new StandartObjects.MarketInfo();
        marketInfo.pairId = pair;

        StandartObjects.CurrencyPair pairInfo = pairMapper.get(pair);
        marketInfo.pairName = pairInfo.pairName;
        marketInfo.firstCurrency = pairInfo.firstCurrency;
        marketInfo.secondCurrency = pairInfo.secondCurrency;

        marketInfo.price.average = tickerData.avg;
        marketInfo.price.low = tickerData.low;
        marketInfo.price.high = tickerData.high;
        marketInfo.price.buy = tickerData.buy;
        marketInfo.price.sell = tickerData.sell;
        marketInfo.price.last = tickerData.last;
        if(retrieveOrders) {
            BtceObjects.Depth depth = internalGetDepth((String)pair);
            for(List<Double> entry : depth.asks) {
                marketInfo.depth.sellOrders.add(new StandartObjects.Order(entry.get(0), entry.get(1)));
            }
            for(List<Double> entry : depth.bids) {
                marketInfo.depth.buyOrders.add(new StandartObjects.Order(entry.get(0), entry.get(1)));
            }
        }
        return marketInfo;
    }

    Map<String, Double> feePercentCache = new HashMap<String, Double>();
    private double internalGetFeePercent(String pair) throws IOException {
        if(!feePercentCache.containsKey(pair)) {
            class FeeReturn {
                double trade;
            }
            String url = publicApiFormatUrl(pair, "fee");
            String json = executeRequest(false, url, null, Constants.REQUEST_GET);
            FeeReturn feeReturn = jsonParser.fromJson(json, new TypeToken<FeeReturn>(){}.getType());
            feePercentCache.put(pair, feeReturn.trade);
            return feeReturn.trade;
        }
        else return feePercentCache.get(pair);
    }

    private final String privateApiUrl = "https://btc-e.com/tapi";
    private ApiStatus<BtceObjects.AccountInfo> internalGetAccountInfo() throws IOException {
        List<NameValuePair> httpParameters = new ArrayList<NameValuePair>();
        httpParameters.add(new BasicNameValuePair("method", "getInfo"));
        String json = executeRequest(true, privateApiUrl, httpParameters, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<BtceObjects.AccountInfo>>(){}.getType());
    }
    private ApiStatus<TreeMap<Long, BtceObjects.Order>> internalGetOpenOrders(String pair) throws IOException {
        List<NameValuePair> httpParameters = new ArrayList<NameValuePair>();
        httpParameters.add(new BasicNameValuePair("method", "ActiveOrders"));
        if(pair != null && !pair.isEmpty() && !pair.equals("")) {
            httpParameters.add(new BasicNameValuePair("pair", pair));
        }
        String json = executeRequest(true, privateApiUrl, httpParameters, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<TreeMap<Long, BtceObjects.Order>>>(){}.getType());
    }
    private ApiStatus<BtceObjects.OpenOrderStatus> internalOpenOrder(String pair, int type, double amount, double price) throws IOException {
        List<NameValuePair> httpParameters = new ArrayList<NameValuePair>();
        httpParameters.add(new BasicNameValuePair("method", "Trade"));
        httpParameters.add(new BasicNameValuePair("pair", pair));
        httpParameters.add(new BasicNameValuePair("type", type == Constants.ORDER_SELL ? "sell" : "buy"));
        httpParameters.add(new BasicNameValuePair("rate", Utils.Strings.formatNumber(price)));
        httpParameters.add(new BasicNameValuePair("amount", Utils.Strings.formatNumber(amount)));
        String json = executeRequest(true, privateApiUrl, httpParameters, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<BtceObjects.OpenOrderStatus>>(){}.getType());
    }
    private ApiStatus<BtceObjects.CancelOrderStatus> internalCancelOrder(long orderId) throws IOException {
        List<NameValuePair> httpParameters = new ArrayList<NameValuePair>();
        httpParameters.add(new BasicNameValuePair("method", "CancelOrder"));
        httpParameters.add(new BasicNameValuePair("order_id", Long.toString(orderId)));
        String json = executeRequest(true, privateApiUrl, httpParameters, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<BtceObjects.CancelOrderStatus>>(){}.getType());
    }

    // Public:
    public StandartObjects.CurrencyPairMapper getCurrencyPairs() throws IOException, TradeApiError {
        return pairMapper;
    }
    public List<StandartObjects.MarketInfo> getMarketData(Object pair, boolean retrieveOrders) throws TradeApiError, IOException {
        List<StandartObjects.MarketInfo> marketInfoList = new ArrayList<StandartObjects.MarketInfo>();
        if(pair != null && !pair.equals("")) {
            marketInfoList.add(internalUnifiedGetMarketData(pair, retrieveOrders));
        } else for(Map.Entry<Object, StandartObjects.CurrencyPair> entry : pairMapper.entrySet()) {
            marketInfoList.add(internalUnifiedGetMarketData(entry.getKey(), retrieveOrders));
        }
        return marketInfoList;
    }
    public StandartObjects.AccountInfo getAccountInfo(boolean retrieveOrders) throws TradeApiError, IOException {
        ApiStatus<BtceObjects.AccountInfo> accountInfoApiStatus = internalGetAccountInfo();
        if(accountInfoApiStatus.success != 1) {
            throw new TradeApiError("Error retrieving account info (" + accountInfoApiStatus.error + ")");
        }
        StandartObjects.AccountInfo accountInfo = new StandartObjects.AccountInfo();
        accountInfo.balance = new StandartObjects.AccountInfo.AccountBalance(accountInfoApiStatus.result.funds);
        if(retrieveOrders) {
            ApiStatus<TreeMap<Long, BtceObjects.Order>> ordersStatus = internalGetOpenOrders(null);
            if(ordersStatus.success != 1) {
                throw new TradeApiError("Error retrieving orders info (" + ordersStatus.error + ")");
            }
            for(Map.Entry<Long, BtceObjects.Order> entry : ordersStatus.result.entrySet()) {
                StandartObjects.Order order = new StandartObjects.Order();
                order.id = entry.getKey();
                order.amount = entry.getValue().amount;
                order.pair = entry.getValue().pair;
                order.price = entry.getValue().rate;
                order.type = entry.getValue().type.equals("sell") ? Constants.ORDER_SELL : Constants.ORDER_BUY;
                order.time = new Date(entry.getValue().timestamp_created * 1000);
                accountInfo.orders.add(order);
            }
        }
        return accountInfo;
    }
    public double calculateFees(Object pair, int orderType, double quantity, double price) throws TradeApiError, IOException {
        double percent = internalGetFeePercent((String)pair);
        return (quantity * price) * percent / 100.0;
    }

    public long createOrder(Object pair, int orderType, double quantity, double price) throws IOException, TradeApiError {
        ApiStatus<BtceObjects.OpenOrderStatus> orderStatus = internalOpenOrder((String)pair, orderType, quantity, price);
        if(orderStatus.success != 1) {
            throw new TradeApiError("Failed to create order (" + orderStatus.error + ")");
        }
        return orderStatus.result.order_id;
    }
    public boolean cancelOrder(long orderId) throws TradeApiError, IOException {
        ApiStatus<BtceObjects.CancelOrderStatus> orderStatus = internalCancelOrder(orderId);
        if(orderStatus.success != 1) {
            throw new TradeApiError("Failed to cancel order (" + orderStatus.error + ")");
        }
        return true;
    }
}
