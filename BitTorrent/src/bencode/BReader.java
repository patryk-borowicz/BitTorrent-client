package bencode;

import java.io.IOException;
import java.io.OutputStream;
import bencode.types.BList;
import bencode.types.BMap;
import bencode.types.BNumber;
import bencode.types.BString;

import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import bencode.types.BStringComparator;

public class BReader {

    /**
     * @param encoded the string to decode
     * @return an array of Bencode elements, decoded from the @param encoded
     */
    public static BElement[] read(final byte[] encoded) {
        final AtomicInteger index = new AtomicInteger(0);
        final List<BElement> elements = new ArrayList<BElement>();
        while (index.get() != encoded.length) {
            elements.add(read(encoded, index));
        }
        return elements.toArray(new BElement[elements.size()]);
    }

    public static BElement read(final byte[] encoded, final AtomicInteger index) {
        switch (encoded[index.get()]) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return BString.read(encoded, index);
            case 'i':
                return BNumber.read(encoded, index);
            case 'l':
                return BList.read(encoded, index);
            case 'd':
                return BMap.read(encoded, index);
        }
        throw new RuntimeException("Failed to identify type{" + encoded[index.get()] + "}");
    }
    
    public static void bencode(BElement e, OutputStream out) throws IOException
        {
            if(e instanceof BString)
            {
                String str = ((BString) e).getValue();
                out.write(Integer.toString(str.length()).getBytes("UTF-8"));
                out.write(':');
                out.write(((BString) e).bytes/*getValue().getBytes("UTF-8")*/);
            }
            else if(e instanceof BNumber)
            {
                out.write('i');
                String s = Integer.toString(((BNumber) e).getValue());
		out.write(s.getBytes("UTF-8"));
                out.write('e');
            }
            else if(e instanceof BMap) //trzeba tu zrobic kolejnosc alfabetyczna??
            {
                BMap map = (BMap)e;
                out.write('d');
                /*for (Map.Entry<BString, BElement> entry : map.entrySet())
                {
                    bencode(entry.getKey(), out);
                    bencode(entry.getValue(), out);
                }*/
                Set<BString> s = map.keySet();
		List<BString> l = new ArrayList<BString>(s);
		Collections.sort(l, new BStringComparator());

		for (BString key : l) {
			BElement value = map.get(key);
			bencode(key, out);
			bencode(value, out);
		}
                out.write('e');
            }
            else if(e instanceof BList) //trzeba tu zrobic kolejnosc alfabetyczna??
            {
                BList list = (BList)e;
                out.write('l');
                for(BElement el : list)
                {
                    bencode(el, out);
                }
                out.write('e');
            }
        }
    
    public static void showAll(BElement e, String lvl)
    {
        if(e instanceof BString)
            {
                String str = ((BString) e).getValue();
                System.out.print(str);
            }
            else if(e instanceof BNumber)
            {
                String s = Integer.toString(((BNumber) e).getValue());
                System.out.print(s);
            }
            else if(e instanceof BMap) //trzeba tu zrobic kolejnosc alfabetyczna??
            {
                System.out.println();
                BMap map = (BMap)e;
                Set<BString> s = map.keySet();
		List<BString> l = new ArrayList<BString>(s);
		Collections.sort(l, new BStringComparator());
                lvl += "-";
		for (BString key : l) {
                    System.out.println(lvl);
			BElement value = map.get(key);
			showAll(key, lvl + "-");
                        System.out.print(" ");
			showAll(value, lvl + "-");
                        System.out.println();
		}
            }
            else if(e instanceof BList) //trzeba tu zrobic kolejnosc alfabetyczna??
            {
                System.out.println();
                BList list = (BList)e;
                for(BElement el : list)
                {
                    System.out.println(lvl);
                    showAll(el, lvl + "-");
                    System.out.println();
                }
            }
    }
}
