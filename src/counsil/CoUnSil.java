/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import couniverse.core.Core;
import java.io.IOException;

/**
 *
 * @author xminarik
 */
public class CoUnSil {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        SessionManager sm = new SessionManagerImpl(null);
        sm.initCounsil();
    }
}
