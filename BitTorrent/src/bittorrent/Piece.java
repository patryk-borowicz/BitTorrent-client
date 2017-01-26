/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Piece implements Comparable{
    private int index;
    private int pieceLength;
    private long byteOffset;
    private boolean completed = false;
    private byte[] hash;
    private HashSet<Peer> seeders = new HashSet<Peer>();
    private boolean requested = false;
    
    public Piece(int index, int pieceLength, byte[] hash, Peer seeder)
    {
        this.index = index;
        this.pieceLength = pieceLength;
        byteOffset = pieceLength * index;
        this.hash = hash;
        seeders.add(seeder);
    }
    
    public HashSet<Peer> getSeeders()
    {
        return seeders;
    }
    
    public int available()
    {
        return seeders.size();
    }
    
    public boolean isCompleted()
    {
        return completed;
    }
    
    public int getIndex()
    {
        return index;
    }
    
    public void setCompleted()
    {
        completed = true;
    }
    
    public boolean checkData(byte[] data)
    {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(data);
            return java.util.Arrays.equals(sha1.digest(), hash);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Piece.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    /*public boolean checkData(SortedMap<Integer, byte[]> blocksData) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                while (blocksData.size() > 0) {
                                    int key = blocksData.firstKey();
                                    baos.write(blocksData.remove(key));
                                }
        return checkData(baos.toByteArray());
    }*/
    
    @Override
    public boolean equals(Object o)
    {
        Piece p = (Piece)o;
        return index == p.index;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + this.index;
        hash = 17 * hash + Arrays.hashCode(this.hash);
        return hash;
    }
    
    public static byte[] toBitfield(List<Piece> pieces, int len)
    {
        byte[] bitfield = new byte[(int)Math.ceil((double)len/8)];
        for(Piece p : pieces)
        {
            if(p.completed)
            {
                int byteIndex = p.index/8;
                int shift = 7 - (p.index % 8);
                bitfield[byteIndex] |= (1 << shift);
            }
        }
        return bitfield;
    }
    
    /*public int getPieceLength()
    {
        return pieceLength;
    }*/
    
    public void setRequested(boolean value)
    {
        requested = value;
    }
    
    public boolean isRequested()
    {
        return requested;
    }
    
    //tymczasowa metoda
    public void keepData(byte[] data)
    {
        this.data = data;
    }
    byte[] data;
    public byte[] getData()
    {
        return data;
    }

    @Override
    public int compareTo(Object o1) {
        if (this.index == ((Piece) o1).index)
            return 0;
        else if ((this.index) > ((Piece) o1).index)
            return 1;
        else
            return -1;
    }
}
