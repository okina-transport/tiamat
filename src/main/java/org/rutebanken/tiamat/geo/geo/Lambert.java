package org.rutebanken.tiamat.geo.geo;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class Lambert {
    public Lambert() {
    }
    public static double latitudeISOFromLat(double lat, double e) {
        double elt11 = 0.7853981633974483D;
        double elt12 = lat / 2.0D;
        double elt1 = Math.tan(elt11 + elt12);
        double elt21 = e * Math.sin(lat);
        double elt2 = Math.pow((1.0D - elt21) / (1.0D + elt21), e / 2.0D);
        return Math.log(elt1 * elt2);
    }
    private static double latitudeFromLatitudeISO(double latISo, double e, double eps) {
        double phi0 = 2.0D * Math.atan(Math.exp(latISo)) - 1.5707963267948966D;
        double phiI = 2.0D * Math.atan(Math.pow((1.0D + e * Math.sin(phi0)) / (1.0D - e * Math.sin(phi0)), e / 2.0D) * Math.exp(latISo)) - 1.5707963267948966D;
        for(double delta = Math.abs(phiI - phi0); delta > eps; delta = Math.abs(phiI - phi0)) {
            phi0 = phiI;
            phiI = 2.0D * Math.atan(Math.pow((1.0D + e * Math.sin(phiI)) / (1.0D - e * Math.sin(phiI)), e / 2.0D) * Math.exp(latISo)) - 1.5707963267948966D;
        }
        return phiI;
    }
    public static LambertPoint geographicToLambertAlg003(double latitude, double longitude, LambertZone zone, double lonMeridian, double e) {
        double n = zone.n();
        double C = zone.c();
        double xs = zone.xs();
        double ys = zone.ys();
        double latIso = latitudeISOFromLat(latitude, e);
        double eLatIso = Math.exp(-n * latIso);
        double nLon = n * (longitude - lonMeridian);
        double x = xs + C * eLatIso * Math.sin(nLon);
        double y = ys - C * eLatIso * Math.cos(nLon);
        return new LambertPoint(x, y, 0.0D);
    }
    public static LambertPoint geographicToLambert(double latitude, double longitude, LambertZone zone, double lonMeridian, double e) {
        double n = zone.n();
        double C = zone.c();
        double xs = zone.xs();
        double ys = zone.ys();
        double sinLat = Math.sin(latitude);
        double eSinLat = e * sinLat;
        double elt1 = (1.0D + sinLat) / (1.0D - sinLat);
        double elt2 = (1.0D + eSinLat) / (1.0D - eSinLat);
        double latIso = 0.5D * Math.log(elt1) - e / 2.0D * Math.log(elt2);
        double R = C * Math.exp(-(n * latIso));
        double LAMBDA = n * (longitude - lonMeridian);
        double x = xs + R * Math.sin(LAMBDA);
        double y = ys - R * Math.cos(LAMBDA);
        return new LambertPoint(x, y, 0.0D);
    }
    public static LambertPoint lambertToGeographic(LambertPoint org, LambertZone zone, double lonMeridian, double e, double eps) {
        double n = zone.n();
        double C = zone.c();
        double xs = zone.xs();
        double ys = zone.ys();
        double x = org.getX();
        double y = org.getY();
        double R = Math.sqrt((x - xs) * (x - xs) + (y - ys) * (y - ys));
        double gamma = Math.atan((x - xs) / (ys - y));
        double lon = lonMeridian + gamma / n;
        double latIso = -1.0D / n * Math.log(Math.abs(R / C));
        double lat = latitudeFromLatitudeISO(latIso, e, eps);
        return new LambertPoint(lon, lat, 0.0D);
    }
    private static double lambertNormal(double lat, double a, double e) {
        return a / Math.sqrt(1.0D - e * e * Math.sin(lat) * Math.sin(lat));
    }
    private static LambertPoint geographicToCartesian(double lon, double lat, double he, double a, double e) {
        double N = lambertNormal(lat, a, e);
        LambertPoint pt = new LambertPoint(0.0D, 0.0D, 0.0D);
        pt.setX((N + he) * Math.cos(lat) * Math.cos(lon));
        pt.setY((N + he) * Math.cos(lat) * Math.sin(lon));
        pt.setZ((N * (1.0D - e * e) + he) * Math.sin(lat));
        return pt;
    }
    private static LambertPoint cartesianToGeographic(LambertPoint org, double meridien, double a, double e, double eps) {
        double x = org.getX();
        double y = org.getY();
        double z = org.getZ();
        double lon = meridien + Math.atan(y / x);
        double module = Math.sqrt(x * x + y * y);
        double phi0 = Math.atan(z / (module * (1.0D - a * e * e / Math.sqrt(x * x + y * y + z * z))));
        double phiI = Math.atan(z / module / (1.0D - a * e * e * Math.cos(phi0) / (module * Math.sqrt(1.0D - e * e * Math.sin(phi0) * Math.sin(phi0)))));
        for(double delta = Math.abs(phiI - phi0); delta > eps; delta = Math.abs(phiI - phi0)) {
            phi0 = phiI;
            phiI = Math.atan(z / module / (1.0D - a * e * e * Math.cos(phiI) / (module * Math.sqrt(1.0D - e * e * Math.sin(phiI) * Math.sin(phiI)))));
        }
        double he = module / Math.cos(phiI) - a / Math.sqrt(1.0D - e * e * Math.sin(phiI) * Math.sin(phiI));
        return new LambertPoint(lon, phiI, he);
    }
    public static LambertPoint convertToWGS84(LambertPoint org, LambertZone zone) {
        if(zone == LambertZone.Lambert93) {
            return lambertToGeographic(org, LambertZone.Lambert93, 0.05235987755982988D, 0.08181919106D, 1.0E-10D);
        } else {
            LambertPoint pt1 = lambertToGeographic(org, zone, 0.0D, 0.08248325676D, 1.0E-10D);
            LambertPoint pt2 = geographicToCartesian(pt1.getX(), pt1.getY(), pt1.getZ(), 6378249.2D, 0.08248325676D);
            pt2.translate(-168.0D, -60.0D, 320.0D);
            return cartesianToGeographic(pt2, 0.04079234433D, 6378137.0D, 0.08181919106D, 1.0E-10D);
        }
    }
    public static LambertPoint convertToLambert(double latitude, double longitude, LambertZone zone) throws NotImplementedException {
        if(zone == LambertZone.Lambert93) {
            throw new NotImplementedException();
        } else {
            LambertPoint pt1 = geographicToCartesian(longitude - 0.04079234433D, latitude, 0.0D, 6378137.0D, 0.08181919106D);
            pt1.translate(168.0D, 60.0D, -320.0D);
            LambertPoint pt2 = cartesianToGeographic(pt1, 0.0D, 6378137.0D, 0.08181919106D, 1.0E-10D);
            return geographicToLambert(pt2.getY(), pt2.getX(), zone, 0.0D, 0.08181919106D);
        }
    }
    public static LambertPoint convertToLambertByAlg003(double latitude, double longitude, LambertZone zone) throws NotImplementedException {
        if(zone == LambertZone.Lambert93) {
            throw new NotImplementedException();
        } else {
            LambertPoint pt1 = geographicToCartesian(longitude - 0.04079234433D, latitude, 0.0D, 6378137.0D, 0.08181919106D);
            pt1.translate(168.0D, 60.0D, -320.0D);
            LambertPoint pt2 = cartesianToGeographic(pt1, 0.0D, 6378137.0D, 0.08181919106D, 1.0E-10D);
            return geographicToLambertAlg003(pt2.getY(), pt2.getX(), zone, 0.0D, 0.08181919106D);
        }
    }
    public static LambertPoint convertToWGS84(double x, double y, LambertZone zone) {
        LambertPoint pt = new LambertPoint(x, y, 0.0D);
        return convertToWGS84(pt, zone);
    }
    public static LambertPoint convertToWGS84Deg(double x, double y, LambertZone zone) {
        LambertPoint pt = new LambertPoint(x, y, 0.0D);
        return convertToWGS84(pt, zone).toDegree();
    }
}