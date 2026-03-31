package org.rutebanken.tiamat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class PostalAddress extends EntityInVersionStructure {

    private String street;

    private String town;

    @Column(name = "postal_region")
    private String postalRegion;

    @Column(name = "post_code")
    private String postCode;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getPostalRegion() {
        return postalRegion;
    }

    public void setPostalRegion(String postalRegion) {
        this.postalRegion = postalRegion;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

}
