import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Utils {
    public static class Crypto {
        public static class Hashing {
            public static final String HMAC_MD5 = "HmacMD5";
            public static final String HMAC_SHA1 = "HmacSHA1";
            public static final String HMAC_SHA256 = "HmacSHA256";
            public static final String HMAC_SHA384 = "HmacSHA384";
            public static final String HMAC_SHA512 = "HmacSHA512";
            public static String hmacDigest(String msg, String keyString, String algo) throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
                SecretKeySpec key = new SecretKeySpec((keyString).getBytes("UTF-8"), algo);
                Mac mac = Mac.getInstance(algo);
                mac.init(key);

                byte[] bytes = mac.doFinal(msg.getBytes("ASCII"));

                StringBuilder hash = new StringBuilder();
                for (byte b : bytes) {
                    String hex = Integer.toHexString(0xFF & b);
                    if (hex.length() == 1) {
                        hash.append('0');
                    }
                    hash.append(hex);
                }
                return hash.toString();
            }
        }
    }
    public static class Strings {
        public static <T> String formatNumber(T value) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
            symbols.setDecimalSeparator('.');
            symbols.setGroupingSeparator(',');
            DecimalFormat df = new DecimalFormat("#################.########", symbols);
            return df.format(value);
        }
    }
}
