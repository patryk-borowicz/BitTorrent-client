package bencode.types;

import bencode.BElement;
import bencode.BReader;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BMap extends HashMap<BString, BElement> implements BElement {

    @Override
    public String encode() {
        final StringBuilder builder = new StringBuilder();
        builder.append('d');
        for (final Map.Entry<BString, BElement> entry : entrySet()) {
            builder.append(entry.getKey().encode() + entry.getValue().encode());
        }
        return builder.append('e').toString();
    }

    /**
     * @param encoded the string we are decoding
     * @param index the index to read from
     */
    public static BMap read(final byte[] encoded, final AtomicInteger index) {
        if (encoded[index.get()] == 'd') index.set(index.get() + 1);
        final BMap map = new BMap();
        while (encoded[index.get()] != 'e') {
            final BString key = BString.read(encoded, index);
            final BElement value = BReader.read(encoded, index);
            map.put(key, value);
        }
        index.set(index.get() + 1);
        return map;
    }

}
