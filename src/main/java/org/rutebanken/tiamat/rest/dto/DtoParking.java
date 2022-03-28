package org.rutebanken.tiamat.rest.dto;

import com.google.common.base.MoreObjects;

public class DtoParking {

    private String id;
    private String nom;
    private String insee;
    private String adresse;
    private String url;
    private String type_usagers;
    private String gratuit;
    private String nb_places;
    private String nb_pr;
    private String nb_pmr;
    private String nb_voitures_electriques;
    private String nb_velo;
    private String nb_2r_el;
    private String nb_autopartage;
    private String nb_2_rm;
    private String nb_covoit;
    private String hauteur_max;
    private String num_siret;
    private String Xlong;
    private String Ylat;
    private String tarif_pmr;
    private String tarif_1h;
    private String tarif_2h;
    private String tarif_3h;
    private String tarif_4h;
    private String tarif_24h;
    private String abo_resident;
    private String abo_non_resident;
    private String type_ouvrage;
    private String info;

    public DtoParking(String id, String nom, String insee, String adresse, String url, String type_usagers, String gratuit, String nb_places, String nb_pr, String nb_pmr, String nb_voitures_electriques, String nb_velo, String nb_2r_el, String nb_autopartage, String nb_2_rm, String nb_covoit, String hauteur_max, String num_siret, String xlong, String ylat, String tarif_pmr, String tarif_1h, String tarif_2h, String tarif_3h, String tarif_4h, String tarif_24h, String abo_resident, String abo_non_resident, String type_ouvrage, String info) {
        this.id = id;
        this.nom = nom;
        this.insee = insee;
        this.adresse = adresse;
        this.url = url;
        this.type_usagers = type_usagers;
        this.gratuit = gratuit;
        this.nb_places = nb_places;
        this.nb_pr = nb_pr;
        this.nb_pmr = nb_pmr;
        this.nb_voitures_electriques = nb_voitures_electriques;
        this.nb_velo = nb_velo;
        this.nb_2r_el = nb_2r_el;
        this.nb_autopartage = nb_autopartage;
        this.nb_2_rm = nb_2_rm;
        this.nb_covoit = nb_covoit;
        this.hauteur_max = hauteur_max;
        this.num_siret = num_siret;
        this.Xlong = xlong;
        this.Ylat = ylat;
        this.tarif_pmr = tarif_pmr;
        this.tarif_1h = tarif_1h;
        this.tarif_2h = tarif_2h;
        this.tarif_3h = tarif_3h;
        this.tarif_4h = tarif_4h;
        this.tarif_24h = tarif_24h;
        this.abo_resident = abo_resident;
        this.abo_non_resident = abo_non_resident;
        this.type_ouvrage = type_ouvrage;
        this.info = info;
    }

    public String getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getInsee() {
        return insee;
    }

    public String getAdresse() {
        return adresse;
    }

    public String getUrl() {
        return url;
    }

    public String getType_usagers() {
        return type_usagers;
    }

    public String getGratuit() {
        return gratuit;
    }

    public String getNb_places() {
        return nb_places;
    }

    public String getNb_pr() {
        return nb_pr;
    }

    public String getNb_pmr() {
        return nb_pmr;
    }

    public String getNb_voitures_electriques() {
        return nb_voitures_electriques;
    }

    public String getNb_velo() {
        return nb_velo;
    }

    public String getNb_2r_el() {
        return nb_2r_el;
    }

    public String getNb_autopartage() {
        return nb_autopartage;
    }

    public String getNb_2_rm() {
        return nb_2_rm;
    }

    public String getNb_covoit() {
        return nb_covoit;
    }

    public String getHauteur_max() {
        return hauteur_max;
    }

    public String getNum_siret() {
        return num_siret;
    }

    public String getXlong() {
        return Xlong;
    }

    public String getYlat() {
        return Ylat;
    }

    public String getTarif_pmr() {
        return tarif_pmr;
    }

    public String getTarif_1h() {
        return tarif_1h;
    }

    public String getTarif_2h() {
        return tarif_2h;
    }

    public String getTarif_3h() {
        return tarif_3h;
    }

    public String getTarif_4h() {
        return tarif_4h;
    }

    public String getTarif_24h() {
        return tarif_24h;
    }

    public String getAbo_resident() {
        return abo_resident;
    }

    public String getAbo_non_resident() {
        return abo_non_resident;
    }

    public String getType_ouvrage() {
        return type_ouvrage;
    }

    public String getInfo() {
        return info;
    }



    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id", id)
                .add("nom", nom)
                .add("insee", insee)
                .add("adresse", adresse)
                .add("url", url)
                .add("type_usagers", type_usagers)
                .add("gratuit", gratuit)
                .add("nb_places", nb_places)
                .add("nb_pr", nb_pr)
                .add("nb_pmr", nb_pmr)
                .add("nb_voitures_electriques", nb_voitures_electriques)
                .add("nb_velo", nb_velo)
                .add("nb_2r_el", nb_2r_el)
                .add("nb_autopartage", nb_autopartage)
                .add("nb_2_rm", nb_2_rm)
                .add("nb_covoit", nb_covoit)
                .add("hauteur_max", hauteur_max)
                .add("num_siret", num_siret)
                .add("Xlong", Xlong)
                .add("Ylat", Ylat)
                .add("tarif_pmr", tarif_pmr)
                .add("tarif_1h", tarif_1h)
                .add("tarif_2h", tarif_2h)
                .add("tarif_3h", tarif_3h)
                .add("tarif_4h", tarif_4h)
                .add("tarif_24h", tarif_24h)
                .add("abo_resident", abo_resident)
                .add("abo_non_resident", abo_non_resident)
                .add("type_ouvrage", type_ouvrage)
                .add("info", info)
                .toString();
    }
}
