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
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.locationtech.jts.geom.Point;
import org.rutebanken.helper.organisation.ReflectionAuthorizationService;
import org.rutebanken.tiamat.auth.UsernameFetcher;
import org.rutebanken.tiamat.changelog.LoggingService;
import org.rutebanken.tiamat.importer.merging.MergingUtils;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.ParkingRepository;
import org.rutebanken.tiamat.rest.graphql.mappers.GeometryMapper;
import org.rutebanken.tiamat.rest.graphql.mappers.GroupOfEntitiesMapper;
import org.rutebanken.tiamat.rest.graphql.mappers.ValidBetweenMapper;
import org.rutebanken.tiamat.service.parking.ParkingInseeCodeResolver;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.ParkingVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.rutebanken.helper.organisation.AuthorizationConstants.ROLE_EDIT_STOPS;
import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;

@Service("parkingUpdater")
@Transactional
class ParkingUpdater implements DataFetcher<List<Parking>> {

    private static final Logger logger = LoggerFactory.getLogger(ParkingUpdater.class);

    private final ParkingRepository parkingRepository;
    private final ParkingVersionedSaverService parkingVersionedSaverService;
    private final GeometryMapper geometryMapper;
    private final ReflectionAuthorizationService authorizationService;
    private final ValidBetweenMapper validBetweenMapper;
    private final VersionCreator versionCreator;
    private final GroupOfEntitiesMapper groupOfEntitiesMapper;
    private final UsernameFetcher usernameFetcher;
    private final LoggingService loggingService;
    private final ParkingInseeCodeResolver parkingInseeCodeResolver;

    ParkingUpdater(ParkingRepository parkingRepository,
                   ParkingVersionedSaverService parkingVersionedSaverService,
                   GeometryMapper geometryMapper,
                   ReflectionAuthorizationService authorizationService,
                   ValidBetweenMapper validBetweenMapper,
                   VersionCreator versionCreator,
                   GroupOfEntitiesMapper groupOfEntitiesMapper,
                   UsernameFetcher usernameFetcher,
                   LoggingService loggingService,
                   ParkingInseeCodeResolver parkingInseeCodeResolver) {
        this.parkingRepository = parkingRepository;
        this.parkingVersionedSaverService = parkingVersionedSaverService;
        this.geometryMapper = geometryMapper;
        this.authorizationService = authorizationService;
        this.validBetweenMapper = validBetweenMapper;
        this.versionCreator = versionCreator;
        this.groupOfEntitiesMapper = groupOfEntitiesMapper;
        this.usernameFetcher = usernameFetcher;
        this.loggingService = loggingService;
        this.parkingInseeCodeResolver = parkingInseeCodeResolver;
    }

    @Override
    public List<Parking> get(DataFetchingEnvironment environment) {
        List<Map<String, Object>> input = environment.getArgument(OUTPUT_TYPE_PARKING);
        if (input == null) {
            return null;
        }
        return input.stream()
                .map(this::createOrUpdateParking)
                .collect(Collectors.toList());
    }

    private static <T> T get(Map<String, Object> input, String key) {
        return (T) input.get(key);
    }

    private static List<Map<String, Object>> getList(Map<String, Object> input, String key) {
        return Objects.requireNonNullElse(get(input, key), List.of());
    }

    private Parking createOrUpdateParking(Map<String, Object> input) {
        Parking updatedParking;
        Parking existingVersion = null;
        String netexId = get(input, ID);
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

        String user = usernameFetcher.getUserNameForAuthenticatedUser();

        if (isUpdated) {
            authorizationService.assertAuthorized(ROLE_EDIT_STOPS, Arrays.asList(existingVersion, updatedParking));

            logger.info("Saving new version of parking {}", updatedParking);
            updatedParking = parkingVersionedSaverService.saveNewVersion(updatedParking);

            logParking(existingVersion, user, updatedParking);

            return updatedParking;
        } else {
            logger.info("No changes - Parking {} NOT updated", netexId);
        }

        logParking(existingVersion, user, updatedParking);

        return existingVersion;
    }

