package org.rutebanken.tiamat.rest.dto;

import com.google.common.base.MoreObjects;



public class DtoParking {

    private String id;
    private String name;
    private String insee;
    private String adress;
    private String url;
    private String userType;
    private String free;
    private String nbOfPlaces;
    private String nbOfPr;
    private String disabledParkingNb;
    private String electricVehicleNb;
    private String bikeNb;
    private String electricBikesNb;
    private String carSharingNb;
    private String motorcycleNb;
    private String carPoolingNb;
    private String maxHeight;
    private String siretNumber;
    private String Xlong;
    private String Ylat;
    private String disabledParkingPrice;
    private String oneHourPrice;
    private String twoHoursPrice;
    private String threeHoursPrice;
    private String fourHoursPrice;
    private String twentyFourHoursPrice;
    private String residentSubscription;
    private String nonResidentSubscription;
    private String workType;
    private String info;
    private String hookType;
    private String operator;

    public DtoParking(String id, String name, String insee, String adress, String url, String userType, String free, String nbOfPlaces, String nbOfPr, String disabledParkingNb, String electricVehicleNb, String bikeNb, String electricBikesNb, String carSharingNb, String motorcycleNb, String carPoolingNb, String maxHeight, String siretNumber, String xlong, String ylat, String disabledParkingPrice, String oneHourPrice, String twoHoursPrice, String threeHoursPrice, String fourHoursPrice, String twentyFourHoursPrice, String residentSubscription, String nonResidentSubscription, String workType, String info, String operator) {
        this.id = id;
        this.name = name;
        this.insee = insee;
        this.adress = adress;
        this.url = url;
        this.userType = userType;
        this.free = free;
        this.nbOfPlaces = nbOfPlaces;
        this.nbOfPr = nbOfPr;
        this.disabledParkingNb = disabledParkingNb;
        this.electricVehicleNb = electricVehicleNb;
        this.bikeNb = bikeNb;
        this.electricBikesNb = electricBikesNb;
        this.carSharingNb = carSharingNb;
        this.motorcycleNb = motorcycleNb;
        this.carPoolingNb = carPoolingNb;
        this.maxHeight = maxHeight;
        this.siretNumber = siretNumber;
        this.Xlong = xlong;
        this.Ylat = ylat;
        this.disabledParkingPrice = disabledParkingPrice;
        this.oneHourPrice = oneHourPrice;
        this.twoHoursPrice = twoHoursPrice;
        this.threeHoursPrice = threeHoursPrice;
        this.fourHoursPrice = fourHoursPrice;
        this.twentyFourHoursPrice = twentyFourHoursPrice;
        this.residentSubscription = residentSubscription;
        this.nonResidentSubscription = nonResidentSubscription;
        this.workType = workType;
        this.info = info;
        this.operator = operator;
    }

    public DtoParking() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getInsee() {
        return insee;
    }

    public String getAdress() {
        return adress;
    }

    public String getUrl() {
        return url;
    }

    public String getUserType() {
        return userType;
    }

    public String getFree() {
        return free;
    }

    public String getNbOfPlaces() {
        return nbOfPlaces;
    }

    public String getNbOfPr() {
        return nbOfPr;
    }

    public String getDisabledParkingNb() {
        return disabledParkingNb;
    }

    public String getElectricVehicleNb() {
        return electricVehicleNb;
    }

    public String getBikeNb() {
        return bikeNb;
    }

    public String getElectricBikesNb() {
        return electricBikesNb;
    }

    public String getCarSharingNb() {
        return carSharingNb;
    }

    public String getMotorcycleNb() {
        return motorcycleNb;
    }

    public String getCarPoolingNb() {
        return carPoolingNb;
    }

    public String getMaxHeight() {
        return maxHeight;
    }

    public String getSiretNumber() {
        return siretNumber;
    }

    public String getXlong() {
        return Xlong;
    }

    public String getYlat() {
        return Ylat;
    }

    public String getDisabledParkingPrice() {
        return disabledParkingPrice;
    }

    public String getOneHourPrice() {
        return oneHourPrice;
    }

    public String getTwoHoursPrice() {
        return twoHoursPrice;
    }

    public String getThreeHoursPrice() {
        return threeHoursPrice;
    }

    public String getFourHoursPrice() {
        return fourHoursPrice;
    }

    public String getTwentyFourHoursPrice() {
        return twentyFourHoursPrice;
    }

