import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.sun.org.apache.bcel.internal.generic.RET;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class CryptsyTradeApi extends BaseTradeApi {
    private static final String PublicApiUrl = "http://pubapi.cryptsy.com/api.php";
    private static final String JsonDateFormat = "yyyy-MM-dd HH:mm:ss";
    private int nonce = 0;
    private String makeSign(List<NameValuePair> urlParameters) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        return Utils.Crypto.Hashing.hmacDigest(requestSender.formatGetParamString(urlParameters), apiKeyPair.privateKey, Utils.Crypto.Hashing.HMAC_SHA512);
    }
    void cleanAuth(List<NameValuePair> urlParameters, List<NameValuePair> httpHeaders) {
        Iterator<NameValuePair> headerIterator = httpHeaders.iterator();
        while(headerIterator.hasNext()) { // Cleaning
            NameValuePair header = headerIterator.next();
            if(header.getName().equals("Key") || header.getName().equals("Sign")) {
                headerIterator.remove();
            }
        }
        Iterator<NameValuePair> paramsIterator = urlParameters.iterator();
        while(paramsIterator.hasNext()) { // Cleaning
            NameValuePair header = paramsIterator.next();
            if(header.getName().equals("nonce")) {
                paramsIterator.remove();
            }
        }
    }
    private void addNonce(List<NameValuePair> urlParameters) {
        this.nonce++;
        urlParameters.add(new BasicNameValuePair("nonce", Integer.toString(this.nonce)));
    }
    void writeAuthParams(List<NameValuePair> urlParameters, List<NameValuePair> httpHeaders) {
        addNonce(urlParameters);
        try {
            httpHeaders.add(new BasicNameValuePair("Sign", makeSign(urlParameters)));
            httpHeaders.add(new BasicNameValuePair("Key", apiKeyPair.publicKey));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class ApiStatus<ReturnType> {
        int success;
        @SerializedName("return")
        ReturnType result;
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

    static Gson makeJsonParser() {
        return new GsonBuilder().setDateFormat(JsonDateFormat).create();
    }

    public static int getMarketIdByPairName(Markets marketsData, String pair) {
        for(Map.Entry<String, MarketData> entry : marketsData.markets.entrySet()) {
            if(entry.getKey().equals(pair)) {
                return entry.getValue().marketid;
            }
        }
        throw new IllegalArgumentException("Pair " + pair + " not found on market");
    }

    public ApiStatus<Markets> getMarketData(String pair) throws IOException { // Pair: market id
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        if(pair == null || pair.equals("")) {
            arguments.add(new BasicNameValuePair("method", "marketdatav2"));
        } else {
            arguments.add(new BasicNameValuePair("method", "singlemarketdata"));
            arguments.add(new BasicNameValuePair("marketid", pair));
        }
        String json = executeRequest(false, PublicApiUrl, arguments, Constants.REQUEST_GET);
        return makeJsonParser().fromJson(json, new TypeToken<ApiStatus<Markets>>() {
        }.getType());
    }

    public ApiStatus<Markets> getMarketData(int marketId) throws IOException {
        return getMarketData(Integer.toString(marketId));
    }

    public Object getOrders(String pair) throws IOException { // TODO: test
        List<NameValuePair> arguments = new ArrayList<NameValuePair>();
        if(pair == null || pair.equals("")) {
            arguments.add(new BasicNameValuePair("method", "orderdata"));
        } else {
            arguments.add(new BasicNameValuePair("method", "singleorderdata"));
            arguments.add(new BasicNameValuePair("marketid", pair));
        }
        String json = executeRequest(false, PublicApiUrl, arguments, Constants.REQUEST_GET);
        return makeJsonParser().fromJson(json, new TypeToken<ApiStatus<OrderBook>>() {
        }.getType());
    }

    public Object getOrders(int marketId) throws IOException {
        return getOrders(Integer.toString(marketId));
    }

    // Private
    public Object getAccountInfo() { // TODO: implement private api
        return null;
    }
    public Object getAccountHistory(String pair) {
        return null;
    }
    public Object getOpenOrders(String pair) {
        return null;
    }
    public Object getMarketInfo(String pair) {
        return null;
    }
    public Object calculateFees(int orderType, double quantity, double price) {
        return null;
    }

    public Object createOrder(String pair, int orderType, double quantity, double price) {
        return null;
    }
    public Object cancelOrder(String orderId) {
        return null;
    }
}
