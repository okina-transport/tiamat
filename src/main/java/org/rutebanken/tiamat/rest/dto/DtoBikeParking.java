package org.rutebanken.tiamat.rest.dto;

import com.google.common.base.MoreObjects;


public class DtoBikeParking {

    private String idLocal;
    private String idOsm;
    private String codeCom;
    private String Xlong;
    private String Ylat;
    private String capacite;
    private String capaciteCargo;
    private String typeAccroche;
    private String mobilier;
    private String acces;
    private String gratuit;
    private String protection;
    private String couverture;
    private String surveillance;
    private String lumiere;
    private String urlInfo;
    private String dService;
    private String source;
    private String proprietaire;
    private String gestionnaire;
    private String dateMaj;
    private String commentaires;

    public String getIdLocal() {
        return idLocal;
    }

    public void setIdLocal(String idLocal) {
        this.idLocal = idLocal;
    }

    public String getIdOsm() {
        return idOsm;
    }

    public void setIdOsm(String idOsm) {
        this.idOsm = idOsm;
    }

    public String getCodeCom() {
        return codeCom;
    }

    public void setCodeCom(String codeCom) {
        this.codeCom = codeCom;
    }

    public String getXlong() {
        return Xlong;
    }

    public String getYlat() {
        return Ylat;
    }

    public void setXlong(String xlong) {
        Xlong = xlong;
    }

    public void setYlat(String ylat) {
        Ylat = ylat;
    }

    public String getCapacite() {
        return capacite;
    }

    public void setCapacite(String capacite) {
        this.capacite = capacite;
    }

    public String getCapaciteCargo() {
        return capaciteCargo;
    }

    public void setCapaciteCargo(String capaciteCargo) {
        this.capaciteCargo = capaciteCargo;
    }

    public String getTypeAccroche() {
        return typeAccroche;
    }

    public void setTypeAccroche(String typeAccroche) {
        this.typeAccroche = typeAccroche;
    }

    public String getMobilier() {
        return mobilier;
    }

    public void setMobilier(String mobilier) {
        this.mobilier = mobilier;
    }

    public String getAcces() {
        return acces;
    }

    public void setAcces(String acces) {
        this.acces = acces;
    }

    public String getGratuit() {
        return gratuit;
    }

    public void setGratuit(String gratuit) {
        this.gratuit = gratuit;
    }

    public String getProtection() {
        return protection;
    }

    public void setProtection(String protection) {
        this.protection = protection;
    }

    public String getCouverture() {
        return couverture;
    }

    public void setCouverture(String couverture) {
        this.couverture = couverture;
    }

    public String getSurveillance() {
        return surveillance;
    }

    public void setSurveillance(String surveillance) {
        this.surveillance = surveillance;
    }

    public String getLumiere() {
        return lumiere;
    }

    public void setLumiere(String lumiere) {
        this.lumiere = lumiere;
    }

    public String getUrlInfo() {
        return urlInfo;
    }

    public void setUrlInfo(String urlInfo) {
        this.urlInfo = urlInfo;
    }

    public String getdService() {
        return dService;
    }

    public void setdService(String dService) {
        this.dService = dService;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getProprietaire() {
        return proprietaire;
    }

    public void setProprietaire(String proprietaire) {
        this.proprietaire = proprietaire;
    }

    public String getGestionnaire() {
        return gestionnaire;
    }

    public void setGestionnaire(String gestionnaire) {
        this.gestionnaire = gestionnaire;
    }

    public String getDateMaj() {
        return dateMaj;
    }

    public void setDateMaj(String dateMaj) {
        this.dateMaj = dateMaj;
    }

    public String getCommentaires() {
        return commentaires;
    }

    public void setCommentaires(String commentaires) {
        this.commentaires = commentaires;
    }

    public DtoBikeParking() {
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id_local", idLocal)
                .add("id_osm", idOsm)
                .add("code_com", codeCom)
                .add("Xlong", Xlong)
                .add("Ylat", Ylat)
                .add("capacite", capacite)
                .add("capacite_cargo", capaciteCargo)
                .add("type_accroche", typeAccroche)
                .add("mobilier", mobilier)
                .add("acces", acces)
                .add("gratuit", gratuit)
                .add("protection", protection)
                .add("couverture", couverture)
                .add("surveillance", surveillance)
                .add("lumiere", lumiere)
                .add("url_info", urlInfo)
                .add("d_service", dService)
                .add("source", source)
                .add("proprietaire", proprietaire)
                .add("gestionnaire", gestionnaire)
                .add("date_maj", dateMaj)
                .add("commentaires", commentaires)
                .toString();
    }
}
