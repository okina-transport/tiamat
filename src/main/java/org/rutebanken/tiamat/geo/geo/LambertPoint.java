package org.rutebanken.tiamat.geo.geo;

public class LambertPoint {
    private double x;
    private double y;
    private double z;
    LambertPoint(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public double getX() {
        return this.x;
    }
    public void setX(double x) {
        this.x = x;
    }
    public double getY() {
        return this.y;
    }
    public void setY(double y) {
        this.y = y;
    }
    public double getZ() {
        return this.z;
    }
    public void setZ(double z) {
        this.z = z;
    }
    public void translate(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
    }
    public LambertPoint toDegree() {
        this.x = this.x * 180.0D / 3.141592653589793D;
        this.y = this.y * 180.0D / 3.141592653589793D;
        this.z = this.z * 180.0D / 3.141592653589793D;
        return this;
    }
}