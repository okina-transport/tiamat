package org.rutebanken.tiamat.changelog;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;

public class LogEntryDto implements Serializable {

    @JsonProperty("event_timestamp")
    private Instant eventTimestamp;

    @JsonProperty("action_type")
    private String actionType;

    @JsonProperty("user")
    private String user;

    @JsonProperty("object_id")
    private String objectId;

    @JsonProperty("organization")
    private String organization;

    @JsonProperty("service")
    private String service;

    @JsonProperty("log_content")
    private LogContentDto logContent;

    public LogEntryDto() {
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public LogContentDto getLogContent() {
        return logContent;
    }

    public void setLogContent(LogContentDto logContent) {
        this.logContent = logContent;
    }
}
