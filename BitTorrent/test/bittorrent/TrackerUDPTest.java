/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bittorrent;

import java.io.IOException;
import java.net.SocketException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TrackerUDPTest {
    
    public TrackerUDPTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testSomeMethod() throws SocketException, IOException {
        // TODO review the generated test code and remove the default call to fail.
        Torrent t = Torrent.readFromFile("C:\\terraria.torrent");
        TrackerUDP__.trackerUDPService(t);
        while(true)
            ;
    }
    
}
