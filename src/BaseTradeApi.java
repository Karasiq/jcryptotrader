import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;


import javax.lang.model.element.Name;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseTradeApi {
    public static final class Constants {
        static final int ORDER_BUY = 0;
        static final int ORDER_SELL = 1;
        static final int REQUEST_GET = 0;
        static final int REQUEST_POST = 1;
    }
    public class RequestSender {
        String requestEncoding = "UTF-8";
        List<NameValuePair> httpHeaders = new ArrayList<NameValuePair>();

        String formatGetParamString(List<NameValuePair> urlParameters) {
            String url = "";
            boolean firstParam = true;
            for(NameValuePair entry : urlParameters) { // Adding fields
                if(!firstParam) url = url + "&";
                else firstParam = false;
                try {
                    url = url + entry.getName() + "=" + URLEncoder.encode(entry.getValue(), requestEncoding);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            return url;
        }

        public HttpResponse getRequest(String url, List<NameValuePair> urlParameters) throws IOException {
            url = url + "?" + formatGetParamString(urlParameters);

            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(url);
            for(NameValuePair header : httpHeaders) { // Adding headers
                request.addHeader(header.getName(), header.getValue());
            }

            return client.execute(request);
        }
        public HttpResponse postRequest(String url, List<NameValuePair> urlParameters) throws IOException {
            url = url + "?";
            HttpClient client = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(url);
            request.setEntity(new UrlEncodedFormEntity(urlParameters));

            for(NameValuePair header : httpHeaders) { // Adding headers
                request.addHeader(header.getName(), header.getValue());
            }

            return client.execute(request);
        }
        public String getResponseString(HttpResponse response) throws IOException {
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            StringBuilder result = new StringBuilder();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        }
    }
    public class ApiKeyPair implements Serializable {
        String publicKey;
        String privateKey;
    }

    ApiKeyPair apiKeyPair;
    RequestSender requestSender;

    public BaseTradeApi() {
        apiKeyPair = new ApiKeyPair();
        requestSender = new RequestSender();
    }
    abstract void cleanAuth(List<NameValuePair> urlParameters, List<NameValuePair> httpHeaders);
    abstract void writeAuthParams(List<NameValuePair> urlParameters, List<NameValuePair> httpHeaders);
    private String executeRequest(boolean needAuth, String url, List<NameValuePair> urlParameters, int httpRequestType) throws IOException {
        cleanAuth(urlParameters, requestSender.httpHeaders);
        if(needAuth) writeAuthParams(urlParameters, requestSender.httpHeaders);
        switch (httpRequestType) {
            case Constants.REQUEST_GET:
                return requestSender.getResponseString(requestSender.getRequest(url, urlParameters));
            case Constants.REQUEST_POST:
                return requestSender.getResponseString(requestSender.postRequest(url, urlParameters));
            default:
                throw new IllegalArgumentException("Unknown httpRequestType value");
        }
    }

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
