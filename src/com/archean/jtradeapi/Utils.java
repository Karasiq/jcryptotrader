package com.archean.jtradeapi;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Utils {
    public static class Threads {
        public static abstract class CycledRunnable implements Runnable {
            public static final int STOP_CYCLE = -1;
            abstract protected int cycle() throws Exception;
            protected int onError(Exception e) {
                e.printStackTrace();
                return 0;
            }
            @Override public void run() {
                int sleepTime = 0;
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        if(sleepTime == STOP_CYCLE) break;
                        else if(sleepTime != 0) Thread.sleep(sleepTime);
                        sleepTime = cycle();
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        sleepTime = onError(e);
                    }
                }
            }
        }
    }
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
        public static class DecimalFormatDescription {
            public DecimalFormat toDecimalFormat() {
                DecimalFormatSymbols symbols = new DecimalFormatSymbols();
                symbols.setDecimalSeparator(decimalSeparator);
                symbols.setGroupingSeparator(groupingSeparator);
                return new DecimalFormat(stringFormat, symbols);
            }
            public char decimalSeparator;
            public char groupingSeparator;
            public String stringFormat;
            public DecimalFormatDescription(String stringFormat, char decimalSeparator, char groupingSeparator) {
                this.decimalSeparator = decimalSeparator;
                this.groupingSeparator = groupingSeparator;
                this.stringFormat = stringFormat;
            }
        }
        // Constants:
        public final static DecimalFormatDescription percentDecimalFormat = new DecimalFormatDescription("######.##", '.', ',');
        public final static DecimalFormatDescription moneyFormat = new DecimalFormatDescription("############.########", '.', ','); // precision = 1 satoshi
        public final static DecimalFormatDescription moneyRepresentFormat = new DecimalFormatDescription("###,###,###,###.########", '.', ','); // with groupings
        public final static DecimalFormatDescription moneyRoughRepresentFormat = new DecimalFormatDescription("###,###,###,###.###", '.', ','); // not precise

        private static Map<DecimalFormatDescription, DecimalFormat> decimalFormatMap = new HashMap<>(); // cached
        public static <T> String formatNumber(T value, DecimalFormatDescription format) { // custom format
            DecimalFormat df = decimalFormatMap.get(format);
            if(df == null) {
                df = format.toDecimalFormat();
                decimalFormatMap.put(format, df);
            }
            return df.format(value);
        }

        public static <T> String formatNumber(T value) { // json format
            return formatNumber(value, moneyFormat);
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
