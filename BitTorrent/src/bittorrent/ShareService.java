/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShareService {

    private volatile int peers = 0;
    private final int MAX_PEERS = 10;
    ArrayList<Torrent> torrents;

    public void run(ArrayList<Torrent> torrents) {
        this.torrents = torrents;
        new Thread() {
            @Override
            public void run() {

                System.out.println("ShareService!");
                server();

            }
        }.start();
    }

    public void server() {

        try {
            Selector selector = Selector.open();

            ServerSocketChannel crunchifySocket = ServerSocketChannel.open();
            InetSocketAddress crunchifyAddr = new InetSocketAddress("localhost", 6881);
            crunchifySocket.bind(crunchifyAddr);
            crunchifySocket.configureBlocking(false);
            int ops = crunchifySocket.validOps();
            SelectionKey selectKy = crunchifySocket.register(selector, ops, null);
            while (true) {
                selector.select();
                Set<SelectionKey> crunchifyKeys = selector.selectedKeys();
                Iterator<SelectionKey> crunchifyIterator = crunchifyKeys.iterator();
                while (crunchifyIterator.hasNext()) {
                    SelectionKey myKey = crunchifyIterator.next();
                    if (myKey.isAcceptable()) {
                        SocketChannel client = crunchifySocket.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                        System.out.println("Connection Accepted: " + client.getLocalAddress());
                    } else if (myKey.isReadable()) {
                        SocketChannel client = (SocketChannel) myKey.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        client.read(buffer);
                        buffer.flip();
                        Handshake handshake = new Handshake(buffer.array(), new byte[20]);
                        for (Torrent t : torrents) {
                            if (java.util.Arrays.equals(t.getInfoHash(), handshake.getInfoHash())) {
                                if (t.getWorkingPeers().size() < 20) {
                                    t.getWorkingPeers().add(new Peer(client, t.getPiecesCount()));
                                }
                                break;
                            }
                        }
                    }
                    crunchifyIterator.remove();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ShareService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
