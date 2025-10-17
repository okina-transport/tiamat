package org.rutebanken.tiamat.service.stopplace;

import java.time.Instant;

public class ExportFileSummary {
    private final String fileName;
    private final String userName;
    private final Instant startDate;

    public ExportFileSummary(String fileName, String username, Instant startDate) {
        this.fileName = fileName;
        this.userName = username;
        this.startDate = startDate;
    }

    public String getFileName() {
        return fileName;
    }

    public String getUserName() {
        return userName;
    }

    public Instant getStartDate() {
        return startDate;
    }
}
