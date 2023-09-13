package org.rutebanken.tiamat.service.batch;

import org.rutebanken.tiamat.dtoassembling.dto.JbvCodeMappingDto;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IdMappingService {

    /**
     * Replace mapping by selected id mapping, if existing
     * @param mapByNetex
     * @param selectedIdMappings
     */
    void replaceBySelectedIds(Map<String, Map<String, List<JbvCodeMappingDto>>> mapByNetex, List<JbvCodeMappingDto> selectedIdMappings){

        for (JbvCodeMappingDto selectedIdMapping : selectedIdMappings) {
            String currentProv = selectedIdMapping.originalId.split(":")[0];

            if (!mapByNetex.containsKey(selectedIdMapping.netexId)){
                Map<String, List<JbvCodeMappingDto>> currentNetexMap = new HashMap<>();
                currentNetexMap.put(currentProv, Arrays.asList(selectedIdMapping));
                mapByNetex.put(selectedIdMapping.netexId,currentNetexMap);
                continue;
            }

            Map<String, List<JbvCodeMappingDto>> currentNetexMap = mapByNetex.get(selectedIdMapping.netexId);
            currentNetexMap.put(currentProv, Arrays.asList(selectedIdMapping));
        }

    }


    /**
     * Build a map that contains all mappings classified by netex_id and by provider
     *
     * @param mappings
     *      the complete lisst of mappings
     * @return
     *      the map
     */
    Map<String, Map<String, List<JbvCodeMappingDto>>> buildNetexIdByOrg(List<JbvCodeMappingDto> mappings){

        // Key : netex id (MOBIITI:Quay:123)
        // Value :  Map with:
        //                 Key = provider(PROV1)
        //                 Value = the list of mappings for this netex id / provider
        Map<String, Map<String, List<JbvCodeMappingDto>>> resultMap = new HashMap<>();

        for (JbvCodeMappingDto mapping : mappings) {

            Map<String, List<JbvCodeMappingDto>> netexMap;
            if (resultMap.containsKey(mapping.netexId)){
                netexMap = resultMap.get(mapping.netexId);
            }else{
                netexMap = new HashMap<>();
                resultMap.put(mapping.netexId, netexMap);
            }

            String provider = mapping.originalId.split(":")[0];
            List<JbvCodeMappingDto> mappingList;
            if (netexMap.containsKey(provider)){
                mappingList = netexMap.get(provider);
            }else{
                mappingList = new ArrayList<>();
                netexMap.put(provider, mappingList);
            }
            mappingList.add(mapping);
        }
        return resultMap;
    }

}
