/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PeerRequest {
    public static byte[] create(PeerResponse.MessageType type, Object... args) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        switch(type)
        {
            case KEEP_ALIVE:
                out.write(toByteArray(0));
                break;
            case CHOKE:
                out.write(toByteArray(1));
                out.write(PeerResponse.CHOKE_MESSAGE);
                break;
            case UNCHOKE:
                out.write(toByteArray(1));
                out.write(PeerResponse.UNCHOKE_MESSAGE);
                break;
            case INTERESTED:
                out.write(toByteArray(1));
                out.write(PeerResponse.INTERESTED_MESSAGE);
                break;
            case NOTINTERESTED:
                out.write(toByteArray(1));
                out.write(PeerResponse.NOTINTERESTED_MESSAGE);
                break;
            case HAVE:
                out.write(toByteArray(5));
                out.write(PeerResponse.HAVE_MESSAGE);
                out.write((int)args[0]); //piece index
                break;
            case BITFIELD:
                ByteArrayOutputStream bitfield = (ByteArrayOutputStream)args[0];
                out.write(toByteArray(1+ bitfield.size()));
                out.write(PeerResponse.BITFIELD_MESSAGE);
                out.write(bitfield.toByteArray());
                /*System.out.println("Butfield len: "+bitfield.size());
                for(byte b : bitfield.toByteArray())
                {
                    System.out.print(Byte.toUnsignedInt(b)+ " ");
                }
                System.out.println();*/
                break;
            case REQUEST:
                out.write(new byte[] {0, 0, 0, 13});
                
                out.write((byte)PeerResponse.REQUEST_MESSAGE);
                
                out.write(toByteArray((int)args[0]));//index
                out.write(toByteArray((int)args[1]));//begin
                out.write(toByteArray((int)args[2]));//length
                break;
            case PIECE:
                out.write(toByteArray(9 + ((ByteArrayOutputStream)args[2]).size()));
                out.write(PeerResponse.PIECE_MESSAGE);
                out.write((int)args[0]);//index
                out.write((int)args[1]);//begin
                ByteArrayOutputStream b = (ByteArrayOutputStream)args[2];
                out.write(b.toByteArray());//block
                break;
            case CANCEL:
                out.write(toByteArray(13));
                out.write(PeerResponse.CANCEL_MESSAGE);
                out.write((int)args[0]);//index
                out.write((int)args[1]);//begin
                out.write((int)args[2]);//length
                break;
        }
        return out.toByteArray();
    }
    
    private static byte[] toByteArray(int val)
    {
        return new byte[] { (byte)((val >> 24) & 0xFF), (byte)((val >> 16) & 0xFF), 
            (byte)((val >> 8) & 0xFF), (byte)(val & 0xFF)};
        
    }
}
