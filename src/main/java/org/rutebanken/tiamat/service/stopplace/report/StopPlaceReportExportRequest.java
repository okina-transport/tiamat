package org.rutebanken.tiamat.service.stopplace.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StopPlaceReportExportRequest {

    public enum Type {
        STOP_PLACES,
        QUAYS
    }

    private Type type;

    /**
     * Ids of the columns to export, in order (same ids as the report page columns).
     */
    private List<String> columns = new ArrayList<>();

    /**
     * Search arguments, with the same names and values as the arguments of the stopPlace GraphQL query.
     */
    private Map<String, Object> arguments = new HashMap<>();

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}
