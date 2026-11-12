package org.rutebanken.tiamat.externalapis;

/**
 * Objet retourné par l'api geo.gouv.fr sur une requête de reverse geocoding.
 */
public class GouvApiReverseGeocoding {

    String nom;

    String code;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
