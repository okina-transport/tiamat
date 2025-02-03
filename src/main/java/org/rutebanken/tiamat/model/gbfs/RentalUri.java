package org.rutebanken.tiamat.model.gbfs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RentalUri {
    @JsonProperty("android")
    private String rentalUriAndroid;

    @JsonProperty("ios")
    private String rentalUriIos;

    @JsonProperty("web")
    private String bookingUri;

    public String getRentalUriAndroid() {
        return rentalUriAndroid;
    }

    public void setRentalUriAndroid(String rentalUriAndroid) {
        this.rentalUriAndroid = rentalUriAndroid;
    }

    public String getRentalUriIos() {
        return rentalUriIos;
    }

    public void setRentalUriIos(String rentalUriIos) {
        this.rentalUriIos = rentalUriIos;
    }

    public String getBookingUri() {
        return bookingUri;
    }

    public void setBookingUri(String bookingUri) {
        this.bookingUri = bookingUri;
    }
}
