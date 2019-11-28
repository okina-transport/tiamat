package org.rutebanken.tiamat.externalapis;

import lombok.Data;
import java.util.List;

/**
 * Objet retourné par l'api geo.gouv.fr sur une requête de reverse geocoding.
 */
@Data
public class GouvApiReverseGeocoding {

	String nom;

	String code;

	String codeDepartement;

	String codeRegion;

	List<String> codesPostaux;

	Long population;
}
