/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.io.IOException;

/**
 *
 * @author xminarik
 */
public interface SessionManager {

    /**
     * Stats Couniverse core and counsil components
     * 
     * @throws IOException if it's not possible to start Couniverse core or producer on local node
     * @throws InterruptedException if it's not possible to start Couniverse core 
     */
    public void initCounsil() throws IOException, InterruptedException;
}
