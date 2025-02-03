package org.rutebanken.tiamat.model.gbfs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IosUrl {
    @JsonProperty("store_uri")
    private String iosStoreUri;
    @JsonProperty("discovery_uri")
    private String iosDiscoveryUri;

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
}
