import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CryptsyApiObjects {
    class ApiStatus<ReturnType> implements Serializable {
        boolean success;
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
    class MarketData {
        int marketid;
        String label;
        double lasttradeprice;
        double volume;
        Date lasttradetime;
        String primaryname;
        String primarycode;
        String secondaryname;
        String secondarycode;
        List<TradeHistory> recenttrades;
        List<TradeOrder> sellorders;
        List<TradeOrder> buyorders;
    }
    class MarketsData {
        Map<String, MarketData> markets;
    }
}
