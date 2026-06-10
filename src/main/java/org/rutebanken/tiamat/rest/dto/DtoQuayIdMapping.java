package org.rutebanken.tiamat.rest.dto;

public class DtoQuayIdMapping {
    private String uttuNetexId;
    private String tiamatSuperId;

    public DtoQuayIdMapping() {}

    public DtoQuayIdMapping(String uttuNetexId, String tiamatSuperId) {
        this.uttuNetexId = uttuNetexId;
        this.tiamatSuperId = tiamatSuperId;
    }

    public String getUttuNetexId() { return uttuNetexId; }
    public void setUttuNetexId(String uttuNetexId) { this.uttuNetexId = uttuNetexId; }

    public String getTiamatSuperId() { return tiamatSuperId; }
    public void setTiamatSuperId(String tiamatSuperId) { this.tiamatSuperId = tiamatSuperId; }
}
