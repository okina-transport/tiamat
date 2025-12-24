package org.rutebanken.tiamat.service.parking;

import io.micrometer.core.instrument.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.tiamat.feign.mdm.ParkingIdentifier;
import org.rutebanken.tiamat.importer.mdm.MdmService;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.Value;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.ParkingRepository;
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

    private final ParkingRepository parkingRepository;
    private final NetexIdMapper netexIdMapper;
    private final ParkingVersionedSaverService parkingVersionedSaverService;
    private final MdmService mdmService;

    @Autowired
    BikeParkingsImportedService(ParkingRepository parkingRepository, NetexIdMapper netexIdMapper, ParkingVersionedSaverService parkingVersionedSaverService, MdmService mdmService) {
        this.parkingRepository = parkingRepository;
        this.netexIdMapper = netexIdMapper;
        this.parkingVersionedSaverService = parkingVersionedSaverService;
        this.mdmService = mdmService;
    }

    public void createBikeParkings(List<Parking> bikeParkingsToSave) {
        for (Parking bikeParkingToSave : bikeParkingsToSave) {

            Optional<Parking> parkingInBDDOpt = retrieveBikeParkingInBDD(bikeParkingToSave);

            if (parkingInBDDOpt.isEmpty()) {

                if (StringUtils.isNotEmpty(bikeParkingToSave.getName().getValue())) {
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
        List<String> idLocs = new ArrayList<>(parking.getKeyValues().get(ID_LOCAL).getItems());
        if (mdmService.isMdmEnabled()) {
            String importedId = CollectionUtils.isNotEmpty(parking.getOriginalIds()) ?
                    parking.getOriginalIds().iterator().next() : parking.getOriginalId();
            Optional<ParkingIdentifier> existingMdmId =
                    mdmService.getExistingParkingMdmIdsFromImportedId(parking.getOperator(), importedId);
            if (existingMdmId.isPresent()) {
                Parking existingParking =
                        parkingRepository.findFirstByNetexIdOrderByVersionDesc(existingMdmId.get().getSuperId());
                if (existingParking != null) {
                    return Optional.of(existingParking);
                }
            }

            Value osmKeyVals = parking.getKeyValues().get(ID_OSM);
            String idOsm;
            if (osmKeyVals != null) {
                List<String> idOsms = new ArrayList<>(osmKeyVals.getItems());
                idOsm = idOsms.get(0);
            return parkingRepository.findByIdLocAndOsm(idLocs.get(0), idOsm);
            }

            return Optional.empty();
        } else {

            Value osmKeyVals = parking.getKeyValues().get(ID_OSM);
            String idOsm = null;
            if (osmKeyVals != null) {
                List<String> idOsms = new ArrayList<>(osmKeyVals.getItems());
                idOsm = idOsms.get(0);
            }

            return parkingRepository.findByIdLocAndOsm(idLocs.get(0), idOsm);
        }
    }
}
