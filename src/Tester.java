import java.io.IOException;

public class Tester {
    public static void main(String[] args) {

        CryptsyTradeApi cryptsyTradeApi = new CryptsyTradeApi();
        try {
            CryptsyTradeApi.ApiStatus<CryptsyTradeApi.Markets> result = cryptsyTradeApi.getMarketData("");
            if(result.success == 1) {
                int centLtcMarket = CryptsyTradeApi.getMarketIdByPairName(result.result, "CENT/LTC");
                CryptsyTradeApi.ApiStatus<CryptsyTradeApi.Markets> centLtcInfo = cryptsyTradeApi.getMarketData(centLtcMarket);
                if(centLtcInfo.success == 1) {
                    System.out.print(Utils.Strings.formatNumber(centLtcInfo.result.markets.firstEntry().getValue().lasttradeprice));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
