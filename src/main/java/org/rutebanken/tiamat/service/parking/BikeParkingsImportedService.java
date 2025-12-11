package org.rutebanken.tiamat.service.parking;

import io.micrometer.core.instrument.util.StringUtils;
import org.rutebanken.tiamat.feign.mdm.OkinaIdentifier;
import org.rutebanken.tiamat.importer.mdm.MdmService;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.Value;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.ParkingVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.ArrayList;

import java.util.List;
import java.util.Optional;



@Service
@Transactional
public class BikeParkingsImportedService {

    private static final Logger logger = LoggerFactory.getLogger(BikeParkingsImportedService.class);

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
    BikeParkingsImportedService(ParkingRepository parkingRepository, NetexIdMapper netexIdMapper, ParkingVersionedSaverService parkingVersionedSaverService, VersionCreator versionCreator, MdmService mdmService) {
        this.parkingRepository = parkingRepository;
        this.netexIdMapper = netexIdMapper;
        this.parkingVersionedSaverService = parkingVersionedSaverService;
        this.versionCreator = versionCreator;
        this.mdmService = mdmService;
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
            }else{
                Value idLocValue = bikeParkingToSave.getKeyValues().get(ID_LOCAL);
                logger.warn("Un parking avec id_local '{}' existe déjà. Le parking est esquivé et ne sera ni importé, ni modifié.",
                        idLocValue.getItems().iterator().next());
            }
        }
    }



    private Optional<Parking> retrieveBikeParkingInBDD(Parking parking) {
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
