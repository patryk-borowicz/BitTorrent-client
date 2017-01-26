/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.util.HashMap;

public class ClientIdentifier {

    private static HashMap<String, String> names = new HashMap<String, String>();

    //////ladowanie slownika nazw
    private static void load() {

        names.put("7T", "aTorrentforAndroid");
        names.put("AB", "AnyEvent::BitTorrent");
        names.put("AG", "Ares");
        names.put("A~", "Ares");
        names.put("AR", "Arctic");
        names.put("AV", "Avicora");
        names.put("AT", "Artemis");
        names.put("AX", "BitPump");
        names.put("AZ", "Azureus");
        names.put("BB", "BitBuddy");
        names.put("BC", "BitComet");
        names.put("BE", "Baretorrent");
        names.put("BF", "Bitflu");
        names.put("BG", "BTG");
        names.put("BL", "BitCometLite");
        names.put("BP", "BitTorrentPro");
        names.put("BR", "BitRocket");
        names.put("BS", "BTSlave");
        names.put("BT", "BitTorrent");
        names.put("Bt", "Bt");
        names.put("BW", "BitWombat");
        names.put("BX", "~BittorrentX");
        names.put("CD", "EnhancedCTorrent");
        names.put("CT", "CTorrent");
        names.put("DE", "DelugeTorrent");
        names.put("DP", "PropagateDataClient");
        names.put("EB", "EBit");
        names.put("ES", "electricsheep");
        names.put("FC", "FileCroc");
        names.put("FD", "FreeDownloadManager");
        names.put("FT", "FoxTorrent");
        names.put("FX", "FreeboxBitTorrent");
        names.put("GS", "GSTorrent");
        names.put("HK", "Hekate");
        names.put("HL", "Halite");
        names.put("HM", "hMule");
        names.put("HN", "Hydranode");
        names.put("IL", "iLivid");
        names.put("JS", "Justseed.itclient");
        names.put("JT", "JavaTorrent");
        names.put("KG", "KGet");
        names.put("KT", "KTorrent");
        names.put("LC", "LeechCraft");
        names.put("LH", "LH-ABC");
        names.put("LP", "Lphant");
        names.put("LT", "libtorrent");
        names.put("lt", "libTorrent");
        names.put("LW", "LimeWire");
        names.put("MK", "Meerkat");
        names.put("MO", "MonoTorrent");
        names.put("MP", "MooPolice");
        names.put("MR", "Miro");
        names.put("MT", "MoonlightTorrent");
        names.put("NB", "Net::BitTorrent");
        names.put("NX", "NetTransport");
        names.put("OS", "OneSwarm");
        names.put("OT", "OmegaTorrent");
        names.put("PB", "Protocol::BitTorrent");
        names.put("PD", "Pando");
        names.put("PI", "PicoTorrent");
        names.put("PT", "PHPTracker");
        names.put("qB", "qBittorrent");
        names.put("QD", "QQDownload");
        names.put("QT", "Qt4Torrentexample");
        names.put("RT", "Retriever");
        names.put("RZ", "RezTorrent");
        names.put("S~", "Shareazaalpha/beta");
        names.put("SB", "~Swiftbit");
        names.put("SD", "Thunder");
        names.put("SM", "SoMud");
        names.put("SP", "BitSpirit");
        names.put("SS", "SwarmScope");
        names.put("ST", "SymTorrent");
        names.put("st", "sharktorrent");
        names.put("SZ", "Shareaza");
        names.put("TB", "Torch");
        names.put("TE", "terasaurSeedBank");
        names.put("TL", "Tribler");
        names.put("TN", "TorrentDotNET");
        names.put("TR", "Transmission");
        names.put("TS", "Torrentstorm");
        names.put("TT", "TuoTu");
        names.put("UL", "uLeecher!");
        names.put("UM", "uTorrentforMac");
        names.put("UT", "uTorrent");
        names.put("VG", "Vagaa");
        names.put("WD", "WebTorrentDesktop");
        names.put("WT", "BitLet");
        names.put("WW", "WebTorrent");
        names.put("WY", "FireTorrent");
        names.put("XF", "Xfplay");
        names.put("XL", "Xunlei");
        names.put("XS", "XSwifter");
        names.put("XT", "XanTorrent");
        names.put("XX", "Xtorrent");
        names.put("ZT", "ZipTorrent");

    }

    public static String getName(byte[] id) {
        if (names.size() == 0) {
            load();
        }

        String _id = new String(id).substring(1, 3);
        return names.containsKey(_id) ? names.get(_id) : "-";
    }
}
