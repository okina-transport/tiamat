package org.rutebanken.tiamat.service.parking;

import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.ParkingVersionedSaverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
@Transactional
public class BikeParkingsImportedService {

    private static final String ID_LOCAL = "id_local";
    private static final String ID_OSM = "id_osm";


    private ParkingRepository parkingRepository;
    private NetexIdMapper netexIdMapper;
    private ParkingVersionedSaverService parkingVersionedSaverService;
    private VersionCreator versionCreator;

    @Autowired
    BikeParkingsImportedService(ParkingRepository parkingRepository, NetexIdMapper netexIdMapper, ParkingVersionedSaverService parkingVersionedSaverService, VersionCreator versionCreator) {
        this.parkingRepository = parkingRepository;
        this.netexIdMapper = netexIdMapper;
        this.parkingVersionedSaverService = parkingVersionedSaverService;
        this.versionCreator = versionCreator;
    }

    public void createBikeParkings(List<Parking> bikeParkingsToSave) {
        for (Parking bikeParkingToSave : bikeParkingsToSave) {

            Parking parkingInBDD = retrieveBikeParkingInBDD(bikeParkingToSave);

            if (parkingInBDD == null) {
                netexIdMapper.moveOriginalIdToKeyValueList(bikeParkingToSave, bikeParkingToSave.getName().getValue());
                netexIdMapper.moveOriginalNameToKeyValueList(bikeParkingToSave, bikeParkingToSave.getName().getValue());

                bikeParkingToSave.setName(new EmbeddableMultilingualString(bikeParkingToSave.getName().getValue()));
                parkingVersionedSaverService.saveNewVersion(bikeParkingToSave);
            }
        }
    }

    private Parking retrieveBikeParkingInBDD(Parking parking) {
        Set values = new HashSet(parking.getKeyValues().get(ID_LOCAL).getItems());

        String parkingNetexId = parkingRepository.findFirstByKeyValues(ID_LOCAL, values);

        if (parkingNetexId != null && !parkingNetexId.isEmpty()) {
            Parking existingParking = parkingRepository.findFirstByNetexIdOrderByVersionDesc(parkingNetexId);
            if (existingParking.getKeyValues().get(ID_OSM) != null && existingParking.getKeyValues().get(ID_OSM).getItems() != null) {
                for (String item : existingParking.getKeyValues().get(ID_OSM).getItems()) {
                    if (parking.getKeyValues().get(ID_OSM) != null && parking.getKeyValues().get(ID_OSM).getItems().contains(item)) {
                        return existingParking;
                    }
                }
            }

            else if (existingParking.getKeyValues().get(ID_OSM) == null && parking.getKeyValues().get(ID_OSM) == null){
                return existingParking;
            }
        }

        return null;

    }
}
