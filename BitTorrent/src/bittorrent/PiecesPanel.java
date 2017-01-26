/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public class PiecesPanel extends JPanel{
    List<Piece> pieces;
    int len;
    int width;
    private final int size = 10;
    public PiecesPanel(ArrayList<Piece> pieces, int len, int width)
    {
        this.pieces = pieces;
        this.len = len;
        this.width = width;
    }
    
    @Override
    public void paint(Graphics g)
    {
        //System.out.println("wwwwwwww");
        if(pieces == null)
            return;
        
        int x = 1;
        int y = 1;
        //najpierw all na czerwono
        for(int i=1; i<=len; i++)
        {
            drawRect(x, y, Color.RED, g);
            x += size + 2;
            if(i%width == 0)
            {
                y += size + 2;
                x = 1;
            }
        }
        
        for(Piece p : pieces)
        {
            x = 1 + (p.getIndex() % width)*(size +2);
            y = 1 + p.getIndex() / width * (size +2);
            if(p.isCompleted())
            {
                fillRect(x, y, Color.GREEN, g);
            }
            else if(!p.getSeeders().isEmpty())
            {
                //System.out.println("x:"+x+" y:"+y);
                fillRect(x, y, Color.RED, g);
            }
        }
    }
    
    public void drawRect(int x, int y, Color color, Graphics g)
    {
        g.setColor(color);
        g.drawRect(x, y, size, size);
        g.setColor(Color.WHITE);
    }
    
    public void fillRect(int x, int y, Color color, Graphics g)
    {
        g.setColor(color);
        g.fillRect(x, y, size, size);
        g.setColor(Color.WHITE);
    }
    
    public void setPieces(List<Piece> pieces, int len, int width)
    {
        //System.out.println("dziala");
        this.pieces = pieces;
        this.len = len;
        this.width = width;
    }
}
