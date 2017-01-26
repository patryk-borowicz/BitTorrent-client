package bencode.types;

import bencode.BElement;
import bencode.BReader;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class BList extends ArrayList<BElement> implements BElement {

    @Override
    public String encode() {
        final StringBuilder builder = new StringBuilder();
        builder.append('l');
        for (final BElement element : this) {
            builder.append(element.encode());
        }
        return builder.append('e').toString();
    }

    /**
     * @param encoded the string we are decoding
     * @param index the index to read from
     */
    public static BList read(final byte[] encoded, final AtomicInteger index) {
        if (encoded[index.get()] == 'l') index.set(index.get() + 1);
        final BList list = new BList();
        while (encoded[index.get()] != 'e') {
            list.add(BReader.read(encoded, index));
        }
        index.set(index.get() + 1);
        return list;
    }

}
