/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import couniverse.core.Core;
import java.io.IOException;
import org.json.JSONException;

/**
 *
 * @author xminarik
 */
public class CoUnSil {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     * @throws org.json.JSONException
     */
    public static void main(String[] args) throws IOException, InterruptedException, JSONException {
        LayoutManager lm = new LayoutManagerImpl();
        SessionManager sm = new SessionManagerImpl(lm);
        sm.initCounsil();
    }
}
