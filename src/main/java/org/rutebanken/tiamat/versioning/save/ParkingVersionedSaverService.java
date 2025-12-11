/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.versioning.save;


import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.helper.organisation.ReflectionAuthorizationService;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.importer.mdm.MdmService;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.repository.reference.ReferenceResolver;
import org.rutebanken.tiamat.service.metrics.MetricsService;
import org.rutebanken.tiamat.versioning.VersionIncrementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.rutebanken.helper.organisation.AuthorizationConstants.ROLE_EDIT_STOPS;

@Transactional
@Service
public class ParkingVersionedSaverService {

    private static final Logger logger = LoggerFactory.getLogger(ParkingVersionedSaverService.class);

    @Autowired
    private ParkingRepository parkingRepository;

    @Autowired
    private UsernameFetcher usernameFetcher;

    @Autowired
    private ReferenceResolver referenceResolver;

    @Autowired
    private VersionIncrementor versionIncrementor;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private MdmService mdmService;

    @Autowired
    private ReflectionAuthorizationService reflectionAuthorizationService;

    public Parking saveNewVersion(Parking newVersion) {

//        Preconditions.checkArgument(newVersion.getParentSiteRef() != null, "Parent site ref cannot be null for parking");

        Parking existing = parkingRepository.findFirstByNetexIdOrderByVersionDesc(newVersion.getNetexId());

        if(newVersion.getParentSiteRef() != null){
            resolveAndAuthorizeParkingSiteRef(newVersion);
        }

        Parking result;
        if (existing != null) {
            logger.trace("existing: {}", existing);
            logger.trace("new: {}", newVersion);

            if(existing.getParentSiteRef() != null) {
                resolveAndAuthorizeParkingSiteRef(existing);
            }
            newVersion.setCreated(existing.getCreated());
            newVersion.setChanged(Instant.now());
            newVersion.setVersion(existing.getVersion());

            parkingRepository.delete(existing);
        } else {
            mdmService.generateIdentifier(newVersion);
            newVersion.setCreated(Instant.now());
        }


        newVersion.setValidBetween(null);
        versionIncrementor.initiateOrIncrement(newVersion);
        Map<String, ParkingProperties> incrementedParkingPropertiesByNetexId = new HashMap<>();

        if (CollectionUtils.isNotEmpty(newVersion.getParkingProperties())) {
            newVersion.setParkingProperties(newVersion.getParkingProperties().stream()
                    .map(parkingProperties -> incrementSharedParkingProperties(parkingProperties, incrementedParkingPropertiesByNetexId))
                    .collect(Collectors.toList()));
        }

        if (CollectionUtils.isNotEmpty(newVersion.getParkingAreas())) {
            for (ParkingArea pa : newVersion.getParkingAreas()) {
                versionIncrementor.initiateOrIncrement(pa);
                pa.setParkingProperties(incrementSharedParkingProperties(pa.getParkingProperties(), incrementedParkingPropertiesByNetexId));
            }
        }

        if (CollectionUtils.isNotEmpty(newVersion.getEquipmentPlaces())) {
            for (EquipmentPlace ep : newVersion.getEquipmentPlaces()) {
                versionIncrementor.initiateOrIncrement(ep);
            }
        }

        if (newVersion.getPlaceEquipments() != null) {
            versionIncrementor.initiateOrIncrementPlaceEquipment(newVersion.getPlaceEquipments());
        }

        newVersion.setChangedBy(usernameFetcher.getUserNameForAuthenticatedUser());
        if (newVersion.getPostalAddress() != null){
            newVersion.getPostalAddress().setId(null);
        }
        result = parkingRepository.save(newVersion);


        logger.info("Saved parking {}, version {}, name {}", result.getNetexId(), result.getVersion(), result.getName());

        metricsService.registerEntitySaved(newVersion.getClass());
        return result;
    }

    private ParkingProperties incrementSharedParkingProperties(ParkingProperties parkingProperties, Map<String, ParkingProperties> incrementedParkingPropertiesByNetexId) {
        if (parkingProperties == null) {
            return null;
        }

        String netexId = parkingProperties.getNetexId();
        if (netexId != null && incrementedParkingPropertiesByNetexId.containsKey(netexId)) {
            return incrementedParkingPropertiesByNetexId.get(netexId);
        }

        if (CollectionUtils.isNotEmpty(parkingProperties.getSpaces())) {
            for (ParkingCapacity parkingSpace : parkingProperties.getSpaces()) {
                versionIncrementor.initiateOrIncrement(parkingSpace);
            }
        }
        versionIncrementor.initiateOrIncrement(parkingProperties);

        if (netexId != null) {
            incrementedParkingPropertiesByNetexId.put(netexId, parkingProperties);
        }
        return parkingProperties;
    }

    /**
     * A parking must refer to a stop place.
     * And the user must be authorized to edit this stop place.
     * In NeTEx, a parking can refer to any site. But this implementation is for now limited to stop place.
     *
     * @param parking
     */
    private void resolveAndAuthorizeParkingSiteRef(Parking parking) {
        DataManagedObjectStructure parentSite = referenceResolver.resolve(parking.getParentSiteRef());
        if (parentSite == null) {
            throw new IllegalArgumentException("Cannot save parking without resolvable parent site ref: " + parking.toString());
        }
        if (!(parentSite instanceof StopPlace)) {
            throw new IllegalArgumentException("Parking must have a parentSiteRef pointing to stop place. Parking: " + parking.toString() + " Parent site: " + parentSite);
        }
        reflectionAuthorizationService.assertAuthorized(ROLE_EDIT_STOPS, Arrays.asList(parentSite));
    }
}
