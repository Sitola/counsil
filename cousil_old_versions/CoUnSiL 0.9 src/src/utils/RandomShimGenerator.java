package utils;

import org.apache.log4j.Logger;

import java.util.ArrayList;

/**
 * Auxilliary class for shim generation
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 30.8.2007
 * Time: 14:35:22
 */
public class RandomShimGenerator implements ShimGenerator {
    static Logger logger = Logger.getLogger("utils");

    public static double shimMin = -30.0;
    public static double shimMax = 30.0;
    public static int shimCount = 100;
    private static double ratio = (shimMax - shimMin) / shimCount;
    private static ArrayList<Double> usedShims = new ArrayList<Double>();
    private final double shimRange;

    private static double getRandomShim() {
        Double shim;
        if (usedShims.size() == shimCount) {
            usedShims.clear();
            RandomShimGenerator.logger.warn("Exceeded shim capacity, restarting generator.");
        }
        int i = 0;
        do {
            i++;
            //noinspection UnsecureRandomNumberGeneration
            shim = (Math.random() - 0.5) * (shimMax - shimMin);
            // now we will do the rounding for comparison
            shim = ((double) Math.round(shim * ratio)) / ratio;
        } while (usedShims.contains(shim) && i < 2 * shimCount);
        //  || ( usedShims.size() > 1 && i < shimCount && (shim - usedShims.get(usedShims.size() - 1)) * ratio < 1)
        usedShims.add(shim);
        return shim;
    }

    public static void restartRandomShimGenerator() {
        usedShims.clear();
    }


    public RandomShimGenerator() {
        this.shimRange = 50.0;
    }

    public RandomShimGenerator(double shimRange) {
        this.shimRange = shimRange;
    }

    public Shim generateShim() {
        Shim shim = new Shim();
        double shimX, shimY;
        do {
            shimX = getRandomShim();
            shimY = getRandomShim();
        } while (Math.sqrt(shimX * shimX + shimY * shimY) < 0.5 * shimRange);
        shim.setShimX((int) Math.round(shimX));
        shim.setShimX((int) Math.round(shimY));
        return shim;
    }
}
