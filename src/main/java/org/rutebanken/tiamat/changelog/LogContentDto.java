package org.rutebanken.tiamat.changelog;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class LogContentDto implements Serializable  {

    @JsonProperty("metadata")
    private String metadata;

    @JsonProperty("object_before")
    private String objectBefore;

    @JsonProperty("object_after")
    private String objectAfter;

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getObjectBefore() {
        return objectBefore;
    }

    public void setObjectBefore(String objectBefore) {
        this.objectBefore = objectBefore;
    }

    public String getObjectAfter() {
        return objectAfter;
    }

    public void setObjectAfter(String objectAfter) {
        this.objectAfter = objectAfter;
    }
}
