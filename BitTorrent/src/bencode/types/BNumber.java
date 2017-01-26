package bencode.types;

import java.nio.charset.StandardCharsets;
import bencode.BElement;

import java.util.concurrent.atomic.AtomicInteger;

public class BNumber implements BElement {

    public int value;

    public BNumber(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(final int value) {
        this.value = value;
    }

    @Override
    public String encode() {
        return "i" + value + "e";
    }

    public static BNumber read(final byte[] encoded2, final AtomicInteger index) {
        String encoded = new String(encoded2, StandardCharsets.ISO_8859_1);
        if (encoded.charAt(index.get()) == 'i') index.set(index.get() + 1);
        final int end = encoded.indexOf('e', index.get());
        final int value = Integer.valueOf(encoded.substring(index.get(), end));
        index.set(end + 1);
        return new BNumber(value);
    }
}
