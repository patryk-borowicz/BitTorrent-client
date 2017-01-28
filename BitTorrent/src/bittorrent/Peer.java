/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Peer {

    public enum Status {
        NOT_STARTED, CONNECTING, FAIL, CONNECTED, DISCONNECTED
    };
    private String ip = "";
    private int port = 0;
    private byte[] ID = null;
    private String clientName = "-";
    private byte[] bitfield = new byte[0];
    private int piecesCount = 0;
    private SortedMap<Integer, byte[]> blocksData;
    private long time = 0;
    private HashMap<Integer, Integer> blockSizes;
    private Queue<Integer> havePieces = new LinkedList<Integer>();
    private SocketChannel client;
    private Status status = Status.NOT_STARTED;
    private boolean authenticated = false;
    private boolean handshakeSended = false;
    private final int BUFFER_LIMIT = 32768;
    private ByteBuffer buffer;
    private final int ALIVE_TIME = 1000 * 120;
    private boolean isChocked = false;
    private Piece actualPieceWait = null;
    private final static int BLOCK_SIZE = (int) Math.pow(2, 14);
    private long downloadTotal = 0;
    private Queue<RequestedBlock> requestedBlocks = new LinkedList<RequestedBlock>();

    public Queue<RequestedBlock> getRequestedBlocks() {
        return requestedBlocks;
    }

    
    public Peer(SocketChannel channel, int pieceCount) throws IOException {
        this.ip = channel.getRemoteAddress().toString();
        this.port = channel.socket().getPort();
        client = channel;
        status = Status.CONNECTED;
        authenticated = true;
        handshakeSended = false;
        buffer = ByteBuffer.allocate(BUFFER_LIMIT);
        bitfield = new byte[(int)Math.ceil(pieceCount/8.0)];
    }

    public Peer(String ip, int port ) {
        this.ip = ip;
        this.port = port;
    }

    public String getIP() {
        return ip;
    }

    public String getClientName() {
        return clientName;
    }

    public int getPieces() {
        int pieces = 0;
        int offset = 0;
        for (byte b : bitfield) {
            for (int i = 7; i >= 0; i--, offset++) {
                if (offset == piecesCount) {
                    return pieces;
                }

                int bit = (b >> i) & 1;
                pieces += bit;
            }
        }
        return pieces;
    }

    public String getStatus() {
        String message = "-";
        switch (status) {
            case NOT_STARTED:
                break;
            case CONNECTING:
                message = "-connecting-";
                break;
            case FAIL:
                message = ">fail<";
                break;
            case CONNECTED:
                message = "[connected]";
                break;
            case DISCONNECTED:
                message = "disconnected";
                break;
        }
        return message;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        for (int i = 0; i < ip.length(); i++) {
            hash = hash * 31 + ip.charAt(i);
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Peer other = (Peer) obj;
        if (!Objects.equals(this.ip, other.ip)) {
            return false;
        }
        return true;
    }

    public void setConnection() {
        try {
            InetSocketAddress hostAddress = new InetSocketAddress(ip, port);
            client = SocketChannel.open();
            client.configureBlocking(false);
            client.connect(hostAddress);
            status = Status.CONNECTING;
            authenticated = false;
            handshakeSended = false;
            buffer = ByteBuffer.allocate(BUFFER_LIMIT);
        } catch (IOException ex) {
            System.err.println("Timeout peer: " + ip);
            status = Status.FAIL;
        }
    }

    public void readData(Torrent torrent) {
        if (status == Status.NOT_STARTED
                || status == Status.FAIL
                || status == Status.DISCONNECTED) {
            return;
        }
        try {
            if (!client.finishConnect()) {
                return;
            }
        } catch (IOException ex) {
            //Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            status = Status.FAIL;
        }

        if (status == Status.CONNECTING) {
            if (client.isConnected()) {
                //System.out.println("Polaczono z " + ip);
                status = Status.CONNECTED;
            } else {
                return;
            }
        }
        if (!client.isConnected()) {
            status = Status.FAIL;
            if (actualPieceWait != null) {
                actualPieceWait.setRequested(false);
            }
            return;
        }
        try {
            if (!handshakeSended) {
                client.write(ByteBuffer.wrap(Handshake.parseRequest(torrent.getInfoHash())));
                ByteArrayOutputStream _bitfield = new ByteArrayOutputStream();
                byte[] bb = Piece.toBitfield(torrent.getPieces(), torrent.getPiecesCount());
                _bitfield.write(bb);
                client.write(ByteBuffer.wrap(
                        PeerRequest.create(PeerResponse.MessageType.BITFIELD,
                                _bitfield)));
                handshakeSended = true;
            }

            if (System.currentTimeMillis() - time > 20000 && actualPieceWait != null) {
                System.err.println("Timeout: " + ip + " index " + actualPieceWait.getIndex() 
                + " Blocks: "+blocksData.size() +"/"+blockSizes.size());
                actualPieceWait.getSeeders().remove(this);
                actualPieceWait.setRequested(false);
                actualPieceWait = null;
            }
            client.read(buffer);

            while (buffer.position() > 0) {
                if (authenticated) {
                    PeerResponse pr = PeerResponse.parse(buffer);
                    switch (pr.getResult()) {
                        case KEEP_ALIVE:
                            break;
                        case CHOKE:
                            isChocked = true;
                            break;
                        case UNCHOKE:
                            isChocked = false;
                            break;
                        case INTERESTED:
                            client.write(ByteBuffer.wrap(
                        PeerRequest.create(PeerResponse.MessageType.UNCHOKE)));
                            break;
                        case NOTINTERESTED:
                            break;
                        case HAVE:
                            bitfield[pr.getIndex() / 8] |= (1 << 7 - (pr.getIndex() % 8));
                            torrent.addPiece(pr.getIndex(), this);
                            break;
                        case BITFIELD:
                            bitfield = pr.getBitfield();
                            piecesCount = torrent.getPiecesCount();
                            torrent.addPiecesFromBitfield(bitfield, this);
                            break;
                        case PIECE:
                            time = System.currentTimeMillis();
                            blocksData.put(pr.getBegin(), pr.getBlock());
                            //System.out.println(" Block: "+blocksData.size() +"/"+blockSizes.size()+" Index: "+actualPieceWait.getIndex());
                            if (blocksData.size() == blockSizes.size()) {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                while (blocksData.size() > 0) {
                                    int key = blocksData.firstKey();
                                    baos.write(blocksData.remove(key));
                                }
                                boolean result = false;
                                if(actualPieceWait != null)
                                    result = actualPieceWait.checkData(baos.toByteArray());

                                if (result) {
                                    actualPieceWait.setCompleted();
                                    downloadTotal += baos.size();
                                    //System.out.println("---[" + ip + "]PIECE OK: " + actualPieceWait.getIndex());
                                    actualPieceWait.keepData(baos.toByteArray());
                                    havePieces.add(actualPieceWait.getIndex());
                                } else {
                                    if(actualPieceWait != null)
                                        actualPieceWait.setRequested(false);
                                    System.err.println("PIECE FAIL: " + actualPieceWait.getIndex());
                                }

                                actualPieceWait = null;
                            }
                            break;
                        case REQUEST:
                            RequestedBlock block = new RequestedBlock(
                                    pr.getIndex(), pr.getBegin(), pr.getLength());
                            requestedBlocks.add(block);
                            break;
                        case CANCEL:
                            break;
                        case FAIL:
                            System.err.println("FAIL!");
                            break;
                        case SHORT_LENGTH:
                            break;
                    }

                    if (pr.getResult() != PeerResponse.MessageType.SHORT_LENGTH) {
                        shiftBytes(buffer, pr.allMessageBytes());
                    } else {
                        break;
                    }

                } else if (buffer.position() >= Handshake.HANDSHAKE_LENGTH && !authenticated) {
                    Handshake h = new Handshake(buffer.array(), torrent.getInfoHash());
                    if (!h.valid()) {
                        System.err.println("Zly handshake! koniec "+ ip);
                        status = Status.DISCONNECTED;
                        break;
                    } else {
                        //System.out.println("Klient ID: " + new String(h.getID()));
                        ID = h.getID();
                        clientName = ClientIdentifier.getName(ID);
                        //System.out.println("Rodzaj klienta: " + clientName);
                        authenticated = true;
                    }
                    shiftBytes(buffer, Handshake.HANDSHAKE_LENGTH);
                } else {
                    break;
                }
            }

        } catch (IOException ex) {
            //Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            status = Status.FAIL;
            if (actualPieceWait != null) {
                actualPieceWait.setRequested(false);
            }
        }
    }

    public void stopConnection() {
        if (status == Status.CONNECTED || status == Status.CONNECTING) {
            status = Status.DISCONNECTED;
            try {
                client.close();
            } catch (IOException ex) {
                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void shiftBytes(ByteBuffer b, int count) {
        if (count == b.position()) {
            b.rewind();
            return;
        }
        byte[] arr = b.array();
        for (int i = count; i < b.position(); i++) {
            arr[i - count] = arr[i];
        }
        int newPos = b.position() - count;
        b.rewind();
        b.get(arr);
        b.position(newPos);
    }

    public Status _getStatus() {
        return status;
    }

    public void keepAlive() {
        if (status == Status.CONNECTED && authenticated) {
            try {
                ByteBuffer buff = ByteBuffer.wrap(
                        PeerRequest.create(PeerResponse.MessageType.KEEP_ALIVE));
                client.write(buff);
            } catch (IOException ex) {
                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public boolean isChocked() {
        return isChocked;
    }

    public boolean isRequested() {
        return actualPieceWait != null;
    }

    public void sendRequest(Piece p, Torrent torrent) {
        if (actualPieceWait != null) {
            return;
        }
        interested();
        p.setRequested(true);

        actualPieceWait = p;
        blocksData = new TreeMap<Integer, byte[]>();
        blockSizes = new HashMap<Integer, Integer>();

        long indexOffset = p.getIndex() * torrent.getPieceLength();
        long pieceEnd = indexOffset + torrent.getPieceLength();
        long pieceStart = indexOffset;
        while (indexOffset < pieceEnd) {
            long offset = indexOffset;
            indexOffset += BLOCK_SIZE;
            if (indexOffset > torrent.getTotalSize()) {
                indexOffset = torrent.getTotalSize() - offset;
                blockSizes.put((int) (offset - pieceStart), (int) (indexOffset));
                write(p.getIndex(), (int) (offset - pieceStart), (int) (indexOffset));
                break;
            }
            blockSizes.put((int) (offset - pieceStart), (int) (indexOffset - offset));
            write(p.getIndex(), (int) (offset - pieceStart), (int) (indexOffset - offset));
        }
    }

    public long getDownloadTotal() {
        return downloadTotal;
    }
    
    public long getUploadTotal()
    {
        return uploadTotal;
    }
    private long uploadTotal = 0;
    public boolean isConnected() {
        return Status.CONNECTED == status && isAuthenticated();
    }

    public int getPort() {
        return port;
    }

    public boolean hasPiece(int index) {
        if (bitfield == null || piecesCount == 0) {
            return false;
        }
        return ((bitfield[index / 8] >> (7 - (index % 8))) & 0x01) == 1;
    }

    public void have(int index) {
        if (hasPiece(index)) {
            return;
        }
        if (status == Status.CONNECTED && authenticated) {
            try {
                //System.out.println("Have: "+ip +" index: "+index);
                ByteBuffer buff = ByteBuffer.wrap(
                        PeerRequest.create(PeerResponse.MessageType.HAVE, index));
                client.write(buff);
            } catch (IOException ex) {
                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public int getHave() {
        if (havePieces.isEmpty()) {
            return -1;
        } else {
            return havePieces.remove();
        }
    }

    private void write(int index, int begin, int length) {
        try {
            client.write(ByteBuffer.wrap(
                    PeerRequest.create(PeerResponse.MessageType.REQUEST,
                            index,
                            begin,
                            length)));
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void interested() {
        try {
            ByteBuffer buff = ByteBuffer.wrap(
                    PeerRequest.create(PeerResponse.MessageType.INTERESTED));
            client.write(buff);
            time = System.currentTimeMillis();
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void piece(int index, int begin, ByteArrayOutputStream data)
    {
        if (status == Status.CONNECTED && authenticated) {
            try {
                uploadTotal += data.size();
                ByteBuffer buff = ByteBuffer.wrap(
                        PeerRequest.create(PeerResponse.MessageType.PIECE, index, begin, data));
                client.write(buff);
            } catch (IOException ex) {
                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public int getDownloadSpeed()
    {
        return 0;
    }
    
    public int getUploadSpeed()
    {
        return 0;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

}
