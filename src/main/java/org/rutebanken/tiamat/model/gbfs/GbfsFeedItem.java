package org.rutebanken.tiamat.model.gbfs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GbfsFeedItem {

    @JsonProperty("name")
    private String serviceName;

    @JsonProperty("url")
    private String serviceUrl;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }
}
