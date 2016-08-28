package counsil;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;

/**
 * @author xdaxner
 */
public class CounsilTimer {
    
    public TimerTask stopper;
    public Timer timer;
    public TimerTask task;
    public int timesFlashed;
    public ScheduledFuture<?> future;
    
    public CounsilTimer() {
        timer = new Timer();
        task = null;
        stopper = null;
        timesFlashed = 0;
        future = null;
    }

    public void killAllTasks() {        
        timer.cancel();
        timer = new Timer();
        task = null;                
    }    
}
