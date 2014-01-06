package com.archean.jtradeapi;

import com.google.gson.reflect.TypeToken;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

public class CryptsyTradeApi extends BaseTradeApi {
    private static final String PublicApiUrl = "http://pubapi.cryptsy.com/api.php";
    private static final String PrivateApiUrl = "https://www.cryptsy.com/api";

    public CryptsyTradeApi(ApiKeyPair keyPair) {
        super(keyPair);
    }

    private class CryptsyObjects { // Cryptsy api specific objects

        class DepthInfo {
            List<List<Double>> sell = new ArrayList<List<Double>>();
            List<List<Double>> buy = new ArrayList<List<Double>>();
        }

        class TradeOrder {
            double price;
            double quantity;
            double total;
        }

        class TradeHistory extends TradeOrder {
            int id;
            Date time;
        }

        class CurrencyPair {
            int marketid;
            String label;
            String primaryname;
            String primarycode;
            String secondaryname;
            String secondarycode;
        }

        class OrderBookData extends CurrencyPair {
            List<TradeOrder> sellorders = new ArrayList<TradeOrder>();
            List<TradeOrder> buyorders = new ArrayList<TradeOrder>();
        }

        class OrderBook extends TreeMap<String, OrderBookData> {
            public OrderBook() {
                super();
            }
        }

        class MarketData extends OrderBookData {
            double lasttradeprice;
            double volume;
            Date lasttradetime;
            List<TradeHistory> recenttrades = new ArrayList<TradeHistory>();
        }

        class Markets {
            TreeMap<String, MarketData> markets = new TreeMap<String, MarketData>();
        }

        class AccountInfo {
            TreeMap<String, Double> balances_available = new TreeMap<String, Double>();
            TreeMap<String, Double> balances_hold = new TreeMap<String, Double>();
            int openordercount;
            int servertimestamp;
            String servertimezone;
            Date serverdatetime;
        }

        class Trade { // getAccountHistory
            long tradeid; // An integer identifier for this trade
            String tradetype; // Buy/Sell
            Date datetime;
            int marketid;
            double tradeprice;
            double quantity;
            double fee;
            double total;
            String initiate_ordertype; // Buy/Sell
            long order_id; // Original order id this trade was executed against
        }

        class Order extends TradeOrder {
            long orderid;
            Date created;
            String ordertype;
            double orig_quantity;
            int marketid;
        }

        class MarketDataPrivate {
            int marketid;
            String label;
            String primary_currency_code;
            String primary_currency_name;
            String secondary_currency_code;
            String secondary_currency_name;
            double current_volume;
            double last_trade;
            double high_trade;
            double low_trade;
            Date created;
        }

        class Fee {
            double fee; // The that would be charged for provided inputs
            double net; // The net total with fees
        }

        class OrderCreateStatus {
            int success;
            long orderid;
            String moreinfo;
            String error;
        }
    }

