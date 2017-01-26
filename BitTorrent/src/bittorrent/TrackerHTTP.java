/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TrackerHTTP extends TrackerBase {
    
    public static int port = 6881;
    public static final String BYTE_ENCODING = "ISO-8859-1";
    
    public static URL buildAnnounceURL(String trackerUrl, byte[] info_hash,
            byte[] peer_id, String port, long downloaded, long left, String event)
            throws UnsupportedEncodingException, MalformedURLException {
        
        StringBuilder url = new StringBuilder(trackerUrl);
        url.append(trackerUrl.contains("?") ? "&" : "?")
                .append("info_hash=")
                .append(URLEncoder.encode(
                        new String(info_hash, BYTE_ENCODING),
                        BYTE_ENCODING))
                .append("&peer_id=")
                .append(URLEncoder.encode(
                        new String(peer_id, BYTE_ENCODING),
                        BYTE_ENCODING))
                .append("&port=").append(port)
                .append("&uploaded=").append("0")
                .append("&downloaded=").append(Long.toString(downloaded))
                .append("&left=").append(Long.toString(left))
                //.append("&compact=").append(this.getCompact() ? 1 : 0)
                //.append("&no_peer_id=").append(this.getNoPeerIds() ? 1 : 0);
                .append("&numwant=").append("50")
                .append("&event=").append(event);
        
        return new URL(url.toString());
    }
    
    public void trackerRequest(Torrent torrent) {
        new Thread() {
            @Override
            public void run() {
                try {
                    URL url = buildAnnounceURL(fullName,
                            torrent.getInfoHash(),
                            Client.CLIENT_ID.getBytes(),
                            Integer.toString(port),
                            torrent.getCompleteBytes(),
                            torrent.getTotalSize() - torrent.getCompleteBytes(),
                            torrent.getEvent());
                    System.out.println(url);
                    send(url, torrent);
                    
                } catch (UnsupportedEncodingException | MalformedURLException ex) {
                    Logger.getLogger(TrackerHTTP.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    //Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
                    System.err.println("Tracker not response: " + fullName);
                }
                
            }
        }.start();
    }
    
    public void scrapeRequest(Torrent torrent) {
        new Thread() {
            @Override
            public void run() {
                try {
                    String urlScrape = fullName.replace("announce", "scrape");
                    StringBuilder url = new StringBuilder(urlScrape);
                    url.append(urlScrape.contains("?") ? "&" : "?")
                            .append("info_hash=")
                            .append(URLEncoder.encode(
                                    new String(torrent.getInfoHash(), BYTE_ENCODING),
                                    BYTE_ENCODING));
                    System.out.println(url);
                    sendScrape(new URL(url.toString()), torrent);
                    
                } catch (UnsupportedEncodingException | MalformedURLException ex) {
                    Logger.getLogger(TrackerHTTP.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    //Logger.getLogger(Tracker.class.getName()).log(Level.SEVERE, null, ex);
                    System.err.println("Tracker not response: " + fullName);
                }
                
            }
        }.start();
    }
    
    public void send(URL target, final Torrent t) throws Exception {
        
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = (HttpURLConnection) target.openConnection();
            in = conn.getInputStream();
        } catch (IOException ioe) {
            status = "fail";
            if (conn != null) {
                in = conn.getErrorStream();
            }
        }
        
        if (in == null) {
            status = "fail";
            throw new Exception("No response or unreachable tracker!");
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int len;
        byte[] buffer = new byte[4096];
        while (-1 != (len = in.read(buffer))) {
            baos.write(buffer, 0, len);
        }
        HTTPResponse tr = HTTPResponse.parse(baos.toByteArray());
        t.getNewPeers().addAll(tr.getPeerList());
        peerCount = tr.getPeerList().size();
        status = "ok";
        leechers = tr.getLeechers();
        seeders = tr.getSeeders();
        interval = tr.getInterval();
        incomplete = tr.getIncomplete();
        downloaded = tr.getDownloaded();
        complete = tr.getComplete();
        in.close();
    }
    
    public void sendScrape(URL target, final Torrent t) throws Exception {
        
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = (HttpURLConnection) target.openConnection();
            in = conn.getInputStream();
        } catch (IOException ioe) {
            status = "fail";
            if (conn != null) {
                in = conn.getErrorStream();
            }
        }
        
        if (in == null) {
            status = "fail";
            throw new Exception("No response or unreachable tracker!");
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int len;
        byte[] buffer = new byte[4096];
        while (-1 != (len = in.read(buffer))) {
            baos.write(buffer, 0, len);
        }
        HTTPResponse tr = HTTPResponse.parse(baos.toByteArray());
        incomplete = tr.getIncomplete();
        downloaded = tr.getDownloaded();
        complete = tr.getComplete();
        in.close();
    }
    
    public static String getIP(byte[] data) {
        if (data.length < 6) {
            return "";
        }
        
        int[] dec = new int[6];
        for (int i = 0; i < 6; i++) {
            String hex = String.format("%02X", data[i]);
            //System.out.print(hex + " ");
            dec[i] = Integer.parseInt(hex, 16);
        }
        
        String ip = Integer.toString(dec[0]);
        
        for (int i = 1; i < 4; i++) {
            ip += "." + Integer.toString(dec[i]);
        }
        ip += ":" + Integer.toString(dec[4] << 8 | dec[5]);
        
        return ip;
    }
    
    public static void trackerHTTPService(Torrent torrent) {
        ArrayList<TrackerHTTP> trackers = new ArrayList<TrackerHTTP>();
        for (String trackerUrl : torrent.getTrackerUrlHTTP()) {
            if (trackerUrl.contains("udp://")) {
                continue;
            }
            TrackerHTTP tracker = new TrackerHTTP(trackerUrl);
            trackers.add(tracker);
            torrent.getTrackers().add(tracker);
            tracker.trackerRequest(torrent);
            //bez scrape
            /*if (trackerUrl.contains("announce")) {
                tracker.scrapeRequest(torrent);
            }*/
        }
    }
    
    @Override
    public String getStatus() {
        return status;
    }
    
    private String status = "working..";
    
    public TrackerHTTP(String url) {
        fullName = url;
    }
}
