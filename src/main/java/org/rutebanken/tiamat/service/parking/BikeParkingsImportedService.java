package org.rutebanken.tiamat.service.parking;

import io.micrometer.core.instrument.util.StringUtils;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.Value;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.ParkingVersionedSaverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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

            Optional<Parking> parkingInBDDOpt = retrieveBikeParkingInBDD(bikeParkingToSave);

            if (!parkingInBDDOpt.isPresent()) {

                if (StringUtils.isNotEmpty(bikeParkingToSave.getName().getValue())){
                    netexIdMapper.moveOriginalNameToKeyValueList(bikeParkingToSave, bikeParkingToSave.getName().getValue());

                    bikeParkingToSave.setName(new EmbeddableMultilingualString(bikeParkingToSave.getName().getValue()));
                }
                parkingVersionedSaverService.saveNewVersion(bikeParkingToSave);
            }
        }
    }

    private Optional<Parking> retrieveBikeParkingInBDD(Parking parking) {
        List<String> idLocs = new ArrayList(parking.getKeyValues().get(ID_LOCAL).getItems());


        Value osmKeyVals = parking.getKeyValues().get(ID_OSM);
        String idOsm = null;
        if (osmKeyVals != null){
            List<String> idOsms = new ArrayList(osmKeyVals.getItems());
            idOsm = idOsms.get(0);
        }

        return parkingRepository.findByIdLocAndOsm(idLocs.get(0), idOsm);
    }
}