    private void logParking(Parking existingVersion, String user, Parking updatedParking) {
        if (existingVersion != null) {
            loggingService.logParkingUpdate(user, existingVersion, updatedParking);
        } else {
            loggingService.logParkingCreation(user, updatedParking);
        }
    }

    private boolean populateParking(Map<String, Object> input, Parking updatedParking) {
        boolean isUpdated = false;
        EmbeddableMultilingualString nameBefore = updatedParking.getName();
        Map<String, Set<String>> keyValuesBefore = keyValuesSnapshot(updatedParking);
        if (input.get(VALID_BETWEEN) != null) {
            updatedParking.setValidBetween(validBetweenMapper.map(get(input, VALID_BETWEEN)));
            isUpdated = true;
        }

        if (input.get(GEOMETRY) != null) {
            Point geoJsonPoint = geometryMapper.createGeoJsonPoint(get(input, GEOMETRY));
            boolean centroidChanged = !geoJsonPoint.equals(updatedParking.getCentroid());
            isUpdated = isUpdated || centroidChanged;
            updatedParking.setCentroid(geoJsonPoint);

            // Remote calls inside the GraphQL transaction: only for new or moved parkings, the get_missing_inseecode batch fills the gaps
            if (centroidChanged) {
                try {
                    parkingInseeCodeResolver.resolve(geoJsonPoint.getX(), geoJsonPoint.getY()).ifPresent(updatedParking::setInsee);
                } catch (Exception e) {
                    logger.error("Unable to get insee code for parking {}", updatedParking.getNetexId(), e);
                }
            }
        }

        if (input.get(PARENT_SITE_REF) != null) {
            SiteRefStructure parentSiteRef = new SiteRefStructure();
            parentSiteRef.setRef(get(input, PARENT_SITE_REF));

            isUpdated = isUpdated || (!parentSiteRef.equals(updatedParking.getParentSiteRef()));

            updatedParking.setParentSiteRef(parentSiteRef);
        }

        if (input.get(TOTAL_CAPACITY) != null) {
            BigInteger totalCapacity = get(input, TOTAL_CAPACITY);
            isUpdated = isUpdated || (!totalCapacity.equals(updatedParking.getTotalCapacity()));

            updatedParking.setTotalCapacity(totalCapacity);
        }

        if (input.get(PRINCIPAL_CAPACITY) != null) {
            BigInteger principalCapacity = get(input, PRINCIPAL_CAPACITY);
            isUpdated = isUpdated || (!principalCapacity.equals(updatedParking.getPrincipalCapacity()));

            updatedParking.setPrincipalCapacity(principalCapacity);
        }

        if (input.get(PARKING_TYPE) != null) {
            ParkingTypeEnumeration parkingType = get(input, PARKING_TYPE);
            isUpdated = isUpdated || (!parkingType.equals(updatedParking.getParkingType()));
            updatedParking.setParkingType(parkingType);
        }

        if (input.get(PARKING_VEHICLE_TYPES) != null) {
            List<ParkingVehicleEnumeration> vehicleTypes = get(input, PARKING_VEHICLE_TYPES);
            isUpdated = isUpdated || !(updatedParking.getParkingVehicleTypes().containsAll(vehicleTypes) &&
                    vehicleTypes.containsAll(updatedParking.getParkingVehicleTypes()));

            updatedParking.getParkingVehicleTypes().clear();
            updatedParking.getParkingVehicleTypes().addAll(vehicleTypes);
        }

        if (input.get(PARKING_LAYOUT) != null) {
            ParkingLayoutEnumeration parkingLayout = get(input, PARKING_LAYOUT);
            isUpdated = isUpdated || (!parkingLayout.equals(updatedParking.getParkingLayout()));
            updatedParking.setParkingLayout(parkingLayout);
        }

        if (input.get(OVERNIGHT_PARKING_PERMITTED) != null) {
            Boolean overnightParkingPermitted = get(input, OVERNIGHT_PARKING_PERMITTED);
            isUpdated = isUpdated || (!overnightParkingPermitted.equals(updatedParking.isOvernightParkingPermitted()));
            updatedParking.setOvernightParkingPermitted(overnightParkingPermitted);
        }

        if (input.get(RECHARGING_AVAILABLE) != null) {
            Boolean rechargingAvailable = get(input, RECHARGING_AVAILABLE);
            isUpdated = isUpdated || (!rechargingAvailable.equals(updatedParking.isRechargingAvailable()));
            updatedParking.setRechargingAvailable(rechargingAvailable);
        }

        if (input.get(CARPOOLING_AVAILABLE) != null) {
            Boolean carpoolingAvailable = get(input, CARPOOLING_AVAILABLE);
            isUpdated = isUpdated || (!carpoolingAvailable.equals(updatedParking.isCarpoolingAvailable()));
            updatedParking.setCarpoolingAvailable(carpoolingAvailable);
        }

        if (input.get(CARSHARING_AVAILABLE) != null) {
            Boolean carsharingAvailable = get(input, CARSHARING_AVAILABLE);
            isUpdated = isUpdated || (!carsharingAvailable.equals(updatedParking.isCarsharingAvailable()));
            updatedParking.setCarsharingAvailable(carsharingAvailable);
        }

        if (input.get(SECURE) != null) {
            Boolean isSecure = get(input, SECURE);
            isUpdated = isUpdated || (!isSecure.equals(updatedParking.isSecure()));
            updatedParking.setSecure(isSecure);
        }

        if (input.get(REAL_TIME_OCCUPANCY_AVAILABLE) != null) {
            Boolean isRealtimeOccupancyAvailable = get(input, REAL_TIME_OCCUPANCY_AVAILABLE);
            isUpdated = isUpdated || (!isRealtimeOccupancyAvailable.equals(updatedParking.isRealTimeOccupancyAvailable()));
            updatedParking.setRealTimeOccupancyAvailable(isRealtimeOccupancyAvailable);
        }

        if (input.get(FREE_PARKING_OUT_OF_HOURS) != null) {
            Boolean freeParkingOutOfHours = get(input, FREE_PARKING_OUT_OF_HOURS);
            isUpdated = isUpdated || (!freeParkingOutOfHours.equals(updatedParking.isFreeParkingOutOfHours()));
            updatedParking.setFreeParkingOutOfHours(freeParkingOutOfHours);
        }

        if (input.get(PARKING_PAYMENT_PROCESS) != null) {
            List<ParkingPaymentProcessEnumeration> parkingPaymentProcessTypes = get(input, PARKING_PAYMENT_PROCESS);
            isUpdated = isUpdated || !(updatedParking.getParkingPaymentProcess().containsAll(parkingPaymentProcessTypes) &&
                    parkingPaymentProcessTypes.containsAll(updatedParking.getParkingPaymentProcess()));

            updatedParking.getParkingPaymentProcess().clear();
            updatedParking.getParkingPaymentProcess().addAll(parkingPaymentProcessTypes);
        }

        if (input.get(PARKING_RESERVATION) != null) {
            ParkingReservationEnumeration parkingReservation = get(input, PARKING_RESERVATION);
            isUpdated = isUpdated || (!parkingReservation.equals(updatedParking.getParkingReservation()));
            updatedParking.setParkingReservation(parkingReservation);
        }

        if (input.get(BOOKING_URL) != null) {
            String bookingUrl = get(input, BOOKING_URL);
            isUpdated = isUpdated || (!bookingUrl.equals(updatedParking.getBookingUrl()));
            updatedParking.setBookingUrl(bookingUrl);
        }

        if (input.get(PARKING_PROPERTIES) != null) {
            List<ParkingProperties> existingParkingProperties = updatedParking.getParkingProperties();
            List<ParkingProperties> parkingPropertiesList = resolveParkingPropertiesList(getList(input, PARKING_PROPERTIES), existingParkingProperties);
            int totalCapacity = parkingPropertiesList.stream()
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

            if (newPmrCapacity > totalCapacity) {
                throw new IllegalArgumentException("La capacité PMR ne peut pas être supérieure à la capacité totale");
            }

            if (newCarsharingCapacity > totalCapacity) {
                throw new IllegalArgumentException("La capacité autopartage ne peut pas être supérieure à la capacité totale");
            }

            List<ParkingArea> existingParkingAreasBeforePropertiesUpdate = updatedParking.getParkingAreas();
            if (existingParkingAreasBeforePropertiesUpdate != null) {
                List<ParkingArea> parkingAreaList = new ArrayList<>();
                for (ParkingArea pa : existingParkingAreasBeforePropertiesUpdate) {
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
                    } else if (pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.NONE)) {
                        pa.setTotalCapacity(BigInteger.valueOf(totalCapacity));
                    }

                    if (toKeep) {
                        parkingAreaList.add(versionCreator.createCopy(pa, ParkingArea.class));
                    }
                }

                if (!parkingAreasEqual(existingParkingAreasBeforePropertiesUpdate, parkingAreaList)) {
                    isUpdated = true;
                }
                updatedParking.setParkingAreas(parkingAreaList);
            }

