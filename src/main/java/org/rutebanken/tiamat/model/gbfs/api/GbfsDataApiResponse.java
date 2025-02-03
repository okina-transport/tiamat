package org.rutebanken.tiamat.model.gbfs.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GbfsDataApiResponse<T> {
    @JsonProperty("last_updated")
    private long lastUpdated;
    @JsonProperty("ttl")
    private int timeToLive;
    @JsonProperty("version")
    private String version;
    private T data;

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public int getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(int timeToLive) {
        this.timeToLive = timeToLive;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
