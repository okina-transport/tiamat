package org.rutebanken.tiamat.model.gbfs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AndroidUrl {

    @JsonProperty("store_uri")
    private String androidStoreUri;
    @JsonProperty("discovery_uri")
    private String androidDiscoveryUri;

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
}
