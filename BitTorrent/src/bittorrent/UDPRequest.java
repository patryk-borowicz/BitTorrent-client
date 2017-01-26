/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.nio.ByteBuffer;

public class UDPRequest {

    public static final int UDP_ANNOUNCE_REQUEST_MESSAGE_SIZE = 98;
    public static final int UDP_CONNECTION_REQUEST_MESSAGE_SIZE = 16;
    public static final int ANNOUNCE_ID = 1;
    public static final long CONNECTION_ID_MAGIC_NUMBER = 0x41727101980L;
    /*
    eventy:
        none = 0
        completed = 1
        started = 2
        stopped = 3
     */
    public static ByteBuffer createRequest(long connectionId,
            int transactionId, byte[] infoHash, byte[] peerId, long downloaded,
            long uploaded, long left, int event,
            int key, int numWant, int port) {

        ByteBuffer data = ByteBuffer.allocate(UDP_ANNOUNCE_REQUEST_MESSAGE_SIZE);
        data.putLong(connectionId);
        data.putInt(ANNOUNCE_ID); //action announce
        data.putInt(transactionId);
        data.put(infoHash);
        data.put(peerId);
        data.putLong(downloaded);
        data.putLong(left);
        data.putLong(uploaded);
        data.putInt(event);
        data.putInt(0); //Your ip address. Set to 0 if you want the tracker to use the sender of this udp packet.
        data.putInt(key);
        data.putInt(numWant);
        data.putShort((short) port);
         data.rewind();
        return data;
    }
    
    public static ByteBuffer createConnection(int transaction_id)
    {
        ByteBuffer data = ByteBuffer.allocate(UDP_CONNECTION_REQUEST_MESSAGE_SIZE);
        data.putLong(CONNECTION_ID_MAGIC_NUMBER);
        data.putInt(0); //action connection
        data.putInt(transaction_id);
        data.rewind();
        return data;
    }
    
    public static ByteBuffer createScrape(long connection_id, int transaction_id, byte[] info_hash)
    {
        ByteBuffer data = ByteBuffer.allocate(16+20);
        data.putLong(connection_id);
        data.putInt(2);
        data.putInt(transaction_id);
        data.put(info_hash);
        data.rewind();
        return data;
    }
}