    // Internal
    @Deprecated
    private ApiStatus<CryptsyObjects.Markets> internalGetMarketData(int marketId) throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        if (marketId == 0) {
            arguments.add(new BasicNameValuePair("method", "marketdatav2"));
        } else {
            arguments.add(new BasicNameValuePair("method", "singlemarketdata"));
            arguments.add(new BasicNameValuePair("marketid", Integer.toString(marketId)));
        }
        String json = executeRequest(false, PublicApiUrl, arguments, Constants.REQUEST_GET);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<CryptsyObjects.Markets>>() {
        }.getType());
    }

    @Deprecated
    private ApiStatus<CryptsyObjects.OrderBook> internalGetOrders(int marketId) throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        if (marketId == 0) {
            arguments.add(new BasicNameValuePair("method", "orderdata"));
        } else {
            arguments.add(new BasicNameValuePair("method", "singleorderdata"));
            arguments.add(new BasicNameValuePair("marketid", Integer.toString(marketId)));
        }
        String json = executeRequest(false, PublicApiUrl, arguments, Constants.REQUEST_GET);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<CryptsyObjects.OrderBook>>() {
        }.getType());
    }

    private ApiStatus<CryptsyObjects.AccountInfo> internalGetAccountInfo() throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        arguments.add(new BasicNameValuePair("method", "getinfo"));
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<CryptsyObjects.AccountInfo>>() {
        }.getType());
    }

    private ApiStatus<List<CryptsyObjects.MarketDataPrivate>> internalGetMarketDataPrivate() throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        arguments.add(new BasicNameValuePair("method", "getmarkets"));
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<ArrayList<CryptsyObjects.MarketDataPrivate>>>() {
        }.getType());
    }

    private ApiStatus<List<CryptsyObjects.Trade>> internalGetAccountHistory(Integer marketId) throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        if (marketId == 0) {
            arguments.add(new BasicNameValuePair("method", "allmytrades"));
        } else {
            arguments.add(new BasicNameValuePair("method", "mytrades"));
            arguments.add(new BasicNameValuePair("marketid", Integer.toString(marketId)));
        }
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<ArrayList<CryptsyObjects.Trade>>>() {
        }.getType());
    }

    private ApiStatus<List<CryptsyObjects.Order>> internalGetMyOrders(Integer marketId) throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        if (marketId == 0) {
            arguments.add(new BasicNameValuePair("method", "allmyorders"));
        } else {
            arguments.add(new BasicNameValuePair("method", "myorders"));
            arguments.add(new BasicNameValuePair("marketid", Integer.toString(marketId)));
        }
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<ArrayList<CryptsyObjects.Order>>>() {
        }.getType());
    }

    private ApiStatus<CryptsyObjects.DepthInfo> internalGetMarketDepth(int marketId) throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        arguments.add(new BasicNameValuePair("method", "depth"));
        arguments.add(new BasicNameValuePair("marketid", Integer.toString(marketId)));
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<CryptsyObjects.DepthInfo>>() {
        }.getType());
    }

    private ApiStatus<List<CryptsyObjects.Trade>> internalGetMarketHistory(Integer marketId) throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        arguments.add(new BasicNameValuePair("method", "markettrades"));
        arguments.add(new BasicNameValuePair("marketid", Integer.toString(marketId)));
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<List<CryptsyObjects.Trade>>>() {
        }.getType());
    }

    @Deprecated
    private ApiStatus<CryptsyObjects.Fee> internalCalculateFees(int orderType, double quantity, double price) throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        arguments.add(new BasicNameValuePair("method", "calculatefees"));
        arguments.add(new BasicNameValuePair("ordertype", orderType == Constants.ORDER_BUY ? "Buy" : "Sell"));
        arguments.add(new BasicNameValuePair("quantity", Utils.Strings.formatNumber(quantity)));
        arguments.add(new BasicNameValuePair("price", Utils.Strings.formatNumber(price)));
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<CryptsyObjects.Fee>>() {
        }.getType());
    }

    private CryptsyObjects.OrderCreateStatus internalCreateOrder(Integer marketId, int orderType, double quantity, double price) throws IOException, TradeApiError {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        arguments.add(new BasicNameValuePair("method", "createorder"));
        arguments.add(new BasicNameValuePair("marketid", Integer.toString(marketId)));
        arguments.add(new BasicNameValuePair("ordertype", orderType == Constants.ORDER_BUY ? "Buy" : "Sell"));
        arguments.add(new BasicNameValuePair("quantity", Utils.Strings.formatNumber(quantity)));
        arguments.add(new BasicNameValuePair("price", Utils.Strings.formatNumber(price)));
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<CryptsyObjects.OrderCreateStatus>() {
        }.getType());
    }

    private ApiStatus<String> internalCancelOrder(long orderId) throws Exception {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        arguments.add(new BasicNameValuePair("method", "cancelorder"));
        arguments.add(new BasicNameValuePair("orderid", Long.toString(orderId)));
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<String>>() {
        }.getType());
    }

    // Public
    private StandartObjects.CurrencyPairMapper cachedMapper = null;

    @Override
    public StandartObjects.CurrencyPairMapper getCurrencyPairs() throws IOException, TradeApiError {
        if (cachedMapper == null) {
            cachedMapper = new StandartObjects.CurrencyPairMapper();
            ApiStatus<List<CryptsyObjects.MarketDataPrivate>> markets = internalGetMarketDataPrivate();
            if (markets.success != 1) {
                throw new TradeApiError("Error retrieving market info (" + markets.error + ")");
            }
            if (markets.result != null) for (CryptsyObjects.MarketDataPrivate marketInfo : markets.result) {
                StandartObjects.CurrencyPair pair = new StandartObjects.CurrencyPair();
                pair.firstCurrency = marketInfo.primary_currency_code;
                pair.secondCurrency = marketInfo.secondary_currency_code;
                pair.pairName = marketInfo.label;
                pair.pairId = marketInfo.marketid;
                cachedMapper.put(marketInfo.marketid, pair);
            }
        }
        return cachedMapper;
    }


    private List<StandartObjects.MarketInfo> internalGetMarketsListPrivateUnified(Integer marketId, boolean retrieveOrders, boolean retrieveHistory) throws Exception {
        List<StandartObjects.MarketInfo> marketInfoList = new ArrayList<>();
        ApiStatus<List<CryptsyObjects.MarketDataPrivate>> generalInfo = internalGetMarketDataPrivate();

        if (generalInfo.success != 1) {
            throw new TradeApiError("Error retrieving market info (" + generalInfo.error + ")");
        } else {
            if (generalInfo.result != null)
                for (CryptsyObjects.MarketDataPrivate entry : generalInfo.result) {  // Conversion
                    if (marketId == null || marketId.equals(0) || marketId.equals(entry.marketid)) {
                        StandartObjects.MarketInfo marketInfo = new StandartObjects.MarketInfo();
                        marketInfo.pairName = entry.label;
                        marketInfo.pairId = entry.marketid;
                        marketInfo.price.high = entry.high_trade;
                        marketInfo.price.low = entry.low_trade;
                        marketInfo.price.buy = marketInfo.price.sell = marketInfo.price.last = entry.last_trade;
                        marketInfo.price.average = (entry.high_trade + entry.low_trade) / 2;
                        marketInfo.price.volume = entry.current_volume;

                        if (retrieveOrders && marketId != null && !marketId.equals(0)) {
                            marketInfo.depth = getMarketDepth(marketId);
                            // Top prices:
                            marketInfo.price.buy = marketInfo.depth.buyOrders.get(0).price;
                            marketInfo.price.sell = marketInfo.depth.sellOrders.get(0).price;
                        }
                        if (retrieveHistory && marketId != null && !marketId.equals(0)) {
                            marketInfo.history = getMarketHistory(marketId);
                        }
                        marketInfoList.add(marketInfo);
                    }
                }
        }
        return marketInfoList;
    }

    @Override
    public StandartObjects.MarketInfo getMarketData(Object pair, boolean retrieveOrders, boolean retrieveHistory) throws Exception {
        List<StandartObjects.MarketInfo> marketInfoList = internalGetMarketsListPrivateUnified((Integer) pair, retrieveOrders, retrieveHistory);
        if (marketInfoList != null && marketInfoList.size() > 0)
            return marketInfoList.get(0);
        else
            throw new UnknownError();
    }

    @Override
    public StandartObjects.Prices getMarketPrices(Object pair) throws Exception {
        return getMarketData(pair, false, false).price;
    }

    @Override
    public StandartObjects.Depth getMarketDepth(Object pair) throws Exception {
        StandartObjects.Depth depth = new StandartObjects.Depth();
        ApiStatus<CryptsyObjects.DepthInfo> ordersInfo = internalGetMarketDepth((Integer) pair);
        if (ordersInfo.success != 1) {
            throw new TradeApiError("Error retrieving orders info (" + ordersInfo.error + ")");
        }
        if (ordersInfo.result.buy != null) for (List<Double> depthEntry : ordersInfo.result.buy) {
            depth.buyOrders.add(new StandartObjects.Order(depthEntry.get(0), depthEntry.get(1)));
        }
        if (ordersInfo.result.sell != null) for (List<Double> depthEntry : ordersInfo.result.sell) {
            depth.sellOrders.add(new StandartObjects.Order(depthEntry.get(0), depthEntry.get(1)));
        }
        return depth;
    }

    @Override
    public List<StandartObjects.Order> getMarketHistory(Object pair) throws Exception {
        List<StandartObjects.Order> result = new ArrayList<>();
        ApiStatus<List<CryptsyObjects.Trade>> history = internalGetMarketHistory((Integer) pair);
        if (history.success != 1) {
            throw new TradeApiError("Error retrieving history info (" + history.error + ")");
        }
        if (history.result != null) for (CryptsyObjects.Trade trade : history.result) {
            StandartObjects.Order order = new StandartObjects.Order();
            order.amount = trade.quantity;
            order.id = trade.tradeid;
            order.pair = trade.marketid;
            order.price = trade.tradeprice;
            order.time = trade.datetime;
            order.type = trade.initiate_ordertype.equals("Sell") ? Constants.ORDER_SELL : Constants.ORDER_BUY;
            result.add(order);
        }
        return result;
    }

    @Override
    public StandartObjects.AccountInfo.AccountBalance getAccountBalances() throws Exception {
        ApiStatus<CryptsyObjects.AccountInfo> generalInfo = internalGetAccountInfo();
        if (generalInfo.success != 1) {
            throw new TradeApiError("Error retrieving account info (" + generalInfo.error + ")");
        } else {  // Conversion
            return new StandartObjects.AccountInfo.AccountBalance(generalInfo.result.balances_available);
        }
    }

    @Override
    public List<StandartObjects.Order> getAccountOpenOrders(Object pair) throws Exception {
        if (pair == null || pair.equals(0)) {
            throw new IllegalArgumentException("Invalid pair");
        }
        List<StandartObjects.Order> orders = new ArrayList<>();
        ApiStatus<List<CryptsyObjects.Order>> ordersInfo = internalGetMyOrders((Integer) pair);
        if (ordersInfo.success != 1) {
            throw new TradeApiError("Error retrieving orders info (" + ordersInfo.error + ")");
        }
        if (ordersInfo.result != null) for (CryptsyObjects.Order order : ordersInfo.result) {
            StandartObjects.Order uOrder = new StandartObjects.Order();
            uOrder.amount = order.quantity;
            uOrder.price = order.price;
            uOrder.pair = (Integer) order.marketid;
            uOrder.type = order.ordertype.equals("Sell") ? Constants.ORDER_SELL : Constants.ORDER_BUY;
            uOrder.id = order.orderid;
            uOrder.time = order.created;
            orders.add(uOrder);
        }
        return orders;
    }

    @Override
    public List<StandartObjects.Order> getAccountHistory(Object pair) throws Exception {
        if (pair == null || pair.equals(0)) {
            throw new IllegalArgumentException("Invalid pair");
        }
        List<StandartObjects.Order> history = new ArrayList<>();
        ApiStatus<List<CryptsyObjects.Trade>> historyApiStatus = internalGetAccountHistory((Integer) pair);
        if (historyApiStatus.success != 1) {
            throw new TradeApiError("Error retrieving account history (" + historyApiStatus.error + ")");
        }
        if (historyApiStatus.result != null) for (CryptsyObjects.Trade trade : historyApiStatus.result) {
            StandartObjects.Order order = new StandartObjects.Order();
            order.amount = trade.quantity;
            order.id = trade.tradeid;
            order.pair = trade.marketid;
            order.price = trade.tradeprice;
            order.time = trade.datetime;
            order.type = trade.tradetype.equals("Sell") ? Constants.ORDER_SELL : Constants.ORDER_BUY;
            history.add(order);
        }
        return history;
    }

    @Override
    public StandartObjects.AccountInfo getAccountInfo(Object pair, boolean retrieveOrders, boolean retrieveHistory) throws Exception {
        StandartObjects.AccountInfo accountInfo = new StandartObjects.AccountInfo();
        accountInfo.balance = getAccountBalances();
        if (retrieveOrders) {
            accountInfo.orders = getAccountOpenOrders(pair);
        }
        if (retrieveHistory) {
            accountInfo.history = getAccountHistory(pair);
        }
        return accountInfo;
    }

    @Override
    public double getFeePercent(Object pair) throws Exception {
        return 0.2;
    }

    @Override
    public long createOrder(Object pair, int orderType, double quantity, double price) throws IOException, TradeApiError {
        CryptsyObjects.OrderCreateStatus orderApiStatus = internalCreateOrder((Integer) pair, orderType, quantity, price);
        if (orderApiStatus.success != 1) {
            throw new TradeApiError("Failed to create order (" + orderApiStatus.error + ")");
        } else return orderApiStatus.orderid;
    }

    @Override
    public boolean cancelOrder(long orderId) throws Exception {
        ApiStatus<String> cancelApiStatus = internalCancelOrder(orderId);
        if (cancelApiStatus.success != 1) {
            throw new TradeApiError("Failed to cancel order (" + cancelApiStatus.error + ")");
        } else return true;
    }
}
