package org.rutebanken.tiamat.rest.graphql.fetchers;

import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.rutebanken.tiamat.model.StopTypeEnumeration;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.rutebanken.tiamat.rest.graphql.GraphQLNames.*;

public final class StopPlaceSearchArgumentsMapper {

    private StopPlaceSearchArgumentsMapper() {
    }

    public static Boolean applySearchFlags(Map<String, Object> arguments, StopPlaceSearch.Builder stopPlaceSearchBuilder) {
        Boolean allVersions = setIfNonNull(arguments, ALL_VERSIONS, stopPlaceSearchBuilder::setAllVersions);
        setIfNonNull(arguments, WITHOUT_LOCATION_ONLY, stopPlaceSearchBuilder::setWithoutLocationOnly);
        setIfNonNull(arguments, WITHOUT_QUAYS_ONLY, stopPlaceSearchBuilder::setWithoutQuaysOnly);
        setIfNonNull(arguments, WITH_DUPLICATED_QUAY_IMPORTED_IDS, stopPlaceSearchBuilder::setWithDuplicatedQuayImportedIds);
        setIfNonNull(arguments, WITH_NEARBY_SIMILAR_DUPLICATES, stopPlaceSearchBuilder::setWithNearbySimilarDuplicates);
        setIfNonNull(arguments, STOP_PLACES_WITHOUT_QUAY, stopPlaceSearchBuilder::setStopPlacesWithoutQuay);
        setIfNonNull(arguments, NEARBY_STOP_PLACES, stopPlaceSearchBuilder::setNearbyStopPlaces);
        setIfNonNull(arguments, NEARBY_RADIUS, stopPlaceSearchBuilder::setNearbyRadius);
        setIfNonNull(arguments, ORGANISATION_NAME, stopPlaceSearchBuilder::setOrganisationName);
        setIfNonNull(arguments, WITH_DISTANT_QUAYS, stopPlaceSearchBuilder::setWithDistantQuays);
        setIfNonNull(arguments, DETECT_MULTI_MODAL_POINTS, stopPlaceSearchBuilder::setDetectMultiModalPoints);
        setIfNonNull(arguments, HAS_PARKING, stopPlaceSearchBuilder::setHasParking);
        setIfNonNull(arguments, WITH_TAGS, stopPlaceSearchBuilder::setWithTags);
        setIfNonNull(arguments, STOP_PLACES_WITH_MULTIPLE_PRODUCERS, stopPlaceSearchBuilder::setStopPlacesWithMultipleProducers);
        setIfNonNull(arguments, QUAYS_WITH_MULTIPLE_PRODUCERS, stopPlaceSearchBuilder::setQuaysWithMultipleProducers);

        if (arguments.get(VERSION_VALIDITY_ARG) != null) {
            ExportParams.VersionValidity versionValidity = ExportParams.VersionValidity.valueOf(ExportParams.VersionValidity.class, (String) arguments.get(VERSION_VALIDITY_ARG));
            stopPlaceSearchBuilder.setVersionValidity(versionValidity);
        }
        return allVersions;
    }

    public static Instant getPointInTime(Map<String, Object> arguments) {
        return (Instant) arguments.get(POINT_IN_TIME);
    }

    @SuppressWarnings("unchecked")
    public static void applyStandardFilters(Map<String, Object> arguments, Boolean allVersions, Instant pointInTime,
                                            ExportParams.Builder exportParamsBuilder, StopPlaceSearch.Builder stopPlaceSearchBuilder) {
        if (allVersions == null || !allVersions) {
            stopPlaceSearchBuilder.setPointInTime(pointInTime);
        }

        List<StopTypeEnumeration> stopTypes = (List<StopTypeEnumeration>) arguments.get(STOP_PLACE_TYPE);
        if (stopTypes != null && !stopTypes.isEmpty()) {
            stopPlaceSearchBuilder.setStopTypeEnumerations(stopTypes.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())
            );
        }

        List<String> countryRef = (List<String>) arguments.get(COUNTRY_REF);
        if (countryRef != null && !countryRef.isEmpty()) {
            exportParamsBuilder.setCountryReferences(
                    countryRef.stream()
                            .filter(countryRefValue -> countryRefValue != null && !countryRefValue.isEmpty())
                            .collect(Collectors.toList())
            );
        }

        List<String> countyRef = (List<String>) arguments.get(COUNTY_REF);
        if (countyRef != null && !countyRef.isEmpty()) {
            exportParamsBuilder.setCountyReferences(
                    countyRef.stream()
                            .filter(countyRefValue -> countyRefValue != null && !countyRefValue.isEmpty())
                            .collect(Collectors.toList())
            );
        }

        List<String> municipalityRef = (List<String>) arguments.get(MUNICIPALITY_REF);
        if (municipalityRef != null && !municipalityRef.isEmpty()) {
            exportParamsBuilder.setMunicipalityReferences(
                    municipalityRef.stream()
                            .filter(municipalityRefValue -> municipalityRefValue != null && !municipalityRefValue.isEmpty())
                            .collect(Collectors.toList())
            );
        }

        if (arguments.get(SEARCH_WITH_CODE_SPACE) != null) {
            String code = (String) arguments.get(SEARCH_WITH_CODE_SPACE);
            exportParamsBuilder.setCodeSpace(code.toLowerCase());
        }

        setIfNonNull(arguments, TAGS, stopPlaceSearchBuilder::setTags);

        stopPlaceSearchBuilder.setQuery((String) arguments.get(QUERY));
    }

    public static boolean shouldResolveParents(Map<String, Object> arguments) {
        return !(isTrue(arguments, NEARBY_STOP_PLACES)
                || isTrue(arguments, ONLY_MONOMODAL_STOPPLACES)
                || isTrue(arguments, STOP_PLACES_WITHOUT_QUAY)
                || isTrue(arguments, STOP_PLACES_WITH_MULTIPLE_PRODUCERS)
                || isTrue(arguments, QUAYS_WITH_MULTIPLE_PRODUCERS));
    }

    private static boolean isTrue(Map<String, Object> arguments, String argumentName) {
        return Boolean.TRUE.equals(arguments.get(argumentName));
    }

    @SuppressWarnings("unchecked")
    private static <T> T setIfNonNull(Map<String, Object> arguments, String argumentName, Consumer<T> consumer) {
        if (arguments.get(argumentName) != null) {
            T value = (T) arguments.get(argumentName);
            consumer.accept(value);
            return value;
        }
        return null;
    }
}
