import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;

public class CryptsyTradeApi extends BaseTradeApi { // TODO: implement cryptsy
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
    public class Public {
        public MarketsData getMarketData(String pair) {

        }
    }
    public class Private {

    }
}
