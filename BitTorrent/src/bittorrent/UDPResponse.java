/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class UDPResponse {

    public enum Action {
        Connect(0), Announce(1), Scrape(2), Error(3);
        private int id = 0;

        Action(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    };

    private Action action = null;
    private int transaction_id = 0;
    private long connection_id = 0;
    private int interval = 0;
    private int leechers = 0;
    private int seeders = 0;
    private int downloaded = 0;
    private int complete = 0;
    private int incomplete = 0;
    private ArrayList<Peer> peers = new ArrayList<Peer>();
    private String error = "";
    
    private UDPResponse() {

    }
    
    public int getInterval()
    {
        return interval;
    }

    public Action getAction() {
        return action;
    }
    
    public ArrayList<Peer> getPeers()
    {
        return peers;
    }
    
    public int getTransactionId()
    {
        return transaction_id;
    }
    
    public long getConnectionId()
    {
        return connection_id;
    }
    
    public int getLeechers()
    {
        return leechers;
    }

    public int getSeeders() {
        return seeders;
    }

    public int getDownloaded() {
        return downloaded;
    }

    public int getComplete() {
        return complete;
    }

    public int getIncomplete() {
        return incomplete;
    }

    public static UDPResponse parse(ByteBuffer data) {
        UDPResponse rsp = new UDPResponse();

        int action_id = data.getInt();
        if (action_id > 3) {
            return null;
        }
        rsp.action = Action.values()[action_id];
        switch (rsp.action) {
            case Connect:
                rsp.transaction_id = data.getInt();
                rsp.connection_id = data.getLong();
                break;
            case Announce:
                rsp.transaction_id = data.getInt();
                //System.out.println("Announce "+rsp.transaction_id+" "+rsp.action.toString());
                rsp.interval = data.getInt();
                rsp.leechers = data.getInt();
                rsp.seeders = data.getInt();
                while (data.remaining() > 0) {
                    byte[] bbb = new byte[6];
                    data.get(bbb);
                    String ip = TrackerHTTP.getIP(bbb);
                    rsp.peers.add(new Peer(ip.split(":")[0], 
                            Integer.parseInt(ip.split(":")[1])));
                }
                break;
            case Scrape:
                rsp.transaction_id = data.getInt();
                rsp.complete = data.getInt();
                rsp.downloaded = data.getInt();
                rsp.incomplete = data.getInt();
                break;
            case Error:
                rsp.transaction_id = data.getInt();
                while(data.remaining() > 0)
                {
                    rsp.error += (char)data.get();
                }
                System.out.println("Error: "+rsp.error);
                break;
            default:
                throw new AssertionError(rsp.action.name());

        }
        return rsp;
    }
}
