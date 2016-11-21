package org.rutebanken.tiamat.model;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;


@MappedSuperclass
public class BoardingPosition_VersionStructure
        extends StopPlaceSpace_VersionStructure {

    protected String publicCode;

    @Enumerated(EnumType.STRING)
    protected BoardingPositionTypeEnumeration boardingPositionType;

    @Transient
    protected EntranceRefs_RelStructure boardingPositionEntrances;

    public String getPublicCode() {
        return publicCode;
    }

    public void setPublicCode(String value) {
        this.publicCode = value;
    }

    public BoardingPositionTypeEnumeration getBoardingPositionType() {
        return boardingPositionType;
    }

    public void setBoardingPositionType(BoardingPositionTypeEnumeration value) {
        this.boardingPositionType = value;
    }

    public EntranceRefs_RelStructure getBoardingPositionEntrances() {
        return boardingPositionEntrances;
    }

    public void setBoardingPositionEntrances(EntranceRefs_RelStructure value) {
        this.boardingPositionEntrances = value;
    }

}
