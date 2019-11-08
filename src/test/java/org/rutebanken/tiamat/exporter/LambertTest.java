package org.rutebanken.tiamat.exporter;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.rutebanken.tiamat.geo.geo.Lambert;
import org.rutebanken.tiamat.geo.geo.LambertPoint;
import org.rutebanken.tiamat.geo.geo.LambertZone;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.Assert.*;

public class LambertTest {

    @Test
    public void convertToLambertIIExtented() {
        //LambertPoint lambertPoint = Lambert.convertToLambert(2.011310f,48.803673f, LambertZone.LambertIIExtended);
        LambertPoint lambertPoint = Lambert.convertToLambert(48.803673f,2.011310f, LambertZone.LambertIIExtended);
        System.out.println(lambertPoint.getX());
        assertTrue(lambertPoint.getX() >0 );
        System.out.println(lambertPoint.getY());
        assertTrue(lambertPoint.getY() >0 );
    }

    @Test
    public void convertToLambert93() {
        //LambertPoint lambertPoint = Lambert.convertToLambert(2.011310f,48.803673f, LambertZone.LambertIIExtended);
        LambertPoint lambertPoint = Lambert.convertToLambert(48.803673f,2.011310f, LambertZone.Lambert93);
        System.out.println(lambertPoint.getX());
        assertTrue(lambertPoint.getX() >0 );
        System.out.println(lambertPoint.getY());
        assertTrue(lambertPoint.getY() >0 );
    }

    @Test
    public void latitudeISOFromLat(){
        double lat = Lambert.latitudeISOFromLat(0.87266462600, 0.08199188998);
        Assertions.assertThat(round(lat, 11) == 1.00552653649);

        double p = toDouble("-0,300 000 000 00");
        double e = toDouble("0,081 991 889 98");
        double r = toDouble("-0,302 616 900 63");
        lat = Lambert.latitudeISOFromLat(p, e);
        assertTrue(round(lat, 11) == r);

        System.out.println("fin");
    }

    @Test
    public void latitudeFromLatitudeISO() {
        // GIVEN

        // WHEN
        double l = toDouble("1,005 526 536 48");
        double e = toDouble("0,081 991 889 98");
        double c = toDouble("1.10", "11");
        double p = toDouble("0,872 664 626 00");

        // THEN
        double v = Lambert.latitudeFromLatitudeISO(l, e, c);
        assertTrue(round(v, 11) == p);
        System.out.println("fin");
    }



    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private static double toDouble(String value){
        value = value.replace(" ", "");
        value = value.replace(",", ".");
        return Double.valueOf(value).doubleValue();
    }
    private static double toDouble(String value, String pow){
        value = value.replace(" ", "");
        value = value.replace(",", ".");
        return Math.pow(Double.valueOf(value).doubleValue(), Double.valueOf(pow).doubleValue());
    }
}