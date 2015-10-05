package utils;

/**
 * Auxilliary converters.
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 10.9.2007
 * Time: 12:24:24
 */
public class Converters {
    /**
     * Returns maximum total bandwidth used by this MediaApplication as a pretty-typed string. Iterates
     * through media steams and counts up maximum bandwidths together.
     * <p/>
     * @param bw bandwidth in bps
     * @return maximum bandwidth as pretty-typed string
     */
    public static String bandwidthToString(long bw) {
        StringBuffer bwStringBuffer = new StringBuffer();
        Double bwToString;
        if (bw > 1000000000) {
            bwToString = Math.round((double) bw / 1000000000.0 * 10.0) / 10.0;
            bwStringBuffer.append(bwToString.toString() + "Gbps");
        } else if (bw > 1000000) {
            bwToString = Math.round((double) bw / 1000000.0 * 10.0) / 10.0;
            bwStringBuffer.append(bwToString.toString() + "Mbps");
        } else if (bw > 1000) {
            bwToString = Math.round((double) bw / 1000.0 * 10.0) / 10.0;
            bwStringBuffer.append(bwToString.toString() + "kbps");
        } else {
            bwToString = Math.round((double) bw * 10.0) / 10.0;
            bwStringBuffer.append(bwToString.toString() + "bps");
        }
        return bwStringBuffer.toString();
    }
}
