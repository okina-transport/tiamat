package org.rutebanken.tiamat.dtoassembling.dto;

import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;

import java.math.BigInteger;

public class IdMappingDto {
    public String originalId;
    public String netexId;


    public IdMappingDto(String originalId, String netexId) {
        this.originalId = originalId;
        this.netexId = netexId;
    }

    public String toCsvString() {
        return originalId + "," + netexId;
    }

}
