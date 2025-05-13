package org.rutebanken.tiamat.service.parking;

import io.micrometer.core.instrument.util.StringUtils;
import org.rutebanken.tiamat.feign.mdm.OkinaIdentifier;
import org.rutebanken.tiamat.general.ParkingsCSVHelper;
import org.rutebanken.tiamat.importer.mdm.MdmService;
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
    private MdmService mdmService;

    @org.springframework.beans.factory.annotation.Value("${netex.validPrefix:MOBIITI}")
    String validNetexPrefix;

    @Autowired
    RentalBikeParkingsImportedService(ParkingRepository parkingRepository, NetexIdMapper netexIdMapper, ParkingVersionedSaverService parkingVersionedSaverService, VersionCreator versionCreator, MdmService mdmService) {
        this.parkingRepository = parkingRepository;
        this.netexIdMapper = netexIdMapper;
        this.parkingVersionedSaverService = parkingVersionedSaverService;
        this.versionCreator = versionCreator;
        this.mdmService = mdmService;
    }

    public void createOrUpdateParkings(List<Parking> parkingsToSave){

        for(Parking parkingToSave: parkingsToSave){

            Optional<Parking> parkingInBDDOpt = retrieveParkingInBDD(parkingToSave);

            if (!parkingInBDDOpt.isPresent()){

                if (StringUtils.isNotEmpty(parkingToSave.getName().getValue())){
                    netexIdMapper.moveOriginalNameToKeyValueList(parkingToSave, parkingToSave.getName().getValue());
                    parkingToSave.setName(new EmbeddableMultilingualString(parkingToSave.getName().getValue()));
                }
                parkingVersionedSaverService.saveNewVersion(parkingToSave);
            }
        }
    }


    private Optional<Parking> retrieveParkingInBDD(Parking parking) {
        List<String> idLocs = new ArrayList(parking.getKeyValues().get(ID_LOCAL).getItems());

        OkinaIdentifier existingMdmId = mdmService.getExistingParkingMdmIdsFromImportedId(idLocs.get(0));
        if (existingMdmId != null){
            Parking existingParking = parkingRepository.findFirstByNetexIdOrderByVersionDesc(validNetexPrefix + ":Parking:" + existingMdmId.getSuperId());
            if (existingParking != null){
                return Optional.of(existingParking);
            }
        }

        Value osmKeyVals = parking.getKeyValues().get(ID_OSM);
        String idOsm = null;
        if (osmKeyVals != null){
            List<String> idOsms = new ArrayList(osmKeyVals.getItems());
            idOsm = idOsms.get(0);
            return parkingRepository.findByOsm(idOsm);
        }

        return Optional.empty();
    }


}
