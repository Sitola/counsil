package utils;

import java.io.Serializable;

/**
 * TODO: Description
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 30.8.2007
 * Time: 17:05:07
 */
public class Shim implements Serializable {
    private int shimX;
    private int shimY;

    public Shim() {
    }

    public Shim(int shimX, int shimY) {
        this.shimX = shimX;
        this.shimY = shimY;
    }

    public int getMyShimX() {
        return shimX;
    }

    public void setShimX(int shimX) {
        this.shimX = shimX;
    }

    public int getMyShimY() {
        return shimY;
    }

    public void setShimY(int shimY) {
        this.shimY = shimY;
    }

    public boolean isShimSet() {
        return this.shimX != 0 || this.shimY != 0;
    }

    @Override
    public String toString() {
        return getMyShimX() + " " + getMyShimY();
    }
}
