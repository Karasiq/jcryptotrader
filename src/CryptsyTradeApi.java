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
            int tradeid;
            String tradetype; // Buy/Sell
            Date datetime;
            int marketid;
            double tradeprice;
            double quantity;
            double fee;
            double total;
            String initiate_ordertype; // Buy/Sell
            int order_id;
        }

        class Order extends TradeOrder {
            int orderid;
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
    }

    // Internal
    @Deprecated private ApiStatus<CryptsyObjects.Markets> internalGetMarketData(int marketId) throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        if(marketId == 0) {
            arguments.add(new BasicNameValuePair("method", "marketdatav2"));
        } else {
            arguments.add(new BasicNameValuePair("method", "singlemarketdata"));
            arguments.add(new BasicNameValuePair("marketid", Integer.toString(marketId)));
        }
        String json = executeRequest(false, PublicApiUrl, arguments, Constants.REQUEST_GET);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<CryptsyObjects.Markets>>() {}.getType());
    }
    @Deprecated private ApiStatus<CryptsyObjects.OrderBook> internalGetOrders(int marketId) throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        if(marketId == 0) {
            arguments.add(new BasicNameValuePair("method", "orderdata"));
        } else {
            arguments.add(new BasicNameValuePair("method", "singleorderdata"));
            arguments.add(new BasicNameValuePair("marketid", Integer.toString(marketId)));
        }
        String json = executeRequest(false, PublicApiUrl, arguments, Constants.REQUEST_GET);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<CryptsyObjects.OrderBook>>() {}.getType());
    }
    private ApiStatus<CryptsyObjects.AccountInfo> internalGetAccountInfo() throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        arguments.add(new BasicNameValuePair("method", "getinfo"));
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<CryptsyObjects.AccountInfo>>() {}.getType());
    }
    private ApiStatus<List<CryptsyObjects.MarketDataPrivate>> internalGetMarketDataPrivate() throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        arguments.add(new BasicNameValuePair("method", "getmarkets"));
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<ArrayList<CryptsyObjects.MarketDataPrivate>>>() {}.getType());
    }
    /* private ApiStatus<List<CryptsyObjects.Trade>> internalGetAccountHistory(int marketId) throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        if(marketId == 0) {
            arguments.add(new BasicNameValuePair("method", "allmytrades"));
        }
        else {
            arguments.add(new BasicNameValuePair("method", "mytrades"));
            arguments.add(new BasicNameValuePair("marketid", Integer.toString(marketId)));
        }
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<ArrayList<CryptsyObjects.Trade>>>() {
        }.getType());
    } */
    private ApiStatus<List<CryptsyObjects.Order>> internalGetMyOrders(int marketId) throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        if(marketId == 0) {
            arguments.add(new BasicNameValuePair("method", "allmyorders"));
        }
        else {
            arguments.add(new BasicNameValuePair("method", "myorders"));
            arguments.add(new BasicNameValuePair("marketid", Integer.toString(marketId)));
        }
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<ArrayList<CryptsyObjects.Order>>>() {}.getType());
    }
    private ApiStatus<CryptsyObjects.DepthInfo> internalGetMarketDepth(int marketId) throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        arguments.add(new BasicNameValuePair("method", "depth"));
        arguments.add(new BasicNameValuePair("marketid", Integer.toString(marketId)));
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<CryptsyObjects.DepthInfo>>() {}.getType());
    }
    private ApiStatus<CryptsyObjects.Fee> internalCalculateFees(int orderType, double quantity, double price) throws IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        arguments.add(new BasicNameValuePair("method", "calculatefees"));
        arguments.add(new BasicNameValuePair("ordertype", orderType == Constants.ORDER_BUY ? "Buy" : "Sell"));
        arguments.add(new BasicNameValuePair("quantity", Utils.Strings.formatNumber(quantity)));
        arguments.add(new BasicNameValuePair("price", Utils.Strings.formatNumber(price)));
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<CryptsyObjects.Fee>>() {}.getType());
    }

    private ApiStatus<CryptsyObjects.Order> internalCreateOrder(Integer marketId, int orderType, double quantity, double price) throws IOException, TradeApiError {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        arguments.add(new BasicNameValuePair("method", "createorder"));
        arguments.add(new BasicNameValuePair("marketid", Integer.toString(marketId)));
        arguments.add(new BasicNameValuePair("ordertype", orderType == Constants.ORDER_BUY ? "Buy" : "Sell"));
        arguments.add(new BasicNameValuePair("quantity", Utils.Strings.formatNumber(quantity)));
        arguments.add(new BasicNameValuePair("price", Utils.Strings.formatNumber(price)));
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<CryptsyObjects.Order>>() {}.getType());
    }
    private ApiStatus<String> internalCancelOrder(int orderId) throws TradeApiError, IOException {
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        arguments.add(new BasicNameValuePair("method", "createorder"));
        arguments.add(new BasicNameValuePair("orderid", Integer.toString(orderId)));
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        return jsonParser.fromJson(json, new TypeToken<ApiStatus<String>>() {}.getType());
    }

    /* private String internalGenerateNewAddress(int currencyId, String currencyCode) throws IOException, TradeApiError {
        class GeneratedAddress {
            String address;
        }
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        arguments.add(new BasicNameValuePair("method", "calculatefees"));
        arguments.add(new BasicNameValuePair("currencyid", Integer.toString(currencyId)));
        arguments.add(new BasicNameValuePair("currencycode", currencyCode));
        String json = executeRequest(true, PrivateApiUrl, arguments, Constants.REQUEST_POST);
        ApiStatus<GeneratedAddress> result = jsonParser.fromJson(json, new TypeToken<ApiStatus<GeneratedAddress>>() {}.getType());
        if(result.success == 1) {
            return result.result.address;
        } else {
            throw new TradeApiError("Error creating address (" + result.error + ")");
        }
    } */

    // Public
    public List<StandartObjects.MarketInfo> getMarketData(Object marketId, boolean retrieveOrders) throws TradeApiError, IOException {
        List<StandartObjects.MarketInfo> marketInfoList = new ArrayList<StandartObjects.MarketInfo>();
        ApiStatus<List<CryptsyObjects.MarketDataPrivate>> generalInfo = internalGetMarketDataPrivate();


        if(generalInfo.success != 1) {
            throw new TradeApiError("Error retrieving market info (" + generalInfo.error + ")");
        } else {
            for(CryptsyObjects.MarketDataPrivate entry : generalInfo.result) {  // Conversion
                if(marketId == null || marketId.equals(0) || marketId.equals(entry.marketid)) {
                    StandartObjects.MarketInfo marketInfo = new StandartObjects.MarketInfo();
                    marketInfo.pairName = entry.label;
                    marketInfo.pairId = entry.marketid;
                    marketInfo.price.high = entry.high_trade;
                    marketInfo.price.low = entry.low_trade;
                    marketInfo.price.buy = marketInfo.price.sell = marketInfo.price.last = entry.last_trade;
                    marketInfo.price.average = (entry.high_trade + entry.low_trade) / 2;

                    if(retrieveOrders) {
                        ApiStatus<CryptsyObjects.DepthInfo> ordersInfo = internalGetMarketDepth(entry.marketid);
                        if(ordersInfo.success != 1) {
                            throw new TradeApiError("Error retrieving orders info (" + ordersInfo.error + ")");
                        }
                        for(List<Double> depthEntry : ordersInfo.result.buy) {
                            marketInfo.depth.buyOrders.add(new StandartObjects.Order(depthEntry.get(0), depthEntry.get(1)));
                        }
                        for(List<Double> depthEntry : ordersInfo.result.sell) {
                            marketInfo.depth.sellOrders.add(new StandartObjects.Order(depthEntry.get(0), depthEntry.get(1)));
                        }
                    }
                    marketInfoList.add(marketInfo);
                }
            }
        }
        return marketInfoList;
    }
    public StandartObjects.AccountInfo getAccountInfo(boolean retrieveOrders) throws TradeApiError, IOException {
        StandartObjects.AccountInfo accountInfo = new StandartObjects.AccountInfo();
        ApiStatus<CryptsyObjects.AccountInfo> generalInfo = internalGetAccountInfo();

        if(generalInfo.success != 1) {
            throw new TradeApiError("Error retrieving account info (" + generalInfo.error + ")");
        } else {  // Conversion
            accountInfo.balance = generalInfo.result.balances_available;
            if(retrieveOrders) {
                ApiStatus<List<CryptsyObjects.Order>> ordersInfo = internalGetMyOrders(0);
                if(ordersInfo.success != 1) {
                    throw new TradeApiError("Error retrieving orders info (" + ordersInfo.error + ")");
                }
                for(CryptsyObjects.Order order : ordersInfo.result) {
                    StandartObjects.Order uOrder = new StandartObjects.Order();
                    uOrder.amount = order.quantity;
                    uOrder.price = order.price;
                    uOrder.pair = (Integer)order.marketid;
                    uOrder.type = order.ordertype.equals("Sell") ? Constants.ORDER_SELL : Constants.ORDER_BUY;
                    uOrder.id = order.orderid;
                    uOrder.time = order.created;
                    accountInfo.orders.add(uOrder);
                }
            }
        }
        return accountInfo;
    }
    public double calculateFees(int orderType, double quantity, double price) throws TradeApiError, IOException {
        ApiStatus<CryptsyObjects.Fee> feeApiStatus = internalCalculateFees(orderType, quantity, price);
        if(feeApiStatus.success != 1) {
            throw new TradeApiError("Error calculating fee (" + feeApiStatus.error + ")");
        }
        else return feeApiStatus.result.fee;
    }

    public int createOrder(Object pair, int orderType, double quantity, double price) throws IOException, TradeApiError {
        ApiStatus<CryptsyObjects.Order> orderApiStatus = internalCreateOrder((Integer)pair, orderType, quantity, price);
        if(orderApiStatus.success != 1) {
            throw new TradeApiError("Error creating order (" + orderApiStatus.error + ")");
        }
        else return orderApiStatus.result.orderid;
    }
    public boolean cancelOrder(int orderId) throws TradeApiError, IOException {
        ApiStatus<String> cancelApiStatus = internalCancelOrder(orderId);
        if(cancelApiStatus.success != 1) {
            throw new TradeApiError("Error cancelling order (" + cancelApiStatus.error + ")");
        }
        else return true;
    }
}
