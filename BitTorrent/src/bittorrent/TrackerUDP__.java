/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TrackerUDP__ extends TrackerBase{

    @Override
    public String getStatus() {
        if(status == Status.FAIL)
            return "fail";
        if(status == Status.WAIT)
            return "ok";
        
        return "working..";
    }

    public enum Status {
        NOT_STARTED, STARTED, CONNECTED, ANNOUNCED, FAIL, WAIT
    };
    private Status status = Status.NOT_STARTED;
    private String ip = "";
    private int port = 0;
    private Random random = new Random();
    private int transaction_id = 0;
    private DatagramChannel datagramChannel;
    private InetSocketAddress adress;
    private long time = 0;
    private ByteBuffer buffer;
    private long connection_id = 0;

    public TrackerUDP__(String url) {
        fullName = url;
        url = url.replaceAll("/announce", "");
        url = url.replaceAll("udp://", "");
        ip = url.split(":")[0];
        port = Integer.parseInt(url.split(":")[1]);
        System.out.println("IP " + ip + " port " + port);
        adress = new InetSocketAddress(ip, port);
    }

    public void service(Torrent torrent) {
        if (status == Status.WAIT && System.currentTimeMillis() - time > interval * 1000) {
            status = Status.NOT_STARTED;
            time = System.currentTimeMillis();
        } else if(status == Status.WAIT)
        {
            return;
        }

        if (status == Status.FAIL) {
            return;
        }

        if (status == Status.NOT_STARTED) {
            status = Status.STARTED;
            transaction_id = random.nextInt();
            buffer = ByteBuffer.allocate(UDPRequest.UDP_CONNECTION_REQUEST_MESSAGE_SIZE);
            try {
                datagramChannel = DatagramChannel.open();
                datagramChannel.configureBlocking(false);
                datagramChannel.bind(null);
                datagramChannel.send(UDPRequest.createConnection(transaction_id),
                        adress);
            } catch (UnresolvedAddressException | IOException ex) {
                //Logger.getLogger(TrackerUDP__.class.getName()).log(Level.SEVERE, null, ex);
                status = Status.FAIL;
            }
            time = System.currentTimeMillis();
            return;
        }

        if (System.currentTimeMillis() - time > 15000) {
            System.err.println("Timeout UDP! " + ip);
            status = Status.FAIL;
            return;
        }

        if (status == Status.STARTED) {
            try {
                datagramChannel.receive(buffer);
            } catch (IOException ex) {
                Logger.getLogger(TrackerUDP__.class.getName()).log(Level.SEVERE, null, ex);
                status = Status.FAIL;
            }
            if (buffer.remaining() == 0) {
                buffer.rewind();
                UDPResponse response = UDPResponse.parse(buffer);
                if (response.getTransactionId() == transaction_id
                        && response.getAction() == UDPResponse.Action.Connect) {
                    connection_id = response.getConnectionId();
                    try {
                        datagramChannel.send(UDPRequest.createRequest(
                                response.getConnectionId(),
                                transaction_id,
                                torrent.getInfoHash(),
                                Client.CLIENT_ID.getBytes(),
                                0, 0, torrent.getTotalSize(),
                                torrent.getEventInt(), 64, 50, 6881), adress);
                    } catch (IOException ex) {
                        Logger.getLogger(TrackerUDP__.class.getName()).log(Level.SEVERE, null, ex);
                        status = Status.FAIL;
                    }

                    time = System.currentTimeMillis();
                    buffer = ByteBuffer.allocate(2048);
                    status = Status.CONNECTED;
                } else {
                    status = Status.FAIL;
                }
            }
            return;
        }

        if (status == Status.CONNECTED) {
            try {
                datagramChannel.receive(buffer);
            } catch (IOException ex) {
                Logger.getLogger(TrackerUDP__.class.getName()).log(Level.SEVERE, null, ex);
                status = Status.FAIL;
            }
            if (buffer.position() >= 20) {
                buffer.flip();
                UDPResponse peerResponse = UDPResponse.parse(buffer);
                if (peerResponse.getTransactionId() == transaction_id
                        && peerResponse.getAction() == UDPResponse.Action.Announce) {
                    torrent.getNewPeers().addAll(peerResponse.getPeers());
                    interval = peerResponse.getInterval();
                    //System.out.println("Peers OK");
                    buffer = ByteBuffer.allocate(20);
                    try {
                        datagramChannel.send(UDPRequest.createScrape(
                                connection_id, //response
                                transaction_id, torrent.getInfoHash()), adress);
                    } catch (IOException ex) {
                        Logger.getLogger(TrackerUDP__.class.getName()).log(Level.SEVERE, null, ex);
                        status = Status.FAIL;
                    }
                    time = System.currentTimeMillis();
                    status = Status.ANNOUNCED;
                    leechers = peerResponse.getLeechers();
                    seeders = peerResponse.getSeeders();
                    peerCount = peerResponse.getPeers().size();
                } else {
                    status = Status.FAIL;
                }
            }
            return;
        }

        if (status == Status.ANNOUNCED) {
            try {
                datagramChannel.receive(buffer);
            } catch (IOException ex) {
                Logger.getLogger(TrackerUDP__.class.getName()).log(Level.SEVERE, null, ex);
                status = Status.FAIL;
            }
            if (buffer.remaining() == 0) {
                buffer.flip();
                UDPResponse scrapeResponse = UDPResponse.parse(buffer);
                if (scrapeResponse.getTransactionId() == transaction_id
                        && scrapeResponse.getAction() == UDPResponse.Action.Scrape) {
                    //System.out.println("Scrape OK WAIT: "+interval);
                    status = Status.WAIT;
                    time = System.currentTimeMillis();
                    incomplete = scrapeResponse.getIncomplete();
                    complete = scrapeResponse.getComplete();
                    downloaded = scrapeResponse.getDownloaded();
                } else {
                    status = Status.FAIL;
                }
            }
            return;
        }
    }
    
    public static void trackerUDPService(Torrent torrent)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                ArrayList <TrackerUDP__> trackers = new ArrayList<TrackerUDP__>();
                for(String url : torrent.getTrackerUrlUDP())
                {
                    TrackerUDP__ tracker = new TrackerUDP__(url);
                    trackers.add(tracker);
                    torrent.getTrackers().add(tracker);
                }
                while(torrent.isWorking())
                {
                    for(TrackerUDP__ tracker : trackers)
                    {
                        tracker.service(torrent);
                    }
                }
                System.out.println("UDP STOP!");
            }
        }.start();
    }
}
