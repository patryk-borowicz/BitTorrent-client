/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PeerResponse {

    public final static int HASH_BYTES = 20;
    public final static int ID_BYTES = 20;
    public final static int INTEGER_BYTES = 4;
    public final static int SKIP_BYTES = 8;

    public enum MessageType {
        KEEP_ALIVE, CHOKE, UNCHOKE, INTERESTED, NOTINTERESTED, HAVE, BITFIELD,
        PIECE, REQUEST, CANCEL, FAIL, SHORT_LENGTH, PORT
    };

    public final static int CHOKE_MESSAGE = 0x0;
    public final static int UNCHOKE_MESSAGE = 0x1;
    public final static int INTERESTED_MESSAGE = 0x2;
    public final static int NOTINTERESTED_MESSAGE = 0x3;
    public final static int HAVE_MESSAGE = 0x4;
    public final static int BITFIELD_MESSAGE = 0x5;
    public final static int REQUEST_MESSAGE = 0x6;
    public final static int PIECE_MESSAGE = 0x7;
    public final static int CANCEL_MESSAGE = 8;
    public final static int PORT_MESSAGE = 9;

    private int len = 0;
    private int index = 0;
    private byte[] block;
    private int begin = 0;
    private int length = 0;
    
    public int getLength()
    {
        return length;
    }
    public byte[] getBlock()
    {
        return block;
    }
    
    public int getBegin()
    {
        return begin;
    }
    
    public int getIndex()
    {
        return index;
    }

    MessageType result;

    public static PeerResponse parse(ByteBuffer b) {
        ByteArrayInputStream in = new ByteArrayInputStream(b.array(), 0, b.position());
        PeerResponse pr = new PeerResponse();
        if (in.available() < 4) {
            pr.result = MessageType.SHORT_LENGTH;
            return pr;
        }

        pr.len = readInteger(in);
        //System.out.println("Length: " + pr.len);
        if (pr.len == 0) {
            pr.result = MessageType.KEEP_ALIVE;
            System.out.println("-alive");
            return pr;
        }
        /*The bitfield message may only be sent immediately after 
            the handshaking sequence is completed, and before any other messages 
            are sent. It is optional, and need not be sent if a client has 
            no pieces. - wazne*/
        if (in.available() >= pr.len) {
            int rsp = in.read();
            switch (rsp) {
                case CHOKE_MESSAGE: //len 1
                    System.out.println("choke!");
                    pr.result = MessageType.CHOKE;
                    break;
                case UNCHOKE_MESSAGE: //len 1
                   // System.out.println("unchoke!");
                    pr.result = MessageType.UNCHOKE;
                    break;
                case INTERESTED_MESSAGE: //len 1
                    System.out.println("interested!");
                    pr.result = MessageType.INTERESTED;
                    break;
                case NOTINTERESTED_MESSAGE: //len 1
                    System.out.println("not interested!");
                    pr.result = MessageType.NOTINTERESTED;
                    break;
                case HAVE_MESSAGE:
                    pr.result = MessageType.HAVE;
                    pr.index = readInteger(in);
                    break;
                case BITFIELD_MESSAGE:
                    pr.result = MessageType.BITFIELD;
                    pr.bitfield = getBytesFromStream(in, pr.len - 1);
                    break;
                case REQUEST_MESSAGE:
                    System.out.println("---------request!");
                    pr.result = MessageType.REQUEST;
                    pr.index = readInteger(in);
                    pr.begin = readInteger(in);
                    pr.length = readInteger(in);
                    break;
                case PIECE_MESSAGE:
                    pr.result = MessageType.PIECE;
                    pr.index = readInteger(in);
                    pr.begin = readInteger(in);
                    pr.block = new byte[pr.len - 9];
                    in.read(pr.block, 0, pr.len - 9);
                    break;
                case CANCEL_MESSAGE:
                    System.out.println("cancel!");
                    pr.result = MessageType.REQUEST;
                    pr.index = readInteger(in);
                    pr.begin = readInteger(in);
                    pr.length = readInteger(in);
                    break;
                case PORT_MESSAGE:
                    System.out.println("port!");
                    pr.result = MessageType.PORT;
                    in.skip(2);
                    break;
                default:
                    System.err.println("response fail");
                    pr.result = MessageType.FAIL;
                    break;
            }
        } else {
            pr.result = MessageType.SHORT_LENGTH;
        }
        return pr;

    }

    public static byte[] getBytesFromStream(ByteArrayInputStream in, int count) {
        byte[] tmp = new byte[count];
        in.read(tmp, 0, count);
        return tmp;
    }

    public MessageType getResult() {
        return result;
    }

    public static int readInteger(ByteArrayInputStream in) {
        ByteBuffer bb = ByteBuffer.wrap(getBytesFromStream(in, INTEGER_BYTES));
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getInt();
    }
    
    public int allMessageBytes()
    {
        return len + 4;
    }
    
    byte[] bitfield;
    public byte[] getBitfield()
    {
        return bitfield;
    }
}
