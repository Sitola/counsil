package utils;

/**
 * Generator that creates shims onto a circle
 * <p/>
 * User: Petr Holub (hopet@ics.muni.cz)
 * Date: 30.8.2007
 * Time: 17:21:57
 */
public class CircleShimGenerator implements ShimGenerator {
    private final double radius;
    private final int nMembers;

    private double angle;

    public CircleShimGenerator(double radius, int nMembers) {
        this.radius = radius;
        this.nMembers = nMembers;
        angle = 20.0;
    }

    public Shim generateShim() {
        int shimX, shimY;
        assert angle < 380.0;
        shimX = (int) Math.round(radius * Math.sin(Math.toRadians(angle)));
        shimY = (int) Math.round(radius * Math.cos(Math.toRadians(angle)));
        angle += 360.0/nMembers;
        return new Shim(shimX, shimY);
    }
}
