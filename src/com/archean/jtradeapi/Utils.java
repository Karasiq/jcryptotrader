package com.archean.jtradeapi;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Utils {
    public static class Crypto {
        public static class Hashing {
            public static final String MD5 = "MD5";
            public static final String SHA1 = "SHA1";
            public static final String SHA256 = "SHA256";
            public static final String SHA384 = "SHA384";
            public static final String SHA512 = "SHA512";

            public static String hmacDigest(String msg, String keyString, String algo) throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
                algo = "Hmac" + algo;
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
        public static <T> String formatNumber(T value, String format) { // custom format
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
            symbols.setDecimalSeparator('.');
            symbols.setGroupingSeparator(',');
            DecimalFormat df = new DecimalFormat(format, symbols);
            return df.format(value);
        }

        public static <T> String formatNumber(T value) { // json format
            return formatNumber(value, "#################.########");
        }
    }

    public static class Serialization {
        public static void serializeObject(Object data, OutputStream outputStream) throws IOException {
            ObjectOutput output = new ObjectOutputStream(new BufferedOutputStream(outputStream));
            output.writeObject(data);
        }

        public static Object deSerializeObject(InputStream inputStream) throws IOException, ClassNotFoundException {
            ObjectInput input = new ObjectInputStream(new BufferedInputStream(inputStream));
            return input.readObject();
        }

    }
}
