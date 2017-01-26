/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JFrame;

public class BitfieldTest extends JFrame{
    byte[] data;
    int len;
    BitfieldTest(String title, byte[] bitfield, int len){
        this.len = len;
        this.data = bitfield;
        setSize(len + 30, 100);
        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    @Override
    public void paint(Graphics g) 
    {
        int offset = 1;
        for(byte b : data)
        {
            for(int i=7; i>0; i--, offset++)
            {
                int bit = (b >> i) & 1;
                g.setColor(bit == 1 ? Color.GREEN : Color.red);
                g.drawRect(30, 40, offset, 30);
                if(offset > len)
                    return;
            }
        }
         
    }
}
