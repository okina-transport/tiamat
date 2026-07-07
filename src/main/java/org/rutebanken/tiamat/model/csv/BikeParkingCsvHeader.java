package org.rutebanken.tiamat.model.csv;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum BikeParkingCsvHeader {

    ID_LOCAL("id_local"),
    ID_OSM("id_osm"),
    CODE_COM("code_com"),
    COORDONNEESXY("coordonneesxy"),
    CAPACITE("capacite"),
    CAPACITE_CARGO("capacite_cargo"),
    TYPE_ACCROCHE("type_accroche"),
    MOBILIER("mobilier"),
    ACCES("acces"),
    GRATUIT("gratuit"),
    PROTECTION("protection"),
    COUVERTURE("couverture"),
    SURVEILLANCE("surveillance"),
    LUMIERE("lumiere"),
    URL_INFO("url_info"),
    D_SERVICE("d_service"),
    SOURCE("source"),
    PROPRIETAIRE("proprietaire"),
    GESTIONNAIRE("gestionnaire"),
    DATE_MAJ("date_maj"),
    COMMENTAIRES("commentaires"),
    NOM("nom");

    private final String value;

    BikeParkingCsvHeader(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static List<String> headerNames() {
        return Arrays.stream(values()).map(BikeParkingCsvHeader::value).collect(Collectors.toList());
    }
}