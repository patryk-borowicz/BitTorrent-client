/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import bencode.BElement;
import bencode.BReader;
import bencode.types.BList;
import bencode.types.BMap;
import bencode.types.BNumber;
import bencode.types.BString;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Torrent {

    private volatile boolean isWorking = false;

    private HashSet<String> trackerUrlHTTP = new HashSet<String>();
    private HashSet<String> trackerUrlUDP = new HashSet<String>();
    private ArrayList<TrackerBase> trackers = new ArrayList<TrackerBase>();
    private String name = "";
    private String torrentPath = "";
    private String filesPath = "";
    private byte[] infoHash;
    private byte[][] _pieces;
    private long len = 0;
    private long completeBytes = 0;
    private int pieceLen = 0;
    private int piecesCount = 0;
    private ArrayList<TorrentItem> files;
    //private Queue<Peer> peers = new LinkedList<Peer>();
    private long torrentComplete = 0;
    final int ALIVE_TIME = 1000 * 120; //2minuty
    private List<Piece> pieces = new ArrayList<Piece>();
    private int completedPieces = 0;
    private double downloadSpeed = 0;
    private double remainingTime = 0;

    private BlockingQueue<Peer> newPeers = new ArrayBlockingQueue<Peer>(2048);
    private Set<Peer> workingPeers = new HashSet<Peer>();

    public ArrayList<TrackerBase> getTrackers() {
        return trackers;
    }

    public BlockingQueue<Peer> getNewPeers() {
        return newPeers;
    }

    public String getStatus() {
        return isWorking ? "working" : "stopped";
    }

    public Set<Peer> getWorkingPeers() {
        return workingPeers;
    }

    public int getPieceLength() {
        return pieceLen;
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    private Torrent() {

    }

    public static Torrent readFromFile(String path) {
        //FIXME: wyjatki przy bencode
        try {
            Torrent torrent = new Torrent();
            byte[] bytes = Files.readAllBytes(Paths.get(path));
            BElement[] elements3 = BReader.read(bytes);
            BMap map = (BMap) elements3[0];

            //BReader.showAll(map, "");
            String announce = ((BString) TorrentItem.getBElement(map, "announce")).getValue();
            if (announce.contains("http://")) {
                torrent.trackerUrlHTTP.add(announce);
            } else if (announce.contains("udp://")) {
                torrent.trackerUrlUDP.add(announce);
            }
            BMap mapInfo = (BMap) TorrentItem.getBElement(map, "info");
            BList announceList = (BList) TorrentItem.getBElement(map, "announce-list");
            if (announceList != null) {
                for (BElement b : announceList) {
                    if (b instanceof BList) {
                        BList list = (BList) b;
                        for (BElement tracker : list) {
                            if (tracker instanceof BString) {
                                String url = ((BString) tracker).getValue();
                                if (url.contains("http://")) {
                                    torrent.trackerUrlHTTP.add(url);
                                } else if (url.contains("udp://")) {
                                    torrent.trackerUrlUDP.add(url);
                                }
                            }
                        }
                    }
                }
            }

            torrent.files = TorrentItem.getFiles(mapInfo);

            if (torrent.files.isEmpty()) {
                return null;
            }

            torrent.name = ((BString) TorrentItem.getBElement(mapInfo, "name")).getValue();
            byte[] pieces = ((BString) TorrentItem.getBElement(mapInfo, "pieces")).bytes;
            torrent.pieceLen = ((BNumber) TorrentItem.getBElement(mapInfo, "piece length")).getValue();
            torrent.piecesCount = pieces.length / 20;

            torrent._pieces = new byte[torrent.piecesCount][20];
            for (int i = 0, k = 0; i < torrent.piecesCount; i++) {
                for (int j = 0; j < 20; j++, k++) {
                    torrent._pieces[i][j] = pieces[k];
                }
            }

            torrent.len = 0;
            torrent.files.stream().forEach((t) -> {
                torrent.len += t.getLength();
            });

            //robienie hashu
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BReader.bencode(mapInfo, out);
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(out.toByteArray());
            torrent.infoHash = sha1.digest();
            /*for(TorrentItem item : torrent.files)
            {
                System.out.println(item.toString());
            }*/
            return torrent;

        } catch (IOException | NoSuchAlgorithmException ex) {
            Logger.getLogger(Torrent.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public boolean equals(Torrent t) {
        return java.util.Arrays.equals(infoHash, t.infoHash);
    }

    @Override
    public String toString() {
        return name;
    }

    public long getLen() {
        return len;
    }

    public long getCompleteBytes() {
        return completeBytes;
    }

    public ArrayList<TorrentItem> getFiles() {
        return files;
    }

    public String getSHA1() {
        return bytesToHex(infoHash);
    }

    public HashSet<String> getTrackerUrlHTTP() {
        return trackerUrlHTTP;
    }

    public HashSet<String> getTrackerUrlUDP() {
        return trackerUrlUDP;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public int getPiecesCount() {
        return piecesCount;
    }

    /*public Queue<Peer> getPeers() {
        return peers;
    }*/
    public void addToQueue(Peer peer) {
        newPeers.add(peer);
    }

    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);

    }

    public void start() {
        peerService();
        TrackerHTTP.trackerHTTPService(this);
        TrackerUDP__.trackerUDPService(this);
    }

    public String getEvent() {
        return isCompleted ? "completed" : "started";
    }

    public int getEventInt() {
        return isCompleted ? 1 : 2;
    }

    public void stop() {
        isWorking = false;
    }

    public synchronized double getDownloadSpeed() {
        return downloadSpeed;
    }

    public double getRemainingTime() {
        return remainingTime;
    }

    private void peerService() {
        System.out.println("Torrent service start!");
        Torrent t = this;
        isWorking = true;
        new Thread() {
            @Override
            public void run() {
                workingPeers = Collections.synchronizedSet(new HashSet<>());
                //newPeers.add(new Peer("localhost", 6881));
                long time = System.currentTimeMillis();
                int maxPeers = 10;
                while (true) {
                    if (!isWorking) {
                        System.out.println("Torrent service stop!");
                        for (Peer p : workingPeers) {
                            p.stopConnection();
                        }
                        newPeers.clear();
                        trackers.clear();
                        for (Piece p : pieces) {
                            p.setRequested(false);
                            p.getSeeders().clear();
                        }
                        break;
                    }

                    for (Peer p : workingPeers) {
                        RequestedBlock block = p.getRequestedBlocks().poll();
                        if (block != null) {
                            Piece piece = getPiece(pieces, block.getIndex());
                            if (piece != null && piece.isCompleted()) {
                                System.out.println("PIECE upload" + block.getIndex());
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                baos.write(piece.getData(), block.getBegin(), block.getLength());
                                p.piece(block.getIndex(), block.getBegin(), baos);
                            }
                        }
                    }

                    while (workingPeers.size() < maxPeers && newPeers.size() > 0) {
                        Peer p = newPeers.poll();
                        if (p._getStatus() == Peer.Status.NOT_STARTED) {
                            p.setConnection();
                            workingPeers.add(p);
                        }
                    }
                    Queue<Peer> toRemove = new LinkedList<Peer>();
                    for (Peer peer : workingPeers) {
                        if (peer._getStatus() == Peer.Status.FAIL) {
                            toRemove.add(peer);
                            continue;
                        }
                        peer.readData(t);
                        int index = peer.getHave();
                        if (index != -1) {
                            for (Peer _peer : workingPeers) {
                                _peer.have(index);
                            }
                        }
                    }
                    while (toRemove.size() > 0) {
                        workingPeers.remove(toRemove.poll());
                    }

                    Piece rarest = rarestPiece();
                    if (rarest != null) {
                        for (Peer peer : rarest.getSeeders()) {
                            if (!peer.isRequested() && peer.isConnected() && peer.hasPiece(rarest.getIndex())) {
                                peer.sendRequest(rarest, t);
                                break;
                            }
                        }
                    }

                    long _completed = 0;
                    int _completedPieces = 0;

                    for (Piece p : pieces) {
                        if (p.isCompleted()) {
                            _completedPieces++;
                            _completed += p.getData().length;
                        }
                    }
                    completedPieces = _completedPieces;
                    completeBytes = _completed;
                    downloadSpeed = (double) completeBytes / ((double) (System.currentTimeMillis() - time) / 1000.0);
                    remainingTime = (double) (piecesCount * pieceLen - completeBytes) / (double) downloadSpeed;

                    if (completedPieces == piecesCount && !isCompleted) {
                        isCompleted = true;
                        System.out.println("Mamy plik ;-)");
                        createFiles();
                    }

                    if (System.currentTimeMillis() - time > ALIVE_TIME) {
                        workingPeers.stream().forEach((peer) -> {
                            peer.keepAlive();
                        });
                        time = System.currentTimeMillis();
                    }
                }
                downloadSpeed = 0;
                remainingTime = 0;
            }
        }.start();
    }

    boolean isCompleted = false;

    public Object[] parseToTable() {
        String stat = isWorking ? "working.." : "stopped";
        return new Object[]{name, Client.bytesFormat(len), torrentComplete + "% " + stat,
            0, 0, getSHA1(), completedPieces + "/" + piecesCount};

    }

    public void addPiecesFromBitfield(byte[] bitfield, Peer p) {
        int offset = 0;
        for (byte b : bitfield) {
            for (int i = 7; i >= 0; i--, offset++) {
                if (offset == piecesCount) {
                    return;
                }

                int bit = (b >> i) & 0x01;
                if (bit == 1) {
                    addPiece(offset, p);
                    //System.out.println("Dodaje peera: " + p.getIP() + " do index: " + offset);
                }
            }
        }
    }

    public static Piece getPiece(List<Piece> pieces, int index) {
        for (Piece p : pieces) {
            if (p.getIndex() == index) {
                return p;
            }
        }
        return null;
    }

    public void addPiece(int index, Peer peer) {
        Piece piece = getPiece(pieces, index);
        if (piece == null) {
            pieces.add(new Piece(index, pieceLen, _pieces[index], peer));
        } else {
            piece.getSeeders().add(peer);
        }
    }

    public int getCompletedCount() {
        return completedPieces;
    }

    public long getTotalSize() {
        long size = 0;
        for (TorrentItem item : files) {
            size += item.getLength();
        }
        return size;
    }

    private Piece rarestPiece() {
        int min = Integer.MAX_VALUE;
        Piece best = null;
        for (Piece p : pieces) {
            if (p.getSeeders().size() < min
                    && !p.isRequested()
                    && !p.isCompleted()
                    && p.getSeeders().size() > 0) {
                min = p.getSeeders().size();
                best = p;
            }
        }
        return best;
    }

    /*Random rand = new Random();
     private Piece rarestPiece() {
        int min = Integer.MAX_VALUE;
        for (Piece p : pieces) {
            if (p.getSeeders().size() < min 
                    && !p.isRequested() 
                    && !p.isCompleted()
                    && p.getSeeders().size() > 0) {
                min = p.getSeeders().size();
            }
        }
        ArrayList<Piece> list = new ArrayList<Piece>();
        for(Piece p : pieces)
        {
            if(p.getSeeders().size() == min 
                    && !p.isRequested() 
                    && !p.isCompleted() 
                    && p.getSeeders().size() > 0)
            {
                list.add(p);
            }
        }
        if(list.isEmpty())
            return null;
        else
            return list.get(rand.nextInt(list.size()));
    }*/
    public boolean isWorking() {
        return isWorking;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + Arrays.hashCode(this.infoHash);
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
        final Torrent other = (Torrent) obj;
        if (!Arrays.equals(this.infoHash, other.infoHash)) {
            return false;
        }
        return true;
    }

    private void createFiles() {
        Collections.sort(pieces);
        if (files.size() == 1) { //jeden plik
            try {
                String path = Client.path + name;
                File f = new File(path);
                f.getParentFile().mkdirs();
                f.createNewFile();

                DataOutputStream os = new DataOutputStream(new FileOutputStream(path));
                for (Piece p : pieces) {
                    os.write(p.getData());
                }
                os.close();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Torrent.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Torrent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else
        {
            //wiele plikow
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for(Piece p : pieces)
            {
                try {
                    baos.write(p.getData());
                } catch (IOException ex) {
                    Logger.getLogger(Torrent.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            ByteArrayInputStream in = new ByteArrayInputStream(baos.toByteArray());
            for(TorrentItem item : files)
            {
                try {
                    String path = Client.path + name.replaceAll(" ", "_") + File.separator + item.toString();
                    System.out.println(path);
                    File f = new File(path);
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                    DataOutputStream os = new DataOutputStream(new FileOutputStream(path));
                    byte[] data = new byte[(int)item.getLength()];
                    in.read(data);
                    os.write(data);
                    os.close();
                } catch (IOException ex) {
                    Logger.getLogger(Torrent.class.getName()).log(Level.SEVERE, null, ex);
                } 
            }
            
        }
    }

}
