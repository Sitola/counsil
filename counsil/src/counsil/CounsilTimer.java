package counsil;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author xdaxner
 */
public class CounsilTimer {
    
    public TimerTask stopper;
    public Timer timer;
    public TimerTask task;
    public int timesFlashed;

    public CounsilTimer() {
        timer = new Timer();
        task = null;
        stopper = null;
        timesFlashed = 0;
    }

    public void killAllTasks() {        
        timer.cancel();
        timer = new Timer();
        task = null;                
    }    
}
