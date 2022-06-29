package org.rutebanken.tiamat.service.parking;

import org.rutebanken.tiamat.general.ParkingsCSVHelper;
import org.rutebanken.tiamat.model.AccessibilityAssessment;
import org.rutebanken.tiamat.model.AccessibilityLimitation;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingPaymentProcessEnumeration;
import org.rutebanken.tiamat.model.ParkingProperties;
import org.rutebanken.tiamat.model.ParkingVehicleEnumeration;
import org.rutebanken.tiamat.model.Value;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.ParkingVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;


@Service
@Transactional
public class RentalBikeParkingsImportedService {

    private static final Logger logger = LoggerFactory.getLogger(RentalBikeParkingsImportedService.class);

    private static final String ID_LOCAL = "id_local";
    private static final String ID_OSM = "id_osm";

    private ParkingRepository parkingRepository;
    private NetexIdMapper netexIdMapper;
    private ParkingVersionedSaverService parkingVersionedSaverService;
    private VersionCreator versionCreator;

    @Autowired
    RentalBikeParkingsImportedService(ParkingRepository parkingRepository, NetexIdMapper netexIdMapper, ParkingVersionedSaverService parkingVersionedSaverService, VersionCreator versionCreator){
        this.parkingRepository = parkingRepository;
        this.netexIdMapper = netexIdMapper;
        this.parkingVersionedSaverService = parkingVersionedSaverService;
        this.versionCreator = versionCreator;
    }

    public void createOrUpdateParkings(List<Parking> parkingsToSave){

        for(Parking parkingToSave: parkingsToSave){

            Optional<Parking> parkingInBDDOpt = retrieveParkingInBDD(parkingToSave);

            if (!parkingInBDDOpt.isPresent()){
                netexIdMapper.moveOriginalIdToKeyValueList(parkingToSave, parkingToSave.getName().getValue());
                netexIdMapper.moveOriginalNameToKeyValueList(parkingToSave, parkingToSave.getName().getValue());

                parkingToSave.setName(new EmbeddableMultilingualString(parkingToSave.getName().getValue()));
                parkingVersionedSaverService.saveNewVersion(parkingToSave);
            }
        }
    }


    private Optional<Parking> retrieveParkingInBDD(Parking parking) {
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
