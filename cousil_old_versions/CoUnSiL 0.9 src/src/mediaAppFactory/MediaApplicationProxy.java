package mediaAppFactory;

/**
 * Interface for remote applications
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 10.4.2009
 * Time: 16:31:21
 */
public interface MediaApplicationProxy {

    public void setStartCommand(String command);
    public String getStartCommand();
    public void setStopCommand(String command);
    public String getStopCommand();

}
