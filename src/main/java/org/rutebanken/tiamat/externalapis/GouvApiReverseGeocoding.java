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

	public String getCodeDepartement() {
		return codeDepartement;
	}

	public void setCodeDepartement(String codeDepartement) {
		this.codeDepartement = codeDepartement;
	}

	public String getCodeRegion() {
		return codeRegion;
	}

	public void setCodeRegion(String codeRegion) {
		this.codeRegion = codeRegion;
	}

	public List<String> getCodesPostaux() {
		return codesPostaux;
	}

	public void setCodesPostaux(List<String> codesPostaux) {
		this.codesPostaux = codesPostaux;
	}

	public Long getPopulation() {
		return population;
	}

	public void setPopulation(Long population) {
		this.population = population;
	}
}
