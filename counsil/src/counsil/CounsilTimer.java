package counsil;

import java.util.concurrent.ScheduledFuture;

/**
 * @author xdaxner
 */
public class CounsilTimer {
    
    public int timesFlashed;
    public ScheduledFuture<?> future;
    
    public CounsilTimer() {
        timesFlashed = 0;
        future = null;
    } 
}
