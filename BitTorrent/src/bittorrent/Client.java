/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class Client {
    public static String path = "C:" + File.separator + "torrent" + File.separator;

    public static final String CLIENT_ID = "ABCDEFGHIJKLMNOPQRST";

    public enum LOAD_RESULT {
        OK, FAIL, EXIST
    };
    private volatile ArrayList<Torrent> torrents = new ArrayList<Torrent>();
    private ShareService shareService;

    public Client() {
        shareService = new ShareService();
        shareService.run(torrents);
    }

    public LOAD_RESULT loadTorrent(String path, DefaultTableModel model) {
        //zaladowanie torrenta z pliku do pamieci
        //sprawdzenie czy podany torrent istnieje na liscie w programie
        //TODO utworzenie katalogu z plikami torrenta
        Torrent torrent = Torrent.readFromFile(path);

        if (torrent != null) {
            for (Torrent t : torrents) {
                if (t.equals(torrent)) {
                    return LOAD_RESULT.EXIST;
                }
            }
            torrents.add(torrent);
            torrent.start();
            model.addRow(torrent.parseToTable());
            return LOAD_RESULT.OK;
        }
        return LOAD_RESULT.FAIL;
    }

    public void refreshTorrents(JTable table) {
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    for (int i = 0; i < table.getRowCount(); i++) {
                        Torrent t = getTorrentFromSHA1((String) table.getValueAt(i, 5));
                        if (t != null) {
                            table.setValueAt(t.getCompletedCount() * 100 / t.getPiecesCount() + "% " + t.getStatus(), i, 2);
                            table.setValueAt(bytesFormat(t.getCompleteBytes()), i, 3);
                            table.setValueAt(bytesFormat(t.getUploadTotal()), i, 4);
                            table.setValueAt(t.getCompletedCount() + "/" + t.getPiecesCount(), i, 6);
                            table.setValueAt(t.isCompleted ? "-" : t.isWorking() ? timeFormat((long) t.getRemainingTime()) : "-", i, 7);
                            table.setValueAt(t.isCompleted ? "-" : t.isWorking() ? bpsFormat(t.getDownloadSpeed()) : "-", i, 8);
                            table.setValueAt("-", i, 9);
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }.start();
    }

    public void refreshButtons(JTable table, JButton start, JButton stop, JProgressBar bar) {
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    if (table.getSelectedRow() > -1) {
                        String sha1 = table.getValueAt(table.getSelectedRow(), 5).toString();
                        Torrent t = getTorrentFromSHA1(sha1);
                        if(t!=null)
                            bar.setValue(t.getCompletedCount()*100/t.getPiecesCount());
                        if (t == null) {
                            start.setEnabled(false);
                            stop.setEnabled(false);
                        } else if (t.isWorking()) {
                            start.setEnabled(false);
                            stop.setEnabled(true);
                        } else {
                            start.setEnabled(true);
                            stop.setEnabled(false);
                        }
                    } else {
                        start.setEnabled(false);
                        stop.setEnabled(false);
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }.start();
    }

    public void loadFilesTable(String sha1, DefaultTableModel model) {
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }
        Torrent t = getTorrentFromSHA1(sha1);
        if (t != null) {
            for (TorrentItem f : t.getFiles()) {
                model.addRow(f.toRow(t.getPieces(), t.getPiecesCount(), t.getPieceLength(), t.getTotalSize()));
            }

        }
    }

    public void loadPeersTrackerTable(String sha1, DefaultTableModel model,
            DefaultTableModel trackerModel, PiecesPanel panel, int width) {
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }
        while (trackerModel.getRowCount() > 0) {
            trackerModel.removeRow(0);
        }
        //tu dodac czyszczenie zakladki pieces
        try {
        Torrent t = getTorrentFromSHA1(sha1);
        if (t != null) {
            for (Peer peer : t.getWorkingPeers()) {
                if (!peer.isConnected() || !peer.isAuthenticated()) {
                    continue;
                }
                int pieces = peer.getPieces();
                int tmp = (pieces * 100) / t.getPiecesCount();
                model.addRow(new Object[]{peer.getIP(),
                    peer.getClientName(),
                    bytesFormat(peer.getDownloadTotal()),
                    bytesFormat(peer.getUploadTotal()),
                    pieces + "(" + tmp + "%)",
                    peer.getStatus(),
                    peer.getPort()});
            }

            for (TrackerBase tracker : t.getTrackers()) {
                trackerModel.addRow(tracker.toRow());
            }
            panel.setPieces(t.getPieces(), t.getPiecesCount(), width / 14);
            panel.revalidate();
            panel.repaint();
        }
        } catch(Exception e)
        {
            System.err.println("GUI ;-(");
        }
    }

    public Torrent getTorrentFromSHA1(String SHA1) {
        for (Torrent t : torrents) {
            if (t.getSHA1().equals(SHA1)) {
                return t;
            }
        }
        return null;
    }

    public static String bytesFormat(long bytes) {
        if (bytes >= 1000000000) {
            return bytes / 1000000000 + " GB";
        } else if (bytes >= 1000000) {
            return bytes / 1000000 + " MB";
        } else if (bytes >= 1000) {
            return bytes / 1000 + " kB";
        } else {
            return bytes + " bytes";
        }
    }

    public static String bpsFormat(double bytes) {
        if (bytes == 0) {
            return "-";
        }

        if (bytes >= 1000000000) {
            return String.format("%.2f GB/s", bytes / 1000000000.0);
        } else if (bytes >= 1000000) {
            return String.format("%.2f MB/s", bytes / 1000000.0);
        } else if (bytes >= 1000) {
            return String.format("%.2f kB/s", bytes / 1000.0);
        } else {
            return String.format("%.2f bytes/s", bytes);
        }
    }

    public static String timeFormat(long timeInSeconds) {
        if (timeInSeconds == 0) {
            return "-";
        }

        long hours = timeInSeconds / (60 * 60);
        long minutes = (timeInSeconds / 60) % 60;
        long seconds = timeInSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void stopTorrent(JTable table) {
        if (table.getSelectedRow() > -1) {
            String sha1 = table.getValueAt(table.getSelectedRow(), 5).toString();
            Torrent t = getTorrentFromSHA1(sha1);
            if (t != null) {
                t.stop();
            }
        }
    }

    public void startTorrent(JTable table) {
        if (table.getSelectedRow() > -1) {
            String sha1 = table.getValueAt(table.getSelectedRow(), 5).toString();
            Torrent t = getTorrentFromSHA1(sha1);
            if (t != null) {
                t.start();
            }
        }
    }
    
    public void openExplorer()
    {
        try {
            new File(path).mkdirs();
            Desktop.getDesktop().open(new File(path));
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
