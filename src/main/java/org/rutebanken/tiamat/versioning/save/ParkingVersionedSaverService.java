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
import org.rutebanken.tiamat.versioning.VersionCreator;
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
    private VersionCreator versionCreator;

    @Autowired
    private ReflectionAuthorizationService reflectionAuthorizationService;

    public Parking saveNewVersion(Parking newVersion) {

//        Preconditions.checkArgument(newVersion.getParentSiteRef() != null, "Parent site ref cannot be null for parking");

        Parking existing = parkingRepository.findFirstByNetexIdOrderByVersionDesc(newVersion.getNetexId());
        Parking newVersionNotEqualsToExisting = versionCreator.createCopy(newVersion, Parking.class);
        if (newVersion.getParentSiteRef() != null) {
            resolveAndAuthorizeParkingSiteRef(newVersion);
        }

        Parking result;
        if (existing != null) {
            logger.trace("existing: {}", existing);
            logger.trace("new: {}", newVersionNotEqualsToExisting);

            if(existing.getParentSiteRef() != null) {
                resolveAndAuthorizeParkingSiteRef(existing);
            }
            newVersionNotEqualsToExisting.setCreated(existing.getCreated());
            newVersionNotEqualsToExisting.setChanged(Instant.now());
            newVersionNotEqualsToExisting.setVersion(existing.getVersion());

            parkingRepository.delete(existing);

        } else {
            mdmService.generateIdentifier(newVersionNotEqualsToExisting);
            newVersionNotEqualsToExisting.setCreated(Instant.now());
        }

        newVersionNotEqualsToExisting.setValidBetween(null);
        versionIncrementor.initiateOrIncrement(newVersionNotEqualsToExisting);
        Map<String, ParkingProperties> incrementedParkingPropertiesByNetexId = new HashMap<>();

        if (CollectionUtils.isNotEmpty(newVersionNotEqualsToExisting.getParkingProperties())) {
            newVersionNotEqualsToExisting.setParkingProperties(newVersionNotEqualsToExisting.getParkingProperties().stream()
                    .map(parkingProperties -> incrementSharedParkingProperties(parkingProperties, incrementedParkingPropertiesByNetexId))
                    .collect(Collectors.toList()));
        }

        if (CollectionUtils.isNotEmpty(newVersionNotEqualsToExisting.getParkingAreas())) {
            for (ParkingArea pa : newVersionNotEqualsToExisting.getParkingAreas()) {
                versionIncrementor.initiateOrIncrement(pa);
                pa.setParkingProperties(incrementSharedParkingProperties(pa.getParkingProperties(), incrementedParkingPropertiesByNetexId));
            }
        }

        if (CollectionUtils.isNotEmpty(newVersionNotEqualsToExisting.getEquipmentPlaces())) {
            for (EquipmentPlace ep : newVersionNotEqualsToExisting.getEquipmentPlaces()) {
                versionIncrementor.initiateOrIncrement(ep);
            }
        }

        newVersionNotEqualsToExisting.setChangedBy(usernameFetcher.getUserNameForAuthenticatedUser());
        if (newVersionNotEqualsToExisting.getPostalAddress() != null){
            newVersionNotEqualsToExisting.getPostalAddress().setId(null);
        }
        result = parkingRepository.save(newVersionNotEqualsToExisting);

        logger.info("Saved parking {}, version {}, name {}", result.getNetexId(), result.getVersion(), result.getName());

        metricsService.registerEntitySaved(newVersionNotEqualsToExisting.getClass());
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
