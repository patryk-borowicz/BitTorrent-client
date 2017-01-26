/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

public class RequestedBlock {
    int index;
    int begin;
    int length;

    public RequestedBlock(int index, int begin, int length) {
        this.index = index;
        this.begin = begin;
        this.length = length;
    }

    public int getIndex() {
        return index;
    }

    public int getBegin() {
        return begin;
    }

    public int getLength() {
        return length;
    }
    
}
