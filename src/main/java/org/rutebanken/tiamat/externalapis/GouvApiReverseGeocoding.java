package org.rutebanken.tiamat.externalapis;


import java.util.List;

/**
 * Objet retourné par l'api geo.gouv.fr sur une requête de reverse geocoding.
 */

public class GouvApiReverseGeocoding {

	String nom;

	String code;

	String codeDepartement;

	String codeRegion;

	List<String> codesPostaux;

	Long population;
}
