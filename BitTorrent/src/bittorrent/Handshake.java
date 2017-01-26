/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import static bittorrent.PeerResponse.HASH_BYTES;
import static bittorrent.PeerResponse.ID_BYTES;
import static bittorrent.PeerResponse.SKIP_BYTES;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Handshake {
    public final static String PROTOCOL = "BitTorrent protocol";
    public final static int HANDSHAKE_LENGTH = 1 + PROTOCOL.length() + 8 + 20 + 20;

    byte[] infoHash;
    byte[] peerName;

    public static byte[] parseRequest(byte[] torrentInfoHash) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write((byte) PROTOCOL.length());
        out.write(PROTOCOL.getBytes());
        for (int i = 0; i < 8; i++) {
            out.write(0);
        }
        out.write(torrentInfoHash);
        out.write(Client.CLIENT_ID.getBytes());
        return out.toByteArray();
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public byte[] getID() {
        return peerName;
    }
    
    public boolean valid()
    {
        return valid;
    }

    private boolean valid = false;

    public Handshake(byte[] data, byte[] sha1) {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        if (in.read() == PROTOCOL.length()) {
            String protocol = new String(PeerResponse.getBytesFromStream(in, PROTOCOL.length()));
            if (protocol.equals(PROTOCOL)) {
                in.skip(SKIP_BYTES);
                infoHash = PeerResponse.getBytesFromStream(in, HASH_BYTES);
                peerName = PeerResponse.getBytesFromStream(in, ID_BYTES);
                valid = java.util.Arrays.equals(sha1, infoHash);
            }
        }
    }
}
