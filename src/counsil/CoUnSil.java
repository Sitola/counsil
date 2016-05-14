/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.jnativehook.NativeHookException;
import org.json.JSONException;
import wddman.WDDManException;

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
     * @throws wddman.WDDManException
     */
    public static void main(String[] args) throws IOException, InterruptedException, JSONException, WDDManException, FileNotFoundException, NativeHookException {
        LayoutManagerImpl lm = new LayoutManagerImpl(); 
        SessionManager sm = new SessionManagerImpl(lm);
        sm.initCounsil();        
  }
}
