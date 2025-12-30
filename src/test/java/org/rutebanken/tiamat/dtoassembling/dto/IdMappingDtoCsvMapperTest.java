package org.rutebanken.tiamat.dtoassembling.dto;


import org.junit.jupiter.api.Test;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.time.ExportTimeZone;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class IdMappingDtoCsvMapperTest {

    private IdMappingDtoCsvMapper mapper = new IdMappingDtoCsvMapper(new ExportTimeZone());


    @Test
    public void whenNotIncludeStopTypeOrInterval_ignoreStopTypeAndInterval() {
        IdMappingDto dto = new IdMappingDto("orgId", "netexId", Instant.now(), Instant.now(), StopTypeEnumeration.AIRPORT);
        assertEquals("orgId,netexId", mapper.toCsvString(dto, false, false));
    }

    @Test
    public void whenIncludeStopTypeOrInterval_printStopTypeAndInterval() {
        IdMappingDto dto = new IdMappingDto("orgId", "netexId", Instant.EPOCH, Instant.EPOCH.plusSeconds(1), StopTypeEnumeration.AIRPORT);
        assertEquals("orgId,airport,netexId,1970-01-01T01:00:00,1970-01-01T01:00:01", mapper.toCsvString(dto, true, true));
    }

    @Test
    public void whenOptionalFieldsAreNull_printEmptyString() {
        IdMappingDto dto = new IdMappingDto("orgId", "netexId", null, null, null);
        assertEquals("orgId,,netexId,,", mapper.toCsvString(dto, true, true));
    }
}
