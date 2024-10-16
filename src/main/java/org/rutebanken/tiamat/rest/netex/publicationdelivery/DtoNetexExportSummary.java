package org.rutebanken.tiamat.rest.netex.publicationdelivery;

import java.time.Instant;

public class DtoNetexExportSummary {

    private String fileName;

    private String userName;

    private Instant creationDate;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }
}
