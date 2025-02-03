package org.rutebanken.tiamat.model.gbfs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemInformation {
    @JsonProperty("system_id")
    private String systemId;
    @JsonProperty("language")
    private String language;
    @JsonProperty("name")
    private String name;
    @JsonProperty("short_name")
    private String shortName;
    @JsonProperty("operator")
    private String operator;
    @JsonProperty("url")
    private String url;
    @JsonProperty("purchase_url")
    private String purchaseUrl;
    @JsonProperty("phone_number")
    private String phoneNumber;
    @JsonProperty("email")
    private String email;
    @JsonProperty("timezone")
    private String timezone;
    @JsonProperty("rental_apps")
    private RentalApps rentalApps;

    public String getSystemId() {
        return systemId;
    }

    public String getLanguage() {
        return language;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getOperator() {
        return operator;
    }

    public String getUrl() {
        return url;
    }

    public String getPurchaseUrl() {
        return purchaseUrl;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setPurchaseUrl(String purchaseUrl) {
        this.purchaseUrl = purchaseUrl;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public RentalApps getRentalApps() {
        return rentalApps;
    }

    public void setRentalApps(RentalApps rentalApps) {
        this.rentalApps = rentalApps;
    }
}
