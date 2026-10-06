package org.rutebanken.tiamat.model;


public class ParkingIdEntity {

    protected String operator;

    protected String originalId;

    protected String superId;

    public ParkingIdEntity() {
    }

    public ParkingIdEntity(String operator, String originalId, String superId) {
        this.operator = operator;
        this.originalId = originalId;
        this.superId = superId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    public String getSuperId() {
        return superId;
    }

    public void setSuperId(String superId) {
        this.superId = superId;
    }
}
