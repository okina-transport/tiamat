package org.rutebanken.tiamat.externalapis;

public class DtoGeocode {

    private String cityCode;
    private String postCode;
    private String address;
    private String city;

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getPostCode() { return postCode; }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getAddress() { return address; }

    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }

    public void setCity(String city) { this.city = city; }
}
