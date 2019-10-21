package org.rutebanken.tiamat.geo.geo;

public enum LambertZone {
    LambertI(0),
    LambertII(1),
    LambertIII(2),
    LambertIV(3),
    LambertIIExtended(4),
    Lambert93(5);
    private final int lambertZone;
    private static final double[] LAMBERT_N = new double[]{0.7604059656D, 0.7289686274D, 0.6959127966D, 0.6712679322D, 0.7289686274D, 0.725607765D};
    private static final double[] LAMBERT_C = new double[]{1.160379698E7D, 1.174579339E7D, 1.194799252E7D, 1.213628199E7D, 1.174579339E7D, 1.1754255426E7D};
    private static final double[] LAMBERT_XS = new double[]{600000.0D, 600000.0D, 600000.0D, 234.358D, 600000.0D, 700000.0D};
    private static final double[] LAMBERT_YS = new double[]{5657616.674D, 6199695.768D, 6791905.085D, 7239161.542D, 8199695.768D, 1.265561205E7D};
    public static final double M_PI_2 = 1.5707963267948966D;
    public static final double DEFAULT_EPS = 1.0E-10D;
    public static final double E_CLARK_IGN = 0.08248325676D;
    public static final double E_WGS84 = 0.08181919106D;
    public static final double A_CLARK_IGN = 6378249.2D;
    public static final double A_WGS84 = 6378137.0D;
    public static final double LON_MERID_PARIS = 0.0D;
    public static final double LON_MERID_GREENWICH = 0.04079234433D;
    public static final double LON_MERID_IERS = 0.05235987755982988D;
    private LambertZone(int value) {
        this.lambertZone = value;
    }
    public double n() {
        return LAMBERT_N[this.lambertZone];
    }
    public double c() {
        return LAMBERT_C[this.lambertZone];
    }
    public double xs() {
        return LAMBERT_XS[this.lambertZone];
    }
    public double ys() {
        return LAMBERT_YS[this.lambertZone];
    }
}