package utils;

/**
 * Created by IntelliJ IDEA.
 * User: xliska
 * Date: 17.10.2007
 * Time: 14:46:14
 */
public class TimeUtils {

    /**
     * Sleeps for specified time.
     * <p/>
     *
     * @param time specified in milliseconds
     */
    public static void sleepFor(long time) {
        //noinspection EmptyCatchBlock
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }
}
