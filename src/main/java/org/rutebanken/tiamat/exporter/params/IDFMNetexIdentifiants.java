package org.rutebanken.tiamat.exporter.params;

import java.util.HashMap;
import java.util.Map;

public class IDFMNetexIdentifiants {
    private static Map<String, String> idSitesList = new HashMap<>();
    private static Map<String, String> nameSitesList = new HashMap<>();

    static {
        idSitesList.put("CTVMI", "81");
        idSitesList.put("CEOBUS", "84");
        idSitesList.put("MOBICITE", "79");
        idSitesList.put("PERRIER", "42");
        idSitesList.put("SQYBUS", "41");
        idSitesList.put("STILE", "89");
        idSitesList.put("TIMBUS", "87");
        idSitesList.put("TVM", "86");
        idSitesList.put("TEST", "12");

        nameSitesList.put("CTVMI", "CTVMI");
        nameSitesList.put("CEOBUS", "GIRAUX VAL D'OISE");
        nameSitesList.put("MOBICITE", "MOBICITE");
        nameSitesList.put("PERRIER", "PERRIER");
        nameSitesList.put("SQYBUS", "SQYBUS");
        nameSitesList.put("STILE", "STILE");
        nameSitesList.put("TIMBUS", "TIM BUS");
        nameSitesList.put("TVM", "TVM");
        nameSitesList.put("TEST", "TEST");
    }

    public static String getIdSite(String name){
        return idSitesList.get(name);
    }

    public static String getNameSite(String name){
        return nameSitesList.get(name);
    }

}
