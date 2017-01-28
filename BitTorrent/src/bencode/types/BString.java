package bencode.types;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import bencode.BElement;

import java.util.concurrent.atomic.AtomicInteger;

public class BString implements BElement {

    public String value;
    public byte[] bytes;

    public BString(final String value, final byte[] bytes) {
        this.value = value;
        this.bytes = bytes;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String encode() {
        return value.length() + ":" + value;
    }

    /**
     * 
     * @param index the index to read from
     */
    public static BString read(final byte[] encoded2, AtomicInteger index) {
        String encoded = new String(encoded2, StandardCharsets.ISO_8859_1);
        final int colonIndex = encoded.indexOf(':', index.get());
        //final int length = Character.getNumericValue(encoded.charAt(index.get()));
        final int length = Integer.parseInt(encoded.substring(index.get(), colonIndex));
        index.set(colonIndex + 1);
        int indexTmp = index.get();
        final String value = encoded.substring(index.get(), index.get() + length);
        index.set(index.get() + length);
        //System.out.println(value);
        return new BString(value, Arrays.copyOfRange(encoded2, indexTmp, indexTmp + length));
    }
   
}
