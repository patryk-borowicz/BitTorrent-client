/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.util.ArrayList;
import java.util.Map;
import bencode.BElement;
import bencode.types.BList;
import bencode.types.BMap;
import bencode.types.BNumber;
import bencode.types.BString;
import java.util.List;

public class TorrentItem {

    private String name;
    private long length;
    private long offset;

    public static ArrayList<TorrentItem> getFiles(BMap infoMap) {
        ArrayList<TorrentItem> files = new ArrayList<TorrentItem>();
        //pojedynczy plik
        BList list = (BList) getBElement(infoMap, "files");
        if (list == null) {
            TorrentItem file = new TorrentItem();
            file.name = ((BString) getBElement(infoMap, "name")).getValue();
            file.length = ((BNumber) getBElement(infoMap, "length")).getValue();
            file.offset = 0;
            files.add(file);
        } else {
            long off = 0;
            for (BElement e : list) {
                
                if (e instanceof BMap) {
                    //FIXME: co z pustym folderem?
                    BMap map = (BMap) e;
                    TorrentItem file = new TorrentItem();
                    file.length = ((BNumber) getBElement(map, "length")).getValue();
                    file.offset = off;
                    //System.out.println("len "+file.length);
                    off += file.length;
                    BList l = (BList) getBElement(map, "path");

                    file.name = "";
                    for (BElement s : l) {
                        file.name += "/" + ((BString) s).getValue();
                    }
                    file.name = file.name.substring(1);

                    files.add(file);
                }
            }
        }
        return files;
    }

    public static BElement getBElement(BMap map, String key) {
        for (Map.Entry<BString, BElement> entry : map.entrySet()) {
            if (entry.getKey().getValue().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
    
    public long getLength()
    {
        return length;
    }
    
    public Object[] toRow(List<Piece> pieces, int piecesCount, int pieceLen, long torrentSize)
    {
        byte[] data = Piece.toBitfield(pieces, piecesCount);
        byte[] bitfield = new byte[piecesCount];
        for(int i = 0; i<piecesCount; i++)
        {
            bitfield[i] = (byte)((data[i/8] >> (7 - (i % 8))) & 1);
        }
        int completed = 0;
        int all = 0;
        int i = (int)(offset / pieceLen);
        long off = i * pieceLen;
        for(; i<bitfield.length && off < torrentSize; i++, off+=pieceLen)
        {
            if(off < offset + length)
            {
                all++;
                completed += bitfield[i];
            }
            else
            {
                break;
            }
        }
        
        String p = (completed * 100 / all) + "%";
        String pp = completed + "/" + all;
        return new Object[] { name, Client.bytesFormat(length), p, pp };
    }
}
