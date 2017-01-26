/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import bencode.BElement;
import bencode.BReader;
import bencode.types.BMap;
import bencode.types.BNumber;
import bencode.types.BString;
import static bittorrent.TrackerHTTP.getIP;
import java.util.ArrayList;
import java.util.Arrays;

public class HTTPResponse {

    private ArrayList<Peer> peerList;
    private int interval = 0;
    private int leechers = 0;
    private int seeders = 0;
    private int incomplete = 0;
    private int complete = 0;
    private int downloaded = 0;

    public static HTTPResponse parse(byte[] response) {
        HTTPResponse tr = new HTTPResponse();
        BElement[] e = BReader.read(response);
        /*for (byte b : response) {
            System.out.print((char) b);
        }*/
        System.out.println("");
        //BReader.showAll(e[0], "");
        BMap map = (BMap) e[0];

        final ArrayList<Peer> peerList = new ArrayList<Peer>();

        BNumber intervalB = ((BNumber) TorrentItem.getBElement(map, "interval"));
        if (intervalB != null) {
            tr.interval = intervalB.getValue();
        }

        BNumber leechersB = ((BNumber) TorrentItem.getBElement(map, "leechers"));
        if (leechersB != null) {
            tr.leechers = leechersB.getValue();
        }

        BNumber seedersB = ((BNumber) TorrentItem.getBElement(map, "seeders"));
        if (seedersB != null) {
            tr.seeders = seedersB.getValue();
        }

        BNumber downloadedB = ((BNumber) TorrentItem.getBElement(map, "downloaded"));
        if (downloadedB != null) {
            tr.downloaded = downloadedB.getValue();
        }

        BNumber incompleteB = ((BNumber) TorrentItem.getBElement(map, "incomplete"));
        if (incompleteB != null) {
            tr.incomplete = incompleteB.getValue();
        }

        BNumber completeB = ((BNumber) TorrentItem.getBElement(map, "complete"));
        if (completeB != null) {
            tr.complete = completeB.getValue();
        }

        BString peerIPs = ((BString) TorrentItem.getBElement(map, "peers"));
        if (peerIPs != null) {
            byte[] tmp = peerIPs.bytes;
            for (int i = 0; i < tmp.length; i += 6) {
                String ip = getIP(Arrays.copyOfRange(tmp, i, i + 6));
                peerList.add(new Peer(ip.split(":")[0], Integer.parseInt(ip.split(":")[1])));
            }
            // System.out.println("Tracker OK");
        }

        tr.peerList = peerList;
        return tr;
    }

    public ArrayList<Peer> getPeerList() {
        return peerList;
    }

    public int getInterval() {
        return interval;
    }

    public int getLeechers() {
        return leechers;
    }

    public int getSeeders() {
        return seeders;
    }

    public int getIncomplete() {
        return incomplete;
    }

    public int getComplete() {
        return complete;
    }

    public int getDownloaded() {
        return downloaded;
    }

}
