/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bencode.types;

import java.util.Comparator;

public class BStringComparator implements Comparator<BString>{
    @Override
    public int compare(BString t, BString t1) {
        return t.getValue().compareTo(t1.getValue());
    }
}
