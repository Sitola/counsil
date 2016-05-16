 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package counsil;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Desanka
 */
public class CounsilTimer {
    
    public Timer timer;
    public TimerTask task;

    public CounsilTimer() {
        timer = new Timer();
        task = null;
    }
    
    
}