    public String getResidentSubscription() {
        return residentSubscription;
    }

    public String getNonResidentSubscription() {
        return nonResidentSubscription;
    }

    public String getWorkType() {
        return workType;
    }

    public String getInfo() {
        return info;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setInsee(String insee) {
        this.insee = insee;
    }

    public void setAdress(String adress) {
        this.adress = adress;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public void setFree(String free) {
        this.free = free;
    }

    public void setNbOfPlaces(String nbOfPlaces) {
        this.nbOfPlaces = nbOfPlaces;
    }

    public void setNbOfPr(String nbOfPr) {
        this.nbOfPr = nbOfPr;
    }

    public void setDisabledParkingNb(String disabledParkingNb) {
        this.disabledParkingNb = disabledParkingNb;
    }

    public void setElectricVehicleNb(String electricVehicleNb) {
        this.electricVehicleNb = electricVehicleNb;
    }

    public void setBikeNb(String bikeNb) {
        this.bikeNb = bikeNb;
    }

    public void setElectricBikesNb(String electricBikesNb) {
        this.electricBikesNb = electricBikesNb;
    }

    public void setCarSharingNb(String carSharingNb) {
        this.carSharingNb = carSharingNb;
    }

    public void setMotorcycleNb(String motorcycleNb) {
        this.motorcycleNb = motorcycleNb;
    }

    public void setCarPoolingNb(String carPoolingNb) {
        this.carPoolingNb = carPoolingNb;
    }

    public void setMaxHeight(String maxHeight) {
        this.maxHeight = maxHeight;
    }

    public void setSiretNumber(String siretNumber) {
        this.siretNumber = siretNumber;
    }

    public void setXlong(String xlong) {
        Xlong = xlong;
    }

    public void setYlat(String ylat) {
        Ylat = ylat;
    }

    public void setDisabledParkingPrice(String disabledParkingPrice) {
        this.disabledParkingPrice = disabledParkingPrice;
    }

    public void setOneHourPrice(String oneHourPrice) {
        this.oneHourPrice = oneHourPrice;
    }

    public void setTwoHoursPrice(String twoHoursPrice) {
        this.twoHoursPrice = twoHoursPrice;
    }

    public void setThreeHoursPrice(String threeHoursPrice) {
        this.threeHoursPrice = threeHoursPrice;
    }

    public void setFourHoursPrice(String fourHoursPrice) {
        this.fourHoursPrice = fourHoursPrice;
    }

    public void setTwentyFourHoursPrice(String twentyFourHoursPrice) {
        this.twentyFourHoursPrice = twentyFourHoursPrice;
    }

    public void setResidentSubscription(String residentSubscription) {
        this.residentSubscription = residentSubscription;
    }

    public void setNonResidentSubscription(String nonResidentSubscription) {
        this.nonResidentSubscription = nonResidentSubscription;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getHookType() {
        return hookType;
    }

    public void setHookType(String hookType) {
        this.hookType = hookType;
    }

    public void setOperator(String operator) { this.operator = operator; }

    public String getOperator() { return this.operator; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id", id)
                .add("nom", name)
                .add("insee", insee)
                .add("adresse", adress)
                .add("url", url)
                .add("type_usagers", userType)
                .add("gratuit", free)
                .add("nb_places", nbOfPlaces)
                .add("nb_pr", nbOfPr)
                .add("nb_pmr", disabledParkingNb)
                .add("nb_voitures_electriques", electricVehicleNb)
                .add("nb_velo", bikeNb)
                .add("nb_2r_el", electricBikesNb)
                .add("nb_autopartage", carSharingNb)
                .add("nb_2_rm", motorcycleNb)
                .add("nb_covoit", carPoolingNb)
                .add("hauteur_max", maxHeight)
                .add("num_siret", siretNumber)
                .add("Xlong", Xlong)
                .add("Ylat", Ylat)
                .add("tarif_pmr", disabledParkingPrice)
                .add("tarif_1h", oneHourPrice)
                .add("tarif_2h", twoHoursPrice)
                .add("tarif_3h", threeHoursPrice)
                .add("tarif_4h", fourHoursPrice)
                .add("tarif_24h", twentyFourHoursPrice)
                .add("abo_resident", residentSubscription)
                .add("abo_non_resident", nonResidentSubscription)
                .add("type_ouvrage", workType)
                .add("info", info)
                .add("operator", operator)
                .toString();
    }
}
