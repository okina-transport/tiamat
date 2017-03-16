package org.rutebanken.tiamat.rest.graphql;

import com.google.api.client.util.Preconditions;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.pelias.CountyAndMunicipalityLookupService;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.rest.graphql.resolver.GeometryResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;

@Service("stopPlaceUpdater")
@Transactional
class StopPlaceUpdater implements DataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceUpdater.class);

    @Autowired
    private StopPlaceRepository stopPlaceRepository;

    @Autowired
    private QuayRepository quayRepository;

    @Autowired
    private GeometryResolver geometryResolver;

    @Autowired
    private CountyAndMunicipalityLookupService countyAndMunicipalityLookupService;

    private static AtomicInteger createdTopographicPlaceCounter = new AtomicInteger();


    @Override
    public Object get(DataFetchingEnvironment environment) {
        List<Field> fields = environment.getFields();
        StopPlace stopPlace = null;
        for (Field field : fields) {
            if (field.getName().equals(MUTATE_STOPPLACE)) {
                stopPlace = createOrUpdateStopPlace(environment);
            }
        }
        return Arrays.asList(stopPlace);
    }

    private StopPlace createOrUpdateStopPlace(DataFetchingEnvironment environment) {
        StopPlace stopPlace = null;
        if (environment.getArgument(OUTPUT_TYPE_STOPPLACE) != null) {
            Map input = environment.getArgument(OUTPUT_TYPE_STOPPLACE);

            String netexId = (String) input.get(ID);
            if (netexId != null) {
                logger.info("Updating StopPlace {}", netexId);
                stopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(netexId);

                Preconditions.checkArgument(stopPlace != null, "Attempting to update StopPlace [id = %s], but StopPlace does not exist.", netexId);

            } else {
                logger.info("Creating new StopPlace");
                stopPlace = new StopPlace();
                stopPlace.setCreated(ZonedDateTime.now());
            }

            if (stopPlace != null) {
                boolean hasValuesChanged = populateStopPlaceFromInput(input, stopPlace);

                if (hasValuesChanged) {
                    if (stopPlace.getQuays() != null) {
                        /*
                         * Explicitly saving new Quays  when updating and creating new Quays in the same request.
                         * Already existing quays are attempted to be inserted causing ConstraintViolationException.
                         *
                         * It is necessary to call saveAndFlush(quay) to enforce database-constraints and updating
                         * references on StopPlace-object.
                         *
                         */
                        stopPlace.getQuays().stream()
                                .filter(quay -> quay.getNetexId() == null)
                                .forEach(quay -> quayRepository.saveAndFlush(quay));
                    }
                    stopPlace.setChanged(ZonedDateTime.now());
                    stopPlace = stopPlaceRepository.save(stopPlace);

                }
            }
        }
        return stopPlace;
    }

    /**
     *
     * @param input
     * @param stopPlace
     * @return true if StopPlace or any og the attached Quays are updated
     */
    private boolean populateStopPlaceFromInput(Map input, StopPlace stopPlace) {
        boolean isUpdated = populate(input, stopPlace);

        if (input.get(STOP_PLACE_TYPE) != null) {
            stopPlace.setStopPlaceType((StopTypeEnumeration) input.get(STOP_PLACE_TYPE));
            isUpdated = true;
        }

        if (input.get(QUAYS) != null) {
            List quays = (List) input.get(QUAYS);
            for (Object quayObject : quays) {

                Map quayInputMap = (Map) quayObject;
                if (populateQuayFromInput(stopPlace, quayInputMap)) {
                    isUpdated = true;
                } else {
                    logger.info("Quay not changed");
                }
            }
        }
        if (isUpdated) {
            stopPlace.setChanged(ZonedDateTime.now());
        }

        return isUpdated;
    }

    private boolean populateQuayFromInput(StopPlace stopPlace, Map quayInputMap) {
        Quay quay;
        if (quayInputMap.get(ID) != null) {
            Optional<Quay> existingQuay = stopPlace.getQuays().stream()
                    .filter(q -> q.getNetexId() != null)
                    .filter(q -> q.getNetexId().equals(quayInputMap.get(ID))).findFirst();

            Preconditions.checkArgument(existingQuay.isPresent(),
                    "Attempting to update Quay [id = %s] on StopPlace [id = %s] , but Quay does not exist on StopPlace",
                    quayInputMap.get(ID),
                    stopPlace.getNetexId());

            quay = existingQuay.get();
            logger.info("Updating Quay {} for StopPlace {}", quay.getNetexId(), stopPlace.getNetexId());
        } else {
            quay = new Quay();
            quay.setCreated(ZonedDateTime.now());

            logger.info("Creating new Quay");
        }
        boolean isQuayUpdated = populate(quayInputMap, quay);

        if (quayInputMap.get(COMPASS_BEARING) != null) {
            quay.setCompassBearing(((BigDecimal) quayInputMap.get(COMPASS_BEARING)).floatValue());
            isQuayUpdated = true;
        }
        if (quayInputMap.get(PUBLIC_CODE) != null) {
            quay.setPublicCode((String) quayInputMap.get(PUBLIC_CODE));
            isQuayUpdated = true;
        }

        if (isQuayUpdated) {
            quay.setChanged(ZonedDateTime.now());

            if (quay.getNetexId() == null) {
                stopPlace.getQuays().add(quay);
            }
        }
        return isQuayUpdated;
    }

    private boolean populate(Map input, SiteElement entity) {
        boolean isUpdated = false;

        if (input.get(NAME) != null) {
            entity.setName(getEmbeddableString((Map) input.get(NAME)));
            isUpdated = true;
        }
        if (input.get(SHORT_NAME) != null) {
            entity.setShortName(getEmbeddableString((Map) input.get(SHORT_NAME)));
            isUpdated = true;
        }
        if (input.get(DESCRIPTION) != null) {
            entity.setDescription(getEmbeddableString((Map) input.get(DESCRIPTION)));
            isUpdated = true;
        }
        if (input.get(ALL_AREAS_WHEELCHAIR_ACCESSIBLE) != null) {
            entity.setAllAreasWheelchairAccessible((Boolean) input.get(ALL_AREAS_WHEELCHAIR_ACCESSIBLE));
            isUpdated = true;
        }
        if (input.get(GEOMETRY) != null) {
            entity.setCentroid(geometryResolver.createGeoJsonPoint((Map) input.get(GEOMETRY)));

            if (entity instanceof StopPlace) {
                try {
                    countyAndMunicipalityLookupService.populateCountyAndMunicipality((StopPlace) entity, createdTopographicPlaceCounter);
                } catch (Exception e) {
                    logger.warn("Setting TopographicPlace on StopPlace failed", e);
                }
            }

            
            isUpdated = true;
        }
        return isUpdated;
    }

    private EmbeddableMultilingualString getEmbeddableString(Map map) {
        return new EmbeddableMultilingualString((String) map.get(VALUE), (String) map.get(LANG));
    }

}
