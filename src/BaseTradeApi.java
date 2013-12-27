public abstract class BaseTradeApi {
    public static final class Constants {
        int ORDER_BUY = 0;
        int ORDER_SELL = 1;
    }
    public class ApiKeyPair {
        String publicKey;
        String privateKey;
    }
    ApiKeyPair apiKeyPair;
    public abstract class Public {
        public abstract Object getMarketData(String pair);
        public abstract Object getOrders(String pair);
    }
    public abstract class Private { // Requires apiKeyPair
        public abstract Object getAccountInfo();
        public abstract Object getAccountHistory(String pair);
        public abstract Object getOpenOrders(String pair);
        public abstract Object getMarketInfo(String pair);
        public abstract Object calculateFees(int orderType, double quantity, double price);

        public abstract Object createOrder(String pair, int orderType, double quantity, double price);
        public abstract Object cancelOrder(String orderId);
    }
}
