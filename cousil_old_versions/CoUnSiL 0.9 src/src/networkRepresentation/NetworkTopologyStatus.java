package networkRepresentation;

/**
 * This is a representation of status of NetworkTopology - if it was changed and when.
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 9.9.2007
 * Time: 20:24:27
 */
public class NetworkTopologyStatus {
    private boolean changed;
    private long changedStamp;
    private long changesCount;

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public long getChangedStamp() {
        return changedStamp;
    }

    public void setChangedStamp(long changedStamp) {
        this.changedStamp = changedStamp;
    }

    public long getChangesCount() {
        return changesCount;
    }

    public void setChangesCount(long changesCount) {
        this.changesCount = changesCount;
    }
}
