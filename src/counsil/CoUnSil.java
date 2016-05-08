/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jnativehook.NativeHookException;
import org.json.JSONException;
import wddman.UnsupportedOperatingSystemException;
import wddman.WDDMan;
import wddman.WDDManException;
import wddman.Window;

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
        
       /* try {
            wddman.WDDMan wd = new WDDMan();
            List<Window> windows = wd.getWindows();
            for (Window win : windows){
                System.out.println(win.getTitle());
            }
        } catch (UnsupportedOperatingSystemException ex) {
            Logger.getLogger(CoUnSil.class.getName()).log(Level.SEVERE, null, ex);
        }
        */
        
       /*lm.addNode("Bez názvu – Poznámkový blok", "student");
        Thread.sleep(5000);
        lm.downScale("Bez názvu – Poznámkový blok");
        System.out.println("counsil.CoUnSil.main()");
         Thread.sleep(2000);
         lm.upScale("Bez názvu – Poznámkový blok");
         System.out.println("counsil.CoUnSil.main()");*/
        
        SessionManager sm = new SessionManagerImpl(lm);
        sm.initCounsil();
  }
}
