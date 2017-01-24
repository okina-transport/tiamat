package org.rutebanken.tiamat.dtoassembling.disassembler;

import org.rutebanken.tiamat.dtoassembling.dto.StopPlaceSearchDto;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.StopPlaceSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class StopPlaceSearchDisassembler {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceSearchDisassembler.class);

    private NetexIdMapper netexIdMapper;

    public StopPlaceSearchDisassembler(NetexIdMapper netexIdMapper) {
        this.netexIdMapper = netexIdMapper;
    }

    public StopPlaceSearch disassemble(StopPlaceSearchDto stopPlaceSearchDto) {

        StopPlaceSearch.Builder stopPlaceSearchBuilder = new StopPlaceSearch.Builder();

        List<StopTypeEnumeration> stopTypeEnums = new ArrayList<>();
        if (stopPlaceSearchDto.stopPlaceTypes != null) {
            stopPlaceSearchDto.stopPlaceTypes.forEach(string ->
                    stopTypeEnums.add(StopTypeEnumeration.fromValue(string)));
            stopPlaceSearchBuilder.setStopTypeEnumerations(stopTypeEnums);
        }

        if(stopPlaceSearchDto.idList != null) {
            stopPlaceSearchBuilder.setIdList(stopPlaceSearchDto.idList.stream()
                    .filter(nsrId -> nsrId.startsWith(NetexIdMapper.NSR))
                    .filter(nsrId -> nsrId.toLowerCase().contains(StopPlace.class.getSimpleName().toLowerCase()))
                    .map(nsrId -> netexIdMapper.extractLongAfterLastColon(nsrId))
                    .collect(Collectors.toList()));
        }
        stopPlaceSearchBuilder.setCountyIds(stopPlaceSearchDto.countyReferences);
        stopPlaceSearchBuilder.setMunicipalityIds(stopPlaceSearchDto.municipalityReferences);
        stopPlaceSearchBuilder.setQuery(stopPlaceSearchDto.query);
        stopPlaceSearchBuilder.setPageable(new PageRequest(stopPlaceSearchDto.page, stopPlaceSearchDto.size));

        StopPlaceSearch stopPlaceSearch = stopPlaceSearchBuilder.build();
        logger.info("Disassembled stop place search '{}'", stopPlaceSearch);

        return stopPlaceSearch;
    }

}
