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

package org.rutebanken.tiamat.rest.graphql.fetchers;

import com.google.common.base.Preconditions;
import org.locationtech.jts.geom.Point;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.rutebanken.helper.organisation.ReflectionAuthorizationService;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.rutebanken.tiamat.rest.graphql.mappers.GeometryMapper;
import org.rutebanken.tiamat.rest.graphql.mappers.GroupOfEntitiesMapper;
import org.rutebanken.tiamat.rest.graphql.mappers.ValidBetweenMapper;
import org.rutebanken.tiamat.versioning.save.ParkingVersionedSaverService;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.rutebanken.helper.organisation.AuthorizationConstants.ROLE_EDIT_STOPS;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;
import static org.rutebanken.tiamat.rest.graphql.mappers.EmbeddableMultilingualStringMapper.getEmbeddableString;

@Service("parkingUpdater")
@Transactional
class ParkingUpdater implements DataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(ParkingUpdater.class);

    @Autowired
    private ParkingRepository parkingRepository;

    @Autowired
    private ParkingVersionedSaverService parkingVersionedSaverService;

    @Autowired
    private GeometryMapper geometryMapper;

    @Autowired
    private ReflectionAuthorizationService authorizationService;

    @Autowired
    private ValidBetweenMapper validBetweenMapper;

    @Autowired
    private VersionCreator versionCreator;

    @Autowired
    private GroupOfEntitiesMapper groupOfEntitiesMapper;


    @Override
    public Object get(DataFetchingEnvironment environment) {

        List<Map> input = environment.getArgument(OUTPUT_TYPE_PARKING);
        List<Parking> parkings = null;
        if (input != null) {
            parkings = input.stream()
             .map(m -> createOrUpdateParking(m))
            .collect(Collectors.toList());
        }
        return parkings;
    }

    private Parking createOrUpdateParking(Map input){
        Parking updatedParking;
        Parking existingVersion = null;
        String netexId = (String) input.get(ID);
        if (netexId != null) {
            logger.info("Updating Parking {}", netexId);
            existingVersion = parkingRepository.findFirstByNetexIdOrderByVersionDesc(netexId);
            Preconditions.checkArgument(existingVersion != null, "Attempting to update Parking [id = %s], but Parking does not exist.", netexId);
            updatedParking = versionCreator.createCopy(existingVersion, Parking.class);

        } else {
            logger.info("Creating new Parking");
            updatedParking = new Parking();
        }

        boolean isUpdated = populateParking(input, updatedParking);
        if (isUpdated) {
            authorizationService.assertAuthorized(ROLE_EDIT_STOPS, Arrays.asList(existingVersion, updatedParking));

            logger.info("Saving new version of parking {}", updatedParking);
            updatedParking = parkingVersionedSaverService.saveNewVersion(updatedParking);

            return updatedParking;
        } else {
            logger.info("No changes - Parking {} NOT updated", netexId);
        }
        return existingVersion;
    }

    private boolean populateParking(Map input, Parking updatedParking) {
        boolean isUpdated = false;
        if (input.get(VALID_BETWEEN) != null) {
            updatedParking.setValidBetween(validBetweenMapper.map((Map) input.get(VALID_BETWEEN)));
            isUpdated = true;
        }

        if (input.get(GEOMETRY) != null) {
            Point geoJsonPoint = geometryMapper.createGeoJsonPoint((Map) input.get(GEOMETRY));
            isUpdated = isUpdated || (!geoJsonPoint.equals(updatedParking.getCentroid()));
            updatedParking.setCentroid(geoJsonPoint);
        }

        if (input.get(PARENT_SITE_REF) != null) {
            SiteRefStructure parentSiteRef = new SiteRefStructure();
            parentSiteRef.setRef((String) input.get(PARENT_SITE_REF));

            isUpdated = isUpdated || (!parentSiteRef.equals(updatedParking.getParentSiteRef()));

            updatedParking.setParentSiteRef(parentSiteRef);
        }

        if (input.get(TOTAL_CAPACITY) != null) {
            BigInteger totalCapacity = (BigInteger) input.get(TOTAL_CAPACITY);
            isUpdated = isUpdated || (!totalCapacity.equals(updatedParking.getTotalCapacity()));

            updatedParking.setTotalCapacity(totalCapacity);
        }

        if (input.get(PRINCIPAL_CAPACITY) != null) {
            BigInteger principalCapacity = (BigInteger) input.get(PRINCIPAL_CAPACITY);
            isUpdated = isUpdated || (!principalCapacity.equals(updatedParking.getPrincipalCapacity()));

            updatedParking.setPrincipalCapacity(principalCapacity);
        }

        if (input.get(PARKING_TYPE) != null) {
            ParkingTypeEnumeration parkingType = (ParkingTypeEnumeration) input.get(PARKING_TYPE);
            isUpdated = isUpdated || (!parkingType.equals(updatedParking.getParkingType()));
            updatedParking.setParkingType(parkingType);
        }

        if (input.get(PARKING_VEHICLE_TYPES) != null) {
            List<ParkingVehicleEnumeration> vehicleTypes = (List<ParkingVehicleEnumeration>) input.get(PARKING_VEHICLE_TYPES);
            isUpdated = isUpdated || !(updatedParking.getParkingVehicleTypes().containsAll(vehicleTypes) &&
                    vehicleTypes.containsAll(updatedParking.getParkingVehicleTypes()));

            updatedParking.getParkingVehicleTypes().clear();
            updatedParking.getParkingVehicleTypes().addAll(vehicleTypes);
        }

        if (input.get(PARKING_LAYOUT) != null) {
            ParkingLayoutEnumeration parkingLayout = (ParkingLayoutEnumeration) input.get(PARKING_LAYOUT);
            isUpdated = isUpdated || (!parkingLayout.equals(updatedParking.getParkingLayout()));
            updatedParking.setParkingLayout(parkingLayout);
        }

        if (input.get(OVERNIGHT_PARKING_PERMITTED) != null) {
            Boolean overnightParkingPermitted = (Boolean) input.get(OVERNIGHT_PARKING_PERMITTED);
            isUpdated = isUpdated || (!overnightParkingPermitted.equals(updatedParking.isOvernightParkingPermitted()));
            updatedParking.setOvernightParkingPermitted(overnightParkingPermitted);
        }

        if (input.get(RECHARGING_AVAILABLE) != null) {
            Boolean rechargingAvailable = (Boolean) input.get(RECHARGING_AVAILABLE);
            isUpdated = isUpdated || (!rechargingAvailable.equals(updatedParking.isRechargingAvailable()));
            updatedParking.setRechargingAvailable(rechargingAvailable);
        }

        if (input.get(CARPOOLING_AVAILABLE) != null) {
            Boolean carpoolingAvailable = (Boolean) input.get(CARPOOLING_AVAILABLE);
            isUpdated = isUpdated || (!carpoolingAvailable.equals(updatedParking.isCarpoolingAvailable()));
            updatedParking.setCarpoolingAvailable(carpoolingAvailable);
        }

        if (input.get(CARSHARING_AVAILABLE) != null) {
            Boolean carsharingAvailable = (Boolean) input.get(CARSHARING_AVAILABLE);
            isUpdated = isUpdated || (!carsharingAvailable.equals(updatedParking.isCarsharingAvailable()));
            updatedParking.setCarsharingAvailable(carsharingAvailable);
        }

        if (input.get(SECURE) != null) {
            Boolean isSecure = (Boolean) input.get(SECURE);
            isUpdated = isUpdated || (!isSecure.equals(updatedParking.isSecure()));
            updatedParking.setSecure(isSecure);
        }

        if (input.get(REAL_TIME_OCCUPANCY_AVAILABLE) != null) {
            Boolean isRealtimeOccupancyAvailable = (Boolean) input.get(REAL_TIME_OCCUPANCY_AVAILABLE);
            isUpdated = isUpdated || (!isRealtimeOccupancyAvailable.equals(updatedParking.isRealTimeOccupancyAvailable()));
            updatedParking.setRealTimeOccupancyAvailable(isRealtimeOccupancyAvailable);
        }

        if (input.get(FREE_PARKING_OUT_OF_HOURS) != null) {
            Boolean freeParkingOutOfHours = (Boolean) input.get(FREE_PARKING_OUT_OF_HOURS);
            isUpdated = isUpdated || (!freeParkingOutOfHours.equals(updatedParking.isFreeParkingOutOfHours()));
            updatedParking.setFreeParkingOutOfHours(freeParkingOutOfHours);
        }

        if (input.get(PARKING_PAYMENT_PROCESS) != null) {
            List<ParkingPaymentProcessEnumeration> parkingPaymentProcessTypes = (List<ParkingPaymentProcessEnumeration>) input.get(PARKING_PAYMENT_PROCESS);
            isUpdated = isUpdated || !(updatedParking.getParkingPaymentProcess().containsAll(parkingPaymentProcessTypes) &&
                    parkingPaymentProcessTypes.containsAll(updatedParking.getParkingPaymentProcess()));

            updatedParking.getParkingPaymentProcess().clear();
            updatedParking.getParkingPaymentProcess().addAll(parkingPaymentProcessTypes);
        }

        if (input.get(PARKING_RESERVATION) != null) {
            ParkingReservationEnumeration parkingReservation = (ParkingReservationEnumeration) input.get(PARKING_RESERVATION);
            isUpdated = isUpdated || (!parkingReservation.equals(updatedParking.getParkingReservation()));
            updatedParking.setParkingReservation(parkingReservation);
        }

        if (input.get(BOOKING_URL) != null) {
            String bookingUrl = (String) input.get(BOOKING_URL);
            isUpdated = isUpdated || (!bookingUrl.equals(updatedParking.getBookingUrl()));
            updatedParking.setBookingUrl(bookingUrl);
        }

        if (input.get(PARKING_PROPERTIES) != null) {
            List<ParkingProperties> parkingPropertiesList = resolveParkingPropertiesList((List) input.get(PARKING_PROPERTIES));
            int total_capacity = parkingPropertiesList.stream()
                    .map(ParkingProperties::getSpaces)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(space -> space.getNumberOfSpaces() != null)
                    .filter(space -> space.getParkingUserType() != ParkingUserEnumeration.REGISTERED_DISABLED)
                    .mapToInt(space -> space.getNumberOfSpaces().intValue())
                    .sum();

            int newCarsharingCapacity = parkingPropertiesList.stream()
                    .map(ParkingProperties::getSpaces)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(space -> space.getNumberOfSpaces() != null)
                    .filter(space -> space.getParkingUserType() != ParkingUserEnumeration.REGISTERED_DISABLED)
                    .mapToInt(space -> space.getNumberOfCarsharingSpaces() != null ? space.getNumberOfCarsharingSpaces().intValue() : 0)
                    .sum();


            int newPmrCapacity = parkingPropertiesList.stream()
                    .map(ParkingProperties::getSpaces)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(space -> space.getNumberOfSpaces() != null)
                    .filter(space -> space.getParkingUserType() != null && space.getParkingUserType().equals(ParkingUserEnumeration.REGISTERED_DISABLED))
                    .mapToInt(space -> space.getNumberOfSpaces().intValue())
                    .sum();

            if (newPmrCapacity > total_capacity) {
                throw new IllegalArgumentException("La capacité PMR ne peut pas être supérieure à la capacité totale");
            }

            if (newCarsharingCapacity > total_capacity) {
                throw  new IllegalArgumentException("La capacité autopartage ne peut pas être supérieure à la capacité totale");
            }

            if (updatedParking.getParkingAreas() != null) {
                List<ParkingArea> parkingAreaList = new ArrayList<>();
                for (ParkingArea pa : updatedParking.getParkingAreas()) {
                    boolean toKeep = true;
                    if (pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.CARSHARE)) {
                        if (updatedParking.isCarsharingAvailable()) {
                            pa.setTotalCapacity(BigInteger.valueOf(newCarsharingCapacity));
                        } else {
                            toKeep = false;
                        }
                    } else if (pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.DISABLED)) {
                        pa.setTotalCapacity(BigInteger.valueOf(newPmrCapacity));
                    } else if (pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.CARPOOL)) {
                        if (!updatedParking.isCarpoolingAvailable()) {
                            toKeep = false;
                        }
                    } else {
                        pa.setTotalCapacity(BigInteger.valueOf(total_capacity));
                    }

                    if (toKeep) {
                        parkingAreaList.add(versionCreator.createCopy(pa, ParkingArea.class));
                    }
                }
                updatedParking.setParkingAreas(parkingAreaList);
            }

            isUpdated = true;
            updatedParking.setParkingProperties(parkingPropertiesList);

            if (total_capacity > 0) {
                updatedParking.setTotalCapacity(BigInteger.valueOf(total_capacity));
            }
        }

        if (input.get(PARKING_AREAS) != null) {
            List<ParkingArea> parkingAreasList = resolveParkingAreasList((List) input.get(PARKING_AREAS), updatedParking.getParkingAreas());

            int totalCapacity = parkingAreasList.stream()
                    .filter(pa -> pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.NONE))
                    .mapToInt(pa -> pa.getTotalCapacity() != null ? pa.getTotalCapacity().intValue() : 0)
                    .sum();

            int newCarpoolingCapacity = parkingAreasList.stream()
                    .filter(pa -> pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.CARPOOL))
                    .mapToInt(pa -> pa.getTotalCapacity() != null ? pa.getTotalCapacity().intValue() : 0)
                    .sum();

            if (newCarpoolingCapacity > totalCapacity) {
                throw  new IllegalArgumentException("La capacité covoiturage ne peut pas être supérieure à la capacité totale");
            }

            isUpdated = true;
            updatedParking.setParkingAreas(parkingAreasList.stream().map(pa -> versionCreator.createCopy(pa, ParkingArea.class)).collect(Collectors.toList()));
        }

        if (input.get(COVERED) != null) {
            CoveredEnumeration parkingCovered = (CoveredEnumeration) input.get(COVERED);
            isUpdated = isUpdated || (!parkingCovered.equals(updatedParking.getCovered()));
            updatedParking.setCovered(parkingCovered);
        }

        if (input.get(TYPE_OF_PARKING_REF) != null) {
            TypeOfParkingRefEnumeration parkingTypeOfParkingRef = (TypeOfParkingRefEnumeration) input.get(TYPE_OF_PARKING_REF);
            isUpdated = isUpdated || (!parkingTypeOfParkingRef.value().equals(updatedParking.getParkingTypeRef()));
            updatedParking.setTypeOfParkingRef(parkingTypeOfParkingRef.value());
        }

        isUpdated = isUpdated | groupOfEntitiesMapper.populate(input, updatedParking);

        return isUpdated;
    }

    private List<ParkingProperties> resolveParkingPropertiesList(List propertyList) {
        List<ParkingProperties> result = new ArrayList<>();
        for (Object property : propertyList) {
            result.add(resolveSingleParkingProperties((Map) property));
        }

        return result;
    }

    private ParkingProperties resolveSingleParkingProperties(Map input) {
        ParkingProperties p = new ParkingProperties();
        p.setSpaces(resolveParkingCapacities((List) input.get(SPACES)));
        return p;
    }

    private List<ParkingCapacity> resolveParkingCapacities(List input) {
        List<ParkingCapacity> result = new ArrayList<>();
        for (Object property : input) {
            result.add(resolveSingleParkingCapacity((Map) property));
        }

        return result;
    }

    private ParkingCapacity resolveSingleParkingCapacity(Map input) {
        ParkingCapacity capacity = new ParkingCapacity();
        capacity.setParkingUserType((ParkingUserEnumeration) input.get(PARKING_USER_TYPE));
        capacity.setParkingVehicleType((ParkingVehicleEnumeration) input.get(PARKING_VEHICLE_TYPE));
        capacity.setParkingStayType((ParkingStayEnumeration) input.get(PARKING_STAY_TYPE));
        capacity.setNumberOfSpaces((BigInteger) input.get(NUMBER_OF_SPACES));
        capacity.setNumberOfSpacesWithRechargePoint((BigInteger) input.get(NUMBER_OF_SPACES_WITH_RECHARGE_POINT));
        if (capacity.getNumberOfSpacesWithRechargePoint() != null && capacity.getNumberOfSpacesWithRechargePoint().compareTo(capacity.getNumberOfSpaces()) > 0) {
            throw  new IllegalArgumentException("Le nombre de places équipées de bornes de recharge ne peut pas être supérieur à la capacité totale");
        }
        capacity.setNumberOfCarsharingSpaces((BigInteger) input.get(NUMBER_OF_CARSHARING_SPACES));
        return capacity;
    }

    private List<ParkingArea> resolveParkingAreasList(List list, List<ParkingArea> existingParkingAreas) {
        List<ParkingArea> result = new ArrayList<>();
        for (Object property : list) {
            result.add(resolveSingleParkingArea((Map) property, existingParkingAreas));
        }

        if(existingParkingAreas != null) {
            result.addAll(existingParkingAreas.stream().filter(pa -> !pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.CARPOOL)).collect(Collectors.toList()));
        }

        return result;
    }

    private ParkingArea resolveSingleParkingArea(Map input, List<ParkingArea> existingParkingArea) {
        ParkingArea parkingArea = null;
        if(existingParkingArea != null){
            for (ParkingArea pa : existingParkingArea) {
                if (pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.CARPOOL)){
                    parkingArea = pa;
                    break;
                }
            }
        }
        if (parkingArea == null) {
            parkingArea = new ParkingArea();
            if (input.get("specificParkingAreaUsage") != null){
                parkingArea.setSpecificParkingAreaUsage((SpecificParkingAreaUsageEnumeration) input.get("specificParkingAreaUsage"));
            }

        }
        parkingArea.setTotalCapacity((BigInteger) input.get(TOTAL_CAPACITY));
        return parkingArea;
    }
}
