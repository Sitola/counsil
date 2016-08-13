package counsil;

/**
 * Represents structure which manipulates with layout
 * @author xdaxner
 */
public interface LayoutManager {

    /**
     * Adds new node window to layout
     * @param title window title
     * @param role tole of window user
     */
    public void addNode(String title, String role);

    /**
     * Removes window from layout
     * @param title window title
     */
    public void removeNode(String title);

    /**
     * Adds listener of layout manager events
     * @param listener
     */
    public void addLayoutManagerListener(LayoutManagerListener listener);

    /**
     * Scales window size down
     * @param name
     */
    public void downScale(String name);

    /**
     * Scales window size up
     * @param name
     */
    public void upScale(String name);

    /**
     * Refreshes layout to default positions
     */
    public void refreshLayout();
}
