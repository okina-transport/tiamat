package org.rutebanken.tiamat.model;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Set;

@Entity
public class Organisation {
    @Id
    @GeneratedValue(generator = "sequence_per_table_generator")
    private Long id;

    private String netexId;

    @UpdateTimestamp
    private Instant changed;

    @CreationTimestamp
    private Instant created;

    private String name;

    private String shortName;

    private String type;

    private String operator;

    private String organisationUrl;

    private String purchaseUrl;

    private String phoneNumber;

    private String email;

    private String androidStoreUri;

    private String androidDiscoveryUri;

    private String iosStoreUri;

    private String iosDiscoveryUri;

    private String language;

    private String timezone;

    @OneToMany(mappedBy="organisation")
    private Set<Parking> parkings;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNetexId() {
        return netexId;
    }

    public void setNetexId(String netexId) {
        this.netexId = netexId;
    }

    public Instant getChanged() {
        return changed;
    }

    public void setChanged(Instant changed) {
        this.changed = changed;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getOrganisationUrl() {
        return organisationUrl;
    }

    public void setOrganisationUrl(String organisationUrl) {
        this.organisationUrl = organisationUrl;
    }

    public String getPurchaseUrl() {
        return purchaseUrl;
    }

    public void setPurchaseUrl(String purchaseUrl) {
        this.purchaseUrl = purchaseUrl;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAndroidStoreUri() {
        return androidStoreUri;
    }

    public void setAndroidStoreUri(String androidStoreUri) {
        this.androidStoreUri = androidStoreUri;
    }

    public String getAndroidDiscoveryUri() {
        return androidDiscoveryUri;
    }

    public void setAndroidDiscoveryUri(String androidDiscoveryUri) {
        this.androidDiscoveryUri = androidDiscoveryUri;
    }

    public String getIosStoreUri() {
        return iosStoreUri;
    }

    public void setIosStoreUri(String iosStoreUri) {
        this.iosStoreUri = iosStoreUri;
    }

    public String getIosDiscoveryUri() {
        return iosDiscoveryUri;
    }

    public void setIosDiscoveryUri(String iosDiscoveryUri) {
        this.iosDiscoveryUri = iosDiscoveryUri;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
