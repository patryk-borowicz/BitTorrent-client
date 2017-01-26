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
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TrackerUDP {

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static void request(Torrent t) {

        new Thread() {

            @Override
            public void run() {
                Set<String> urls = t.getTrackerUrlUDP();
                for (String url : urls) {
                    try {
                        if (url.contains("http://")) {
                            continue;
                        }

                        url = url.replaceAll("/announce", "");
                        url = url.replaceAll("udp://", "");
                        String ip = url.split(":")[0];
                        int port = Integer.parseInt(url.split(":")[1]);
                        System.out.println("IP " + ip + " port " + port);
                        
                        Random random = new Random();
                        int transaction_id = random.nextInt();
                        System.out.println("ID: " + transaction_id);
                        //czekac 15 sekund na kolejne zapytanie, po 60 odpuscic - 3 proby?
                        long time = System.currentTimeMillis();
                        ByteBuffer buffer = ByteBuffer.allocate(UDPRequest.UDP_CONNECTION_REQUEST_MESSAGE_SIZE);
                        InetSocketAddress myAddress = new InetSocketAddress(ip, port);
                        DatagramChannel datagramChannel = DatagramChannel.open();
                        datagramChannel.configureBlocking(false);
                        datagramChannel.bind(null);
                        datagramChannel.send(UDPRequest.createConnection(transaction_id),
                                myAddress);

                        while (System.currentTimeMillis() - time < 5000) {
                            datagramChannel.receive(buffer);
                            if (buffer.remaining() == 0) {
                                buffer.rewind();
                                UDPResponse response = UDPResponse.parse(buffer);
                                if (response.getTransactionId() == transaction_id
                                        && response.getAction() == UDPResponse.Action.Connect) {
                                    //System.out.println("weszlo!");
                                    datagramChannel.send(UDPRequest.createRequest(
                                            response.getConnectionId(),
                                            transaction_id,
                                            t.getInfoHash(),
                                            Client.CLIENT_ID.getBytes(),
                                            0, 0, t.getTotalSize(),
                                            2, 64, 50, 6881), myAddress);

                                    long time2 = System.currentTimeMillis();
                                    buffer = ByteBuffer.allocate(2048);

                                    while (System.currentTimeMillis() - time2 < 5000) {
                                        datagramChannel.receive(buffer);
                                        if (buffer.position() >= 20) {
                                            buffer.flip();
                                            UDPResponse peerResponse = UDPResponse.parse(buffer);
                                            if (peerResponse.getTransactionId() == transaction_id
                                                    && peerResponse.getAction() == UDPResponse.Action.Announce) {
                                                //System.out.println("weszlo2");
                                                t.getNewPeers().addAll(peerResponse.getPeers());
                                                System.out.println("Peers OK");
                                                buffer = ByteBuffer.allocate(20);
                                                datagramChannel.send(UDPRequest.createScrape(
                                                        response.getConnectionId(), //response
                                                        transaction_id, t.getInfoHash()), myAddress);
                                                long time3 = System.currentTimeMillis();
                                                while (System.currentTimeMillis() - time3 < 5000) {
                                                    datagramChannel.receive(buffer);
                                                    if (buffer.remaining() == 0) {
                                                        buffer.flip();
                                                        UDPResponse scrapeResponse = UDPResponse.parse(buffer);
                                                        if (scrapeResponse.getTransactionId() == transaction_id
                                                                && scrapeResponse.getAction() == UDPResponse.Action.Scrape) {
                                                            System.out.println("Scrape OK");
                                                            break;
                                                        }
                                                        else
                                                        {
                                                            break;
                                                        }
                                                    }
                                                }
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(TrackerUDP.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
            }
        }.start();
    }
}