            if (!parkingPropertiesListsEqual(existingParkingProperties, parkingPropertiesList)) {
                isUpdated = true;
            }
            updatedParking.setParkingProperties(parkingPropertiesList);

            if (totalCapacity > 0) {
                BigInteger newTotalCapacity = BigInteger.valueOf(totalCapacity);
                if (!newTotalCapacity.equals(updatedParking.getTotalCapacity())) {
                    isUpdated = true;
                }
                updatedParking.setTotalCapacity(newTotalCapacity);
            }
        }

        if (input.get(PARKING_AREAS) != null) {
            List<ParkingArea> existingParkingAreas = updatedParking.getParkingAreas();
            List<ParkingArea> parkingAreasList = resolveParkingAreasList(getList(input, PARKING_AREAS), existingParkingAreas);

            int newNoneCapacity = parkingAreasList.stream()
                    .filter(pa -> pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.NONE))
                    .mapToInt(pa -> pa.getTotalCapacity() != null ? pa.getTotalCapacity().intValue() : 0)
                    .sum();

            int totalCapacity = newNoneCapacity != 0 ? newNoneCapacity : (updatedParking.getTotalCapacity() != null ? updatedParking.getTotalCapacity().intValue() : 0);

            int newCarpoolingCapacity = parkingAreasList.stream()
                    .filter(pa -> pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.CARPOOL))
                    .mapToInt(pa -> pa.getTotalCapacity() != null ? pa.getTotalCapacity().intValue() : 0)
                    .sum();

            int newCarshareCapacity = parkingAreasList.stream()
                    .filter(pa -> pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.CARSHARE))
                    .mapToInt(pa -> pa.getTotalCapacity() != null ? pa.getTotalCapacity().intValue() : 0)
                    .sum();

            int newDisabledCapacity = parkingAreasList.stream()
                    .filter(pa -> pa.getSpecificParkingAreaUsage().equals(SpecificParkingAreaUsageEnumeration.DISABLED))
                    .mapToInt(pa -> pa.getTotalCapacity() != null ? pa.getTotalCapacity().intValue() : 0)
                    .sum();

            if (newCarpoolingCapacity > totalCapacity) {
                throw new IllegalArgumentException("La capacité covoiturage ne peut pas être supérieure à la capacité totale");
            }
            if (newCarshareCapacity > totalCapacity) {
                throw new IllegalArgumentException("La capacité d'autopartage ne peut pas être supérieure à la capacité totale");
            }
            if (newDisabledCapacity > totalCapacity) {
                throw new IllegalArgumentException("La capacité du nombre de places réservées aux personnes handicapées ne peut pas être supérieure à la capacité totale");
            }

            List<ParkingArea> copiedParkingAreas = parkingAreasList.stream().map(pa -> versionCreator.createCopy(pa, ParkingArea.class)).collect(Collectors.toList());
            if (!parkingAreasEqual(existingParkingAreas, copiedParkingAreas)) {
                isUpdated = true;
            }
            updatedParking.setParkingAreas(copiedParkingAreas);
        }

        if (input.get(COVERED) != null) {
            CoveredEnumeration parkingCovered = get(input, COVERED);
            isUpdated = isUpdated || (!parkingCovered.equals(updatedParking.getCovered()));
            updatedParking.setCovered(parkingCovered);
        }

        if (input.get(TYPE_OF_PARKING_REF) != null) {
            TypeOfParkingRefEnumeration parkingTypeOfParkingRef = get(input, TYPE_OF_PARKING_REF);
            isUpdated = isUpdated || (!parkingTypeOfParkingRef.value().equals(updatedParking.getParkingTypeRef()));
            updatedParking.setTypeOfParkingRef(parkingTypeOfParkingRef.value());
        }

        if (input.get(OPERATOR) != null) {
            String operator = get(input, OPERATOR);
            isUpdated = isUpdated || !operator.equals(updatedParking.getOperator());
            updatedParking.setOperator(operator);
        }

        if (input.get(ADDRESS) != null) {
            String address = get(input, ADDRESS);
            isUpdated = isUpdated || !address.equals(updatedParking.getAddress());
            updatedParking.setAddress(address);
        }

        if (input.get(POSTAL_CODE) != null || input.get(CITY) != null) {
            if (updatedParking.getPostalAddress() == null) {
                updatedParking.setPostalAddress(new PostalAddress());
            }
            PostalAddress postalAddress = updatedParking.getPostalAddress();
            if (input.get(POSTAL_CODE) != null) {
                String postCode = get(input, POSTAL_CODE);
                isUpdated = isUpdated || !postCode.equals(postalAddress.getPostCode());
                postalAddress.setPostCode(postCode);
            }
            if (input.get(CITY) != null) {
                String town = get(input, CITY);
                isUpdated = isUpdated || !town.equals(postalAddress.getTown());
                postalAddress.setTown(town);
            }
        }

        if (input.get(AVAILABILITY_CONDITIONS) != null) {
            Set<AvailabilityCondition> availabilityConditions = mapAvailabilityConditions(getList(input, AVAILABILITY_CONDITIONS));
            if (!MergingUtils.availabilityConditionsSignature(availabilityConditions)
                    .equals(MergingUtils.availabilityConditionsSignature(updatedParking.getAvailabilityConditions()))) {
                updatedParking.setAvailabilityConditions(availabilityConditions);
                isUpdated = true;
            }
        }

        boolean groupOfEntitiesUpdated = groupOfEntitiesMapper.populate(input, updatedParking);
        // name, keyValues and geometry are always flagged as updated by the shared mappers: compare real values instead
        if (groupOfEntitiesUpdated && onlyNameKeyValuesOrGeometry(input)) {
            groupOfEntitiesUpdated = !Objects.equals(nameBefore, updatedParking.getName())
                    || !keyValuesBefore.equals(keyValuesSnapshot(updatedParking));
        }
        isUpdated = isUpdated || groupOfEntitiesUpdated;

        return isUpdated;
    }

    private boolean onlyNameKeyValuesOrGeometry(Map<String, Object> input) {
        return Stream.of(SHORT_NAME, DESCRIPTION, VERSION_COMMENT, ALTERNATIVE_NAMES, ACCESSIBILITY_ASSESSMENT, PLACE_EQUIPMENTS)
                .allMatch(key -> input.get(key) == null);
    }

    private Map<String, Set<String>> keyValuesSnapshot(Parking parking) {
        Map<String, Set<String>> snapshot = new HashMap<>();
        parking.getKeyValues().forEach((key, value) -> {
            if (value != null && !value.getItems().isEmpty()) {
                snapshot.put(key, new HashSet<>(value.getItems()));
            }
        });
        return snapshot;
    }

    private Set<AvailabilityCondition> mapAvailabilityConditions(List<Map<String, Object>> input) {
        Set<AvailabilityCondition> availabilityConditions = new HashSet<>();
        for (Map<String, Object> availabilityConditionInput : input) {
            AvailabilityCondition availabilityCondition = new AvailabilityCondition();
            availabilityCondition.setAvailable(Boolean.TRUE.equals(availabilityConditionInput.get(IS_AVAILABLE)));
            for (Map<String, Object> dayTypeInput : getList(availabilityConditionInput, DAY_TYPES)) {
                DayType dayType = new DayType();
                dayType.setDaysOfWeek(new ArrayList<>(Objects.<List<DayOfWeekEnumeration>>requireNonNullElse(get(dayTypeInput, DAYS_OF_WEEK), List.of())));
                for (Map<String, Object> timeBandInput : getList(dayTypeInput, TIME_BANDS)) {
                    TimeBand timeBand = new TimeBand();
                    if (timeBandInput.get(START_TIME) != null) {
                        timeBand.setStartTime(LocalTime.parse(get(timeBandInput, START_TIME)));
                    }
                    if (timeBandInput.get(END_TIME) != null) {
                        timeBand.setEndTime(LocalTime.parse(get(timeBandInput, END_TIME)));
                    }
                    timeBand.setDayOffset(get(timeBandInput, DAY_OFFSET));
                    dayType.getTimeBand().add(timeBand);
                }
                availabilityCondition.getDayTypes().add(dayType);
            }
            availabilityConditions.add(availabilityCondition);
        }
        return availabilityConditions;
    }

    private List<ParkingProperties> resolveParkingPropertiesList(List<Map<String, Object>> propertyList, List<ParkingProperties> existingParkingPropertiesList) {
        List<ParkingProperties> result = new ArrayList<>();
        for (int i = 0; i < propertyList.size(); i++) {
            ParkingProperties existing = existingParkingPropertiesList != null && i < existingParkingPropertiesList.size()
                    ? existingParkingPropertiesList.get(i)
                    : null;
            result.add(resolveSingleParkingProperties(propertyList.get(i), existing));
        }

        return result;
    }

    private ParkingProperties resolveSingleParkingProperties(Map<String, Object> input, ParkingProperties existing) {
        ParkingProperties p = existing != null ? versionCreator.createCopy(existing, ParkingProperties.class) : new ParkingProperties();
        p.setSpaces(resolveParkingCapacities(get(input, SPACES), existing != null ? existing.getSpaces() : null));
        return p;
    }

    private List<ParkingCapacity> resolveParkingCapacities(List<Map<String, Object>> input, List<ParkingCapacity> existingSpaces) {
        List<ParkingCapacity> result = new ArrayList<>();
        List<ParkingCapacity> remainingExistingSpaces = existingSpaces != null ? new ArrayList<>(existingSpaces) : new ArrayList<>();

        for (Map<String, Object> property : input) {
            result.add(resolveSingleParkingCapacity(property, remainingExistingSpaces));
        }

        remainingExistingSpaces.stream()
                .map(s -> versionCreator.createCopy(s, ParkingCapacity.class))
                .forEach(result::add);

        return result;
    }

    private ParkingCapacity resolveSingleParkingCapacity(Map<String, Object> input, List<ParkingCapacity> remainingExistingSpaces) {
        ParkingUserEnumeration parkingUserType = get(input, PARKING_USER_TYPE);

        ParkingCapacity existing = remainingExistingSpaces.stream()
                .filter(s -> parkingUserType != null && parkingUserType.equals(s.getParkingUserType()))
                .findFirst()
                .orElse(null);
        remainingExistingSpaces.remove(existing);

        ParkingCapacity capacity = existing != null ? versionCreator.createCopy(existing, ParkingCapacity.class) : new ParkingCapacity();
        capacity.setParkingUserType(parkingUserType);
        capacity.setParkingVehicleType(get(input, PARKING_VEHICLE_TYPE));
        capacity.setParkingStayType(get(input, PARKING_STAY_TYPE));
        capacity.setNumberOfSpaces(get(input, NUMBER_OF_SPACES));
        capacity.setNumberOfSpacesWithRechargePoint(get(input, NUMBER_OF_SPACES_WITH_RECHARGE_POINT));
        if (capacity.getNumberOfSpacesWithRechargePoint() != null && capacity.getNumberOfSpacesWithRechargePoint().compareTo(capacity.getNumberOfSpaces()) > 0) {
            throw new IllegalArgumentException("Le nombre de places équipées de bornes de recharge ne peut pas être supérieur à la capacité totale");
        }
        capacity.setNumberOfCarsharingSpaces(get(input, NUMBER_OF_CARSHARING_SPACES));
        return capacity;
    }

    private List<ParkingArea> resolveParkingAreasList(List<Map<String, Object>> list, List<ParkingArea> existingParkingAreas) {
        List<ParkingArea> result = new ArrayList<>();
        Set<SpecificParkingAreaUsageEnumeration> newUsages = new HashSet<>();
        for (Map<String, Object> property : list) {
            ParkingArea newArea = createParkingAreaFromInputParameters(property);
            result.add(newArea);
            newUsages.add(newArea.getSpecificParkingAreaUsage());
        }

        if (existingParkingAreas != null) {
            result.addAll(existingParkingAreas.stream().filter(pa -> !newUsages.contains(pa.getSpecificParkingAreaUsage())).collect(Collectors.toList()));
        }

        return result;
    }

    private boolean parkingPropertiesListsEqual(List<ParkingProperties> before, List<ParkingProperties> after) {
        List<ParkingProperties> safeBefore = Collections.emptyList();
        if (before != null) {
            safeBefore = before;
        }
        if (safeBefore.size() != after.size()) {
            return false;
        }
        for (int i = 0; i < after.size(); i++) {
            if (!parkingCapacitiesEqual(safeBefore.get(i).getSpaces(), after.get(i).getSpaces())) {
                return false;
            }
        }
        return true;
    }

    private boolean parkingCapacitiesEqual(List<ParkingCapacity> before, List<ParkingCapacity> after) {
        return toComparableCapacities(before).equals(toComparableCapacities(after));
    }

    private List<String> toComparableCapacities(List<ParkingCapacity> spaces) {
        if (spaces == null) {
            return Collections.emptyList();
        }
        return spaces.stream()
                .map(c -> c.getParkingUserType() + "|" + c.getParkingVehicleType() + "|"
                        + c.getParkingStayType() + "|" + c.getNumberOfSpaces() + "|"
                        + c.getNumberOfSpacesWithRechargePoint() + "|" + c.getNumberOfCarsharingSpaces())
                .sorted()
                .collect(Collectors.toList());
    }

    private boolean parkingAreasEqual(List<ParkingArea> before, List<ParkingArea> after) {
        return toComparableAreas(before)
                .equals(toComparableAreas(after));
    }

    private List<String> toComparableAreas(List<ParkingArea> areas) {
        if (areas == null) {
            return Collections.emptyList();
        }
        return areas.stream()
                .map(a -> a.getNetexId() + "|" + a.getSpecificParkingAreaUsage() + "|" + a.getTotalCapacity())
                .sorted()
                .collect(Collectors.toList());
    }

    private ParkingArea createParkingAreaFromInputParameters(Map<String, Object> inputParams) {
        ParkingArea newParkingArea = new ParkingArea();
        if (inputParams.get("specificParkingAreaUsage") != null) {
            newParkingArea.setSpecificParkingAreaUsage(get(inputParams, "specificParkingAreaUsage"));
        }
        newParkingArea.setTotalCapacity(get(inputParams, TOTAL_CAPACITY));
        return newParkingArea;
    }


}
