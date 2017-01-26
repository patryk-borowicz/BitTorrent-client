/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

public abstract class TrackerBase {
    protected int interval = 0;
    protected int leechers = 0;
    protected int seeders = 0;
    protected int downloaded = 0;
    protected int complete = 0;
    protected int incomplete = 0;
    protected int peerCount = 0;
    protected String fullName = "";
    
    public int getInterval() {
        return interval;
    }

    public int getLeechers() {
        return leechers;
    }

    public int getSeeders() {
        return seeders;
    }

    public int getDownloaded() {
        return downloaded;
    }

    public int getComplete() {
        return complete;
    }

    public int getIncomplete() {
        return incomplete;
    }

    public int getPeerCount() {
        return peerCount;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public Object[] toRow()
    {
        return new Object[] {fullName, getStatus(), interval, incomplete, downloaded, complete, seeders, leechers, peerCount };
    }
    
    public abstract String getStatus();
    
}
