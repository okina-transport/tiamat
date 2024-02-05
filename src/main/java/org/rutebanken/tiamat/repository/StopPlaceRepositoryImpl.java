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

package org.rutebanken.tiamat.repository;


import com.google.common.collect.Sets;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.hibernate.query.NativeQuery;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.dtoassembling.dto.IdMappingDto;
import org.rutebanken.tiamat.dtoassembling.dto.JbvCodeMappingDto;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.importer.StopPlaceSharingPolicy;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.repository.iterator.ScrollableResultIterator;
import org.rutebanken.tiamat.repository.search.ChangedStopPlaceSearch;
import org.rutebanken.tiamat.repository.search.SearchHelper;
import org.rutebanken.tiamat.repository.search.StopPlaceQueryFromSearchBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.MERGED_ID_KEY;
import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;
import static org.rutebanken.tiamat.repository.QuayRepositoryImpl.JBV_CODE;

@Transactional
public class StopPlaceRepositoryImpl implements StopPlaceRepositoryCustom {

    private static final Logger logger = LoggerFactory.getLogger(StopPlaceRepositoryImpl.class);

    private static final int SCROLL_FETCH_SIZE = 1000;

    private static BasicFormatterImpl basicFormatter = new BasicFormatterImpl();

    @Value("${administration.space.name}")
    protected String administrationSpaceName;

    /**
     * Part of SQL that checks that either the stop place named as *s* or the parent named *p* is valid at the point in time.
     * The parameter "pointInTime" must be set.
     * The parent stop must be joined in as 'p' to allow checking the validity.
     */
    protected static final String SQL_STOP_PLACE_OR_PARENT_IS_VALID_AT_POINT_IN_TIME =
            " ((p.netex_id IS NOT NULL AND (p.from_date IS NULL OR p.from_date <= :pointInTime) AND (p.to_date IS NULL OR p.to_date > :pointInTime))" +
                    "  OR (p.netex_id IS NULL AND (s.from_date IS NULL OR s.from_date <= :pointInTime) AND (s.to_date IS NULL OR s.to_date > :pointInTime))) ";

    /**
     * Part of SQL that checks that either the stop place named as *s* or the parent named *p* is valid within a provided interval
     * The parameters "validFrom" and "validTo" must be set.
     * The parent stop must be joined in as 'p' to allow checking the validity.
     */
    protected static final String SQL_STOP_PLACE_OR_PARENT_IS_VALID_IN_INTERVAL =
            " ((p.netex_id IS NOT NULL AND (p.from_date IS NULL OR p.from_date <= :validTo) AND (p.to_date IS NULL OR p.to_date > :validFrom))" +
                    "  OR (p.netex_id IS NULL AND (s.from_date IS NULL OR s.from_date <= :validTo) AND (s.to_date IS NULL OR s.to_date > :validFrom))) ";

    /**
     * Left join parent stop place p with stop place s on parent site ref and parent site ref version.
     */
    public static final String SQL_LEFT_JOIN_PARENT_STOP =
            createLeftJoinParentStopQuery("p");

    public static final String SQL_LEFT_JOIN_PARENT_STOP_TEMPLATE =
            "LEFT JOIN stop_place %s ON s.parent_site_ref = %s.netex_id AND s.parent_site_ref_version = CAST(%s.version as text) ";

    public static final String SQL_LEFT_JOIN_IMPORTED_ID =
            "LEFT JOIN stop_place_key_values spkv ON s.id = spkv.stop_place_id AND spkv.key_values_key= 'imported-id' " +
                    " LEFT JOIN value_items vi on vi.value_id = spkv.key_values_id ";


    public static String createLeftJoinParentStopQuery(String parentAlias) {
        return String.format(SQL_LEFT_JOIN_PARENT_STOP_TEMPLATE, Collections.nCopies(3, parentAlias).toArray());
    }

    /**
     * When selecting stop places and there are multiple versions of the same stop place, and you only need the highest version by number.
     */
    protected static final String SQL_MAX_VERSION_OF_STOP_PLACE = "s.version = (select max(sv.version) from stop_place sv where sv.netex_id = s.netex_id) ";

    /**
     * Check stop place or it's parent for match in geometry filter.
     */
    protected static final String SQL_CHILD_OR_PARENT_WITHIN = "(ST_within(s.centroid, :filter) = true OR ST_within(p.centroid, :filter) = true) ";

    /**
     * SQL for making sure the stop selected is not a parent stop place.
     */
    public static final String SQL_NOT_PARENT_STOP_PLACE = "s.parent_stop_place = false ";

    /**
     * Ignore netex id for both stop place and its parent
     */
    protected static final String SQL_IGNORE_STOP_PLACE_ID = "(s.netex_id != :ignoreStopPlaceId AND (p.netex_id IS NULL OR p.netex_id != :ignoreStopPlaceId)) ";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private GeometryFactory geometryFactory;

    @Autowired
    private StopPlaceQueryFromSearchBuilder stopPlaceQueryFromSearchBuilder;

    @Autowired
    private SearchHelper searchHelper;

    @Value("${stopPlace.sharing.policy}")
    protected StopPlaceSharingPolicy sharingPolicy;

    /**
     * Find nearby stop places that are valid 'now', specifying a bounding box.
     * Optionally, a stop place ID to ignore can be defined.
     */
    @Override
    public Page<StopPlace> findStopPlacesWithin(double xMin, double yMin, double xMax, double yMax, String ignoreStopPlaceId, Pageable pageable) {
        return findStopPlacesWithin(xMin, yMin, xMax, yMax, ignoreStopPlaceId, Instant.now(), pageable);
    }

    /**
     * Find nearby stop places that are valid at the given point in time, specifying a bounding box.
     * Optionally, a stop place ID to ignore can be defined.
     */
    @Override
    public Page<StopPlace> findStopPlacesWithin(double xMin, double yMin, double xMax, double yMax, String ignoreStopPlaceId, Instant pointInTime, Pageable pageable) {
        Envelope envelope = new Envelope(xMin, xMax, yMin, yMax);

        Geometry geometryFilter = geometryFactory.toGeometry(envelope);


        String queryString;
        if (pointInTime != null) {
        queryString = "SELECT s.* FROM stop_place s " +
                                     SQL_LEFT_JOIN_PARENT_STOP +
                                     "WHERE " +
                                        SQL_CHILD_OR_PARENT_WITHIN +
                                        "AND "
                                        + SQL_NOT_PARENT_STOP_PLACE +
                                        "AND "
                                        + SQL_STOP_PLACE_OR_PARENT_IS_VALID_AT_POINT_IN_TIME;
            if(ignoreStopPlaceId != null) {
                queryString += "AND " + SQL_IGNORE_STOP_PLACE_ID;
            }
        } else {
            // If no point in time is set, use max version to only get one version per stop place
            String subQueryString = "SELECT s.netex_id,max(s.version) FROM stop_place s " +
                    SQL_LEFT_JOIN_PARENT_STOP +
                    "WHERE " +
                    SQL_CHILD_OR_PARENT_WITHIN +
                    "AND "
                    + SQL_NOT_PARENT_STOP_PLACE;


            if (ignoreStopPlaceId != null) {
                subQueryString += "AND " + SQL_IGNORE_STOP_PLACE_ID + "group by s.netex_id";
            } else {
                subQueryString += "group by s.netex_id" ;
            }

            queryString = "SELECT s.* FROM stop_place s " +
                    "WHERE (netex_id,version) in (" + subQueryString + ")" ;

        }

        logger.debug("finding stops within bounding box with query: {}", queryString);

        final Query query = entityManager.createNativeQuery(queryString, StopPlace.class);
        query.setParameter("filter", geometryFilter);

        if(ignoreStopPlaceId != null) {
            query.setParameter("ignoreStopPlaceId", ignoreStopPlaceId);
        }

        if (pointInTime != null) {
            query.setParameter("pointInTime", Date.from(pointInTime));
        }

        query.setFirstResult(Math.toIntExact(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());
        List<StopPlace> stopPlaces = query.getResultList();
        return new PageImpl<>(stopPlaces, pageable, stopPlaces.size());
    }

    /**
     * This query contains a fuzzy similarity check on name.
     *
     * @param envelope            bounding box
     * @param name                name to fuzzy match
     * @param stopTypeEnumeration stop place type
     * @return the stop place within bounding box if equal type, within envelope and closest similarity in name
     */
    @Override
    public String findNearbyStopPlace(Envelope envelope, String name, StopTypeEnumeration stopTypeEnumeration, Provider provider) {
        Geometry geometryFilter = geometryFactory.toGeometry(envelope);

        String sql = "SELECT sub.netex_id FROM " +
                "(SELECT s.netex_id AS netex_id, similarity(s.name_value, :name) AS sim FROM stop_place s " +
                SQL_LEFT_JOIN_PARENT_STOP +
                "WHERE ST_Within(s.centroid, :filter) = true " +
                "AND " + SQL_STOP_PLACE_OR_PARENT_IS_VALID_AT_POINT_IN_TIME +
                "AND s.stop_place_type = :stopPlaceType ";

        if(provider != null){
            sql += "AND s.provider = :providerName";
        }

        sql +=  ") sub " +
                "WHERE sub.sim > 0.6 " +
                "ORDER BY sub.sim DESC LIMIT 1";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("pointInTime", Date.from(Instant.now()));
        query.setParameter("filter", geometryFilter);
        query.setParameter("stopPlaceType", stopTypeEnumeration.toString());
        query.setParameter("name", name);
        if (provider != null){
            query.setParameter("providerName", provider.name);
        }
        return getOneOrNull(query);
    }

    /**
     * Search nearby stopPlace
     *
     * @param envelope            bounding box     *
     * @param stopTypeEnumeration stop place type
     * @param provider provider to search
     * @return the stop place within bounding box if equal type, within envelope and closest similarity in name
     */
    @Override
    public List<String> findNearbyStopPlace(Envelope envelope, StopTypeEnumeration stopTypeEnumeration, Provider provider) {
        Geometry geometryFilter = geometryFactory.toGeometry(envelope);

        String sql ="SELECT s.netex_id FROM stop_place s " +
                SQL_LEFT_JOIN_PARENT_STOP +
                "WHERE ST_within(s.centroid, :filter) = true " +
                "AND " + SQL_STOP_PLACE_OR_PARENT_IS_VALID_AT_POINT_IN_TIME +
                "AND s.stop_place_type = :stopPlaceType ";

        if(provider != null){
            sql += "AND s.provider = :providerName";
        }

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("filter", geometryFilter);
        query.setParameter("stopPlaceType", stopTypeEnumeration.toString());
        query.setParameter("pointInTime", Date.from(Instant.now()));
        if (provider != null){
            query.setParameter("providerName", provider.name);
        }
        try {
            List<String> resultList = query.getResultList();
            return resultList;
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public String findNearbyStopPlace(Envelope envelope, String name, Provider provider) {
        Geometry geometryFilter = geometryFactory.toGeometry(envelope);

        String sql = "SELECT s.netex_id FROM stop_place s " +
                                                           SQL_LEFT_JOIN_PARENT_STOP +
                                                           "WHERE ST_Within(s.centroid, :filter) = true " +
                                                           "AND " + SQL_STOP_PLACE_OR_PARENT_IS_VALID_AT_POINT_IN_TIME +
                                                           "AND s.name_value = :name ";

        if(provider != null){
            sql += "AND s.provider = :providerName";
        }

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("filter", geometryFilter);
        query.setParameter("name", name);
        query.setParameter("pointInTime", Date.from(Instant.now()));
        if (provider != null){
            query.setParameter("providerName", provider.name);
        }
        return getOneOrNull(query);
    }



    @Override
    public List<String> findNearbyStopPlace(Envelope envelope, StopTypeEnumeration stopTypeEnumeration) {
        Geometry geometryFilter = geometryFactory.toGeometry(envelope);

        Query query = entityManager.createNativeQuery("SELECT s.netex_id FROM stop_place s " +
                                                                SQL_LEFT_JOIN_PARENT_STOP +
                                                                "WHERE ST_within(s.centroid, :filter) = true " +
                                                                "AND " + SQL_STOP_PLACE_OR_PARENT_IS_VALID_AT_POINT_IN_TIME +
                                                                "AND s.stop_place_type = :stopPlaceType");

        query.setParameter("filter", geometryFilter);
        query.setParameter("stopPlaceType", stopTypeEnumeration.toString());
        query.setParameter("pointInTime", Date.from(Instant.now()));
        try {
            List<String> resultList = query.getResultList();
            return resultList;
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public String findFirstByKeyValues(String key, Set<String> values) {
        Set<String> matches = findByKeyValues(key, values);
        if(matches.isEmpty()) {
            return null;
        }
        return matches.iterator().next();

    }

    /**
     * Find stop place netex IDs by key value
     *
     * @param key key in key values for stop
     * @param values list of values to check for
     * @return set of stop place's netex IDs
     */
    @Override
    public Set<String> findByKeyValues(String key, Set<String> values) {
        return findByKeyValues(key, values, false);
    }

    /**
     * Find stop place netex IDs by key value
     *
     * @param key key in key values for stop
     * @param values list of values to check for
     * @param exactMatch set to <code>true</code> to perform lookup instead of search
     * @return set of stop place's netex IDs
     */
    @Override
    public Set<String> findByKeyValues(String key, Set<String> values, boolean exactMatch) {

        StringBuilder sqlQuery = new StringBuilder("SELECT s.netex_id " +
                                                           "FROM stop_place s " +
                                                            "INNER JOIN stop_place_key_values spkv " +
                                                           "ON spkv.stop_place_id = s.id " +
                                                           "INNER JOIN value_items v " +
                                                           "ON spkv.key_values_id = v.value_id " +
                                                           SQL_LEFT_JOIN_PARENT_STOP +
                                                           "WHERE spkv.key_values_key = :key " +
                                                           "AND " + SQL_STOP_PLACE_OR_PARENT_IS_VALID_AT_POINT_IN_TIME);


        List<String> parameters = new ArrayList<>(values.size());
        List<String> parametervalues = new ArrayList<>(values.size());
        final String parameterPrefix = "value";
        sqlQuery.append(" AND (");
        Iterator<String> valuesIterator = values.iterator();
        for (int parameterCounter = 0; parameterCounter < values.size(); parameterCounter++) {
            if(exactMatch){
                sqlQuery.append(" v.items = :value").append(parameterCounter);
                parameters.add(parameterPrefix + parameterCounter);
                parametervalues.add(valuesIterator.next());
            }
            else{
                sqlQuery.append(" v.items LIKE :value").append(parameterCounter);
                parameters.add(parameterPrefix + parameterCounter);
                parametervalues.add("%" + valuesIterator.next());
            }
            if (parameterCounter + 1 < values.size()) {
                sqlQuery.append(" OR ");
            }
        }

        sqlQuery.append(" )");

        Query query = entityManager.createNativeQuery(sqlQuery.toString());

        Iterator<String> iterator = parametervalues.iterator();
        parameters.forEach(parameter -> query.setParameter(parameter, iterator.next()));
        query.setParameter("key", key);
        query.setParameter("pointInTime", Date.from(Instant.now()));


        return getSetResult(query);
    }

    public List<String> searchByKeyValue(String key, String value) {

        Query query = entityManager.createNativeQuery("SELECT s.netex_id " +
                                                              "FROM stop_place_key_values spkv " +
                                                              "INNER JOIN value_items v " +
                                                              "ON spkv.key_values_id = v.value_id " +
                                                              "INNER JOIN stop_place s " +
                                                              "ON spkv.stop_place_id = s.id " +
                                                               SQL_LEFT_JOIN_PARENT_STOP +
                                                              "WHERE  spkv.key_values_key = :key " +
                                                              "AND v.items LIKE ( :value ) " +
                                                              "AND " + SQL_STOP_PLACE_OR_PARENT_IS_VALID_AT_POINT_IN_TIME);

        query.setParameter("key", key);
        query.setParameter("value", value);
        query.setParameter("pointInTime", Timestamp.from(Instant.now()));

        try {
            @SuppressWarnings("unchecked")
            List<String> results = query.getResultList();
            if (results.isEmpty()) {
                return null;
            } else {
                return results;
            }
        } catch (NoResultException noResultException) {
            return null;
        }
    }

    private StopTypeEnumeration parseStopType(Object o) {
        if (o != null) {
            return StopTypeEnumeration.valueOf(o.toString());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<IdMappingDto> findKeyValueMappingsForStop(Instant validFrom, Instant validTo, int recordPosition, int recordsPerRoundTrip) {
        String sql = "SELECT v.items, s.netex_id, s.stop_place_type, s.from_date sFrom, s.to_date sTo, p.from_date pFrom, p.to_date pTo " +
                             "FROM stop_place_key_values spkv " +
                             "  INNER JOIN value_items v " +
                             "      ON spkv.key_values_key in (:mappingIdKeys) AND spkv.key_values_id = v.value_id AND v.items NOT LIKE '' " +
                             "  INNER JOIN stop_place s ON s.id = spkv.stop_place_id " +
                             SQL_LEFT_JOIN_PARENT_STOP +
                             "WHERE " +
                              SQL_STOP_PLACE_OR_PARENT_IS_VALID_IN_INTERVAL +
                             "ORDER BY s.id,spkv.key_values_id";


        Query nativeQuery = entityManager.createNativeQuery(sql).setFirstResult(recordPosition).setMaxResults(recordsPerRoundTrip);

        if (validTo == null) {
            // Assuming 1000 years into the future is the same as forever
            validTo = Instant.from(ZonedDateTime.now().plusYears(1000).toInstant());
        }

        nativeQuery.setParameter("mappingIdKeys", Arrays.asList(ORIGINAL_ID_KEY, MERGED_ID_KEY));
        nativeQuery.setParameter("validFrom", Date.from(validFrom));
        nativeQuery.setParameter("validTo", Date.from(validTo));

        List<Object[]> result = nativeQuery.getResultList();

        List<IdMappingDto> mappingResult = new ArrayList<>();
        for (Object[] row : result) {
            Instant mappingValidFrom = parseInstant(row[3]);
            Instant mappingValidTo = parseInstant(row[4]);
            if (mappingValidFrom == null && mappingValidTo == null) {
                mappingValidFrom = parseInstant(row[5]);
                mappingValidTo = parseInstant(row[6]);
            }
            mappingResult.add(new IdMappingDto(row[0].toString(), row[1].toString(), mappingValidFrom, mappingValidTo, parseStopType(row[2])));
        }

        return mappingResult;
    }


    private Instant parseInstant(Object timestampObject) {
        if (timestampObject instanceof Timestamp) {
            return ((Timestamp)timestampObject).toInstant();
        }
        return null;
    }


    @Override
    public Set<String> findUniqueStopPlaceIds(Instant validFrom, Instant validTo) {
        String sql = "SELECT DISTINCT s.netex_id FROM stop_place s " +
                        SQL_LEFT_JOIN_PARENT_STOP +
                        "WHERE " +
                        SQL_STOP_PLACE_OR_PARENT_IS_VALID_IN_INTERVAL +
                        "ORDER BY s.netex_id";


        Query nativeQuery = entityManager.createNativeQuery(sql);

        if (validTo == null) {
            // Assuming 1000 years into the future is the same as forever
            validTo = Instant.from(ZonedDateTime.now().plusYears(1000).toInstant());
        }

        nativeQuery.setParameter("validFrom", Date.from(validFrom));
        nativeQuery.setParameter("validTo", Date.from(validTo));

        List<String> results = nativeQuery.getResultList();

        Set<String> ids = new HashSet<>();
        for(String result : results) {
            ids.add(result);
        }
        return ids;
    }


    @Override
    public List<String> findStopPlaceFromQuayOriginalId(String quayOriginalId, Instant pointInTime) {
        String sql = "SELECT DISTINCT s.netex_id " +
                             "FROM stop_place s " +
                             "  INNER JOIN stop_place_quays spq " +
                             "    ON s.id = spq.stop_place_id " +
                             "  INNER JOIN quay q " +
                             "    ON spq.quays_id = q.id " +
                             "  INNER JOIN quay_key_values qkv " +
                             "    ON q.id = qkv.quay_id AND qkv.key_values_key in (:originalIdKey) " +
                             "  INNER JOIN value_items vi " +
                             "    ON vi.value_id = qkv.key_values_id AND vi.items = :value " +
                             SQL_LEFT_JOIN_PARENT_STOP +
                             " WHERE " +
                SQL_STOP_PLACE_OR_PARENT_IS_VALID_AT_POINT_IN_TIME;

        Query query = entityManager.createNativeQuery(sql);


        query.setParameter("value", quayOriginalId);
        query.setParameter("originalIdKey", ORIGINAL_ID_KEY);
        query.setParameter("pointInTime",  Date.from(pointInTime));

        try {
            @SuppressWarnings("unchecked")
            List<String> results = query.getResultList();
            if (results.isEmpty()) {
                return new ArrayList<>();
            } else {
                return results;
            }
        } catch (NoResultException noResultException) {
            return null;
        }
    }

    @Override
    public List<StopPlace> findAllFromKeyValue(String key, Set<String> values) {

        String sql;
        if (values != null && !values.isEmpty()){
            sql = "SELECT DISTINCT s.* " +
                    " FROM stop_place s " +
                    "  INNER JOIN stop_place_key_values spkv " +
                    "    ON s.id = spkv.stop_place_id AND spkv.key_values_key = :key" +
                    "  INNER JOIN value_items vi " +
                    "    ON vi.value_id = spkv.key_values_id " +
                    " WHERE upper(vi.items) IN (:values) " +
                    " AND s.version = (select max(sv.version) from stop_place sv where sv.netex_id = s.netex_id)";
        } else {
            sql = "SELECT DISTINCT s.* " +
                    " FROM stop_place s " +
                    "   INNER JOIN stop_place_key_values spkv " +
                    "     ON s.id = spkv.stop_place_id AND spkv.key_values_key = :key" +
                    " WHERE s.version = (select max(sv.version) from stop_place sv where sv.netex_id = s.netex_id)";
        }

        Query query = entityManager.createNativeQuery(sql, StopPlace.class);


        query.setParameter("key", key);
        if (values != null && !values.isEmpty()){
            query.setParameter("values", values);
        }

        try {
            @SuppressWarnings("unchecked")
            List<StopPlace> results = query.getResultList();
            if (results.isEmpty()) {
                return new ArrayList<>();
            } else {
                results.forEach(stopPlace -> {
                        Hibernate.initialize(stopPlace.getKeyValues());
                        stopPlace.getKeyValues().values().forEach(value1 -> Hibernate.initialize(value1.getItems()));
                });
                return results;
            }
        } catch (NoResultException noResultException) {
            return null;
        }
    }

    @Override
    public Map<String, Set<String>> listStopPlaceIdsAndQuayIds(Instant validFrom, Instant validTo) {
        String sql = "SELECT DISTINCT s.netex_id as stop_place_id, q.netex_id as quay_id " +
                "FROM stop_place s " +
                "  INNER JOIN stop_place_quays spq " +
                "    ON s.id = spq.stop_place_id " +
                "  INNER JOIN quay q " +
                "    ON spq.quays_id = q.id " +
                SQL_LEFT_JOIN_PARENT_STOP +
                " WHERE " +
                SQL_STOP_PLACE_OR_PARENT_IS_VALID_IN_INTERVAL;

        if (validTo == null) {
            // Assuming 1000 years into the future is the same as forever
            validTo = Instant.from(ZonedDateTime.now().plusYears(1000).toInstant());
        }

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("validTo", Date.from(validTo));
        query.setParameter("validFrom",  Date.from(validFrom));

        try {
            @SuppressWarnings("unchecked")
            List<String[]> results = query.getResultList();
            if (results.isEmpty()) {
                return Collections.emptyMap();
            } else {
                HashMap<String, Set<String>> result = new HashMap<>();
                for (Object[] strings : results) {
                    String stopplaceId = (String) strings[0];
                    String quayId = (String) strings[1];
                    Set<String> quays = result.computeIfAbsent(stopplaceId, s -> new HashSet<>());
                    quays.add(quayId);
                }
                return result;
            }
        } catch (NoResultException noResultException) {
            return null;
        }
    }


    @Override
    public Iterator<StopPlace> scrollStopPlaces() {
        Session session = entityManager.unwrap(Session.class);

        Criteria criteria = session.createCriteria(StopPlace.class);

        criteria.setReadOnly(true);
        criteria.setFetchSize(SCROLL_FETCH_SIZE);
        criteria.setCacheable(false);
        ScrollableResults results = criteria.scroll(ScrollMode.FORWARD_ONLY);

        ScrollableResultIterator<StopPlace> stopPlaceEntityIterator = new ScrollableResultIterator<>(results, SCROLL_FETCH_SIZE, session);

        return stopPlaceEntityIterator;
    }

    @Override
    public Iterator<StopPlace> scrollStopPlaces(ExportParams exportParams) {

        Session session = entityManager.unwrap(Session.class);

        Pair<String, Map<String, Object>> queryWithParams = stopPlaceQueryFromSearchBuilder.buildQueryString(exportParams);
        NativeQuery nativeQuery = session.createSQLQuery(queryWithParams.getFirst());
        searchHelper.addParams(nativeQuery, queryWithParams.getSecond());

        return scrollStopPlaces(nativeQuery, session);
    }

    @Override
    public Iterator<StopPlace> scrollStopPlaces(Set<Long> stopPlacePrimaryIds) {
        Session session = entityManager.unwrap(Session.class);

        NativeQuery nativeQuery = session.createSQLQuery(generateStopPlaceQueryFromStopPlaceIds(stopPlacePrimaryIds));

        logger.info("Scrolling {} stop places", stopPlacePrimaryIds.size());
       return scrollStopPlaces(nativeQuery, session);
    }

    public List<StopPlace> getStopPlaceInitializedForExport(Set<Long> stopPlacePrimaryIds) {

        Set<String> stopPlacePrimaryIdStrings = stopPlacePrimaryIds.stream().map(lvalue -> String.valueOf(lvalue)).collect(Collectors.toSet());
        String joinedStopPlaceDbIds = String.join(",", stopPlacePrimaryIdStrings);
        StringBuilder sql = new StringBuilder("SELECT s FROM StopPlace s WHERE s.id IN(");
        sql.append(joinedStopPlaceDbIds);
        sql.append(")");


        EntityGraph<?> graph = entityManager.createEntityGraph("graph.exportNetexGraph");
        TypedQuery<StopPlace> q = entityManager.createQuery(sql.toString(), StopPlace.class);
        q.setHint("javax.persistence.fetchgraph", graph);
        List<StopPlace> results = q.getResultList();

        results.forEach(this::initializeStopPlace);

        return results;
    }

    public Iterator<StopPlace> scrollStopPlaces(NativeQuery nativeQuery, Session session) {

        nativeQuery.addEntity(StopPlace.class);
        nativeQuery.setReadOnly(true);
        nativeQuery.setFetchSize(SCROLL_FETCH_SIZE);
        nativeQuery.setCacheable(false);
        ScrollableResults results = nativeQuery.scroll(ScrollMode.FORWARD_ONLY);

        ScrollableResultIterator<StopPlace> stopPlaceEntityIterator = new ScrollableResultIterator<>(results, SCROLL_FETCH_SIZE, session);

        return stopPlaceEntityIterator;
    }

    private String generateStopPlaceQueryFromStopPlaceIds(Set<Long> stopPlacePrimaryIds) {

        Set<String> stopPlacePrimaryIdStrings = stopPlacePrimaryIds.stream().map(lvalue -> String.valueOf(lvalue)).collect(Collectors.toSet());
        String joinedStopPlaceDbIds = String.join(",", stopPlacePrimaryIdStrings);
        StringBuilder sql = new StringBuilder("SELECT s.* FROM stop_place s WHERE s.id IN(");
        sql.append(joinedStopPlaceDbIds);
        sql.append(")");
        return sql.toString();
    }

    @Override
    public Set<String> getNetexIds(ExportParams exportParams) {
        Pair<String, Map<String, Object>> pair = stopPlaceQueryFromSearchBuilder.buildQueryString(exportParams);
        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery("SELECT sub.netex_id from (" + pair.getFirst() + ") sub");

        searchHelper.addParams(query, pair.getSecond());

        @SuppressWarnings("unchecked")
        Set<String> result =  new HashSet<>(query.list());
        return result;
    }

    @Override
    public Set<Long> getDatabaseIds(ExportParams exportParams, boolean ignorePaging, Provider provider) {
        Pair<String, Map<String, Object>> pair = stopPlaceQueryFromSearchBuilder.buildQueryStringByProvider(exportParams, provider);
        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery("SELECT sub.id from (" + pair.getFirst() + ") sub");

        if(!ignorePaging) {
            long firstResult = exportParams.getStopPlaceSearch().getPageable().getOffset();
            query.setFirstResult(Math.toIntExact(firstResult));
            query.setMaxResults(exportParams.getStopPlaceSearch().getPageable().getPageSize());
        }
        searchHelper.addParams(query, pair.getSecond());

        Set<Long> result = new HashSet<>();
        for(Object object : query.list()) {
            BigInteger bigInteger = (BigInteger) object;
            result.add(bigInteger.longValue());

        }

        return result;
    }


    /**
     * Initialize export job table with stop ids that must be exported
     * @param provider
     *  provider for which export is launched
     * @param exportJobId
     *  id of the export job
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initExportJobTable(Provider provider, Long exportJobId){

        Map<String, Object> parameters = new HashMap<>();


        String queryStr = " INSERT INTO export_job_id_list \n" +
                "           SELECT :exportJobId,s.id as stop_id FROM stop_place s where s.id in  \n" +
                "                 ( SELECT max(s1.id) FROM stop_place s1 ";

        if (provider != null && provider.getChouetteInfo().getReferential() != null && !provider.getChouetteInfo().getReferential().equals(administrationSpaceName)){
            queryStr = queryStr + " JOIN stop_place_key_values spkv ON spkv.stop_place_id = s1.id\n" +
                    "            JOIN value_items vi ON spkv.key_values_id = vi.value_id\n" +
                    "            WHERE LOWER(vi.items) LIKE concat(:providerName, ':%') \n";
            parameters.put("providerName",  provider.getChouetteInfo().getReferential().toLowerCase());
        }

        queryStr = queryStr + "  group by s1.netex_id  ) " +
                "                     AND  (s.from_date <= :pointInTime OR  s.from_date IS NULL) \n" +
                "                     AND (   s.to_date >= :pointInTime  OR s.to_date IS NULL) \n" +
                "                        and s.parent_stop_place = false  ";


        parameters.put("exportJobId", exportJobId);
        parameters.put("pointInTime", Date.from(Instant.now()));

        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery(queryStr);
        searchHelper.addParams(query, parameters);

        query.executeUpdate();

    }

    /**
     * Add parent_stop_places that must be exported to table export_job_id_list
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addParentStopPlacesToExportJobTable(Long exportJobId){


        Map<String, Object> parameters = new HashMap<>();
        String queryStr = "INSERT INTO export_job_id_list \n" +
                " SELECT :exportJobId, s.id FROM stop_place s WHERE \n" +
                " s.from_date <= :pointInTime \n" +
                " AND (   s.to_date >= :pointInTime  or s.to_date IS NULL) \n" +
                " AND s.netex_id in ( \n" +
                " SELECT distinct s2.parent_site_ref FROM stop_place s2 WHERE s2.parent_site_ref IS NOT NULL \n" +
                " AND S2.id IN (SELECT exported_object_id FROM export_job_id_list WHERE job_id = :exportJobId) ) \n";



        Session session = entityManager.unwrap(Session.class);
        parameters.put("exportJobId", exportJobId);
        parameters.put("pointInTime",  Date.from(Instant.now()));

        NativeQuery query = session.createNativeQuery(queryStr);
        searchHelper.addParams(query, parameters);
        query.executeUpdate();

    }

    /**
     * Get a batch of object to process
     * @param exportJobId
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Set<Long> getNextBatchToProcess(Long exportJobId){

        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery("SELECT exported_object_id FROM export_job_id_list WHERE job_id  = :exportJobId LIMIT 1000");

        query.setParameter("exportJobId", exportJobId);

        Set<Long> result = new HashSet<>();
        for(Object object : query.list()) {
            BigInteger bigInteger = (BigInteger) object;
            result.add(bigInteger.longValue());

        }
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteProcessedIds(Long exportJobId, Set<Long> processedStops){
        Session session = entityManager.unwrap(Session.class);
        String queryStr = "DELETE FROM export_job_id_list WHERE job_id = :exportJobId AND exported_object_id IN :stopPlaceIdList";
        NativeQuery query = session.createNativeQuery(queryStr);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("exportJobId", exportJobId);
        parameters.put("stopPlaceIdList", processedStops);
        searchHelper.addParams(query, parameters);
        query.executeUpdate();
    }

    public int countStopsInExport(Long exportJobId) {
        String queryString = "select count(*) FROM export_job_id_list WHERE job_id = :exportJobId";
        return ((Number)entityManager.createNativeQuery(queryString).setParameter("exportJobId", exportJobId).getSingleResult()
               ).intValue();
    }

    /**
     * Takes a set of stop place id and search for parents. Add parent's ids to the set
     * @param stopPlaceDbIds
     * 		A set of children's ids
     * @return
     * 		A set with children ids + parents ids
     *
     */
    @Override
    public Set<Long> addParentIds(Set<Long> stopPlaceDbIds) {

        if (stopPlaceDbIds == null || stopPlaceDbIds.size() == 0){
            logger.info("No results where found");
            return stopPlaceDbIds;
        }

        Set<String> stopPlaceStringDbIds = stopPlaceDbIds.stream().map(lvalue -> String.valueOf(lvalue)).collect(Collectors.toSet());
        String joinedStopPlaceDbIds = String.join(",", stopPlaceStringDbIds);
        StringBuilder sql = new StringBuilder("SELECT id FROM stop_place sp " +
                                                "WHERE parent_stop_place = TRUE " +
                                                "AND sp.netex_id IN  ( " +
                                                    "SELECT parent_site_ref FROM stop_place childrenSP " +
                                                    "where childrenSP.id in (");

        sql.append(joinedStopPlaceDbIds);
        sql.append(") " +
                " ) " +
                "AND " +
                "sp.from_date <= :pointInTime " +
                "AND ( " +
                "sp.to_date >= :pointInTime OR sp.to_date IS NULL )");


        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery(sql.toString());

        Map<String,Object> parameters = new HashMap<>();
        parameters.put("pointInTime", Date.from(Instant.now()));

        searchHelper.addParams(query, parameters);

        List<Long> results = query.getResultList().stream()
                                    .mapToLong(bi-> ((BigInteger)bi).longValue())
                                    .boxed()
                                    .collect(Collectors.toList());

        Set<Long> parentsAndChildrenIds = new HashSet<>();
        parentsAndChildrenIds.addAll(stopPlaceDbIds);
        parentsAndChildrenIds.addAll(results);
        return parentsAndChildrenIds;
    }


    @Override
    public Page<StopPlace> findStopPlace(ExportParams exportParams) {
        Pair<String, Map<String, Object>> queryWithParams = stopPlaceQueryFromSearchBuilder.buildQueryString(exportParams);

        final Query nativeQuery = entityManager.createNativeQuery(queryWithParams.getFirst(), StopPlace.class);

        queryWithParams.getSecond().forEach(nativeQuery::setParameter);
        long firstResult = exportParams.getStopPlaceSearch().getPageable().getOffset();
        nativeQuery.setFirstResult(Math.toIntExact(firstResult));
        nativeQuery.setMaxResults(exportParams.getStopPlaceSearch().getPageable().getPageSize());

        List<StopPlace> stopPlaces = nativeQuery.getResultList();
        return new PageImpl<>(stopPlaces, exportParams.getStopPlaceSearch().getPageable(), stopPlaces.size());

    }

    @Override
    public List<StopPlace> findStopPlaceByQuays(List<Quay> quays) {

        List<Long> quaysIdList = quays.stream()
                                      .map(Quay::getId)
                                      .collect(Collectors.toList());


        final String queryString = "SELECT sp.* FROM stop_place sp JOIN stop_place_quays spq on sp.id = spq.stop_place_id  " +
                "                            WHERE spq.quays_id IN :quaysId";

        List<StopPlace> stopPlaces = entityManager.createNativeQuery(queryString, StopPlace.class)
                                    .setParameter("quaysId",quaysIdList)
                                    .getResultList();

        return stopPlaces;

    }

    @Override
    public List<StopPlace> findAll(List<String> stopPlacesNetexIds) {
        final String queryString = "SELECT stopPlace FROM StopPlace stopPlace WHERE stopPlace.netexId IN :netexIds";
        final TypedQuery<StopPlace> typedQuery = entityManager.createQuery(queryString, StopPlace.class);
        typedQuery.setParameter("netexIds", stopPlacesNetexIds);
        return typedQuery.getResultList();
    }

    @Override
    public List<Quay> findQuayByNetexId(String netexId) {
        final String queryString = "SELECT quay FROM Quay quay WHERE quay.netexId = :netexId";
        final TypedQuery<Quay> typedQuery = entityManager.createQuery(queryString, Quay.class);
        typedQuery.setParameter("netexId", netexId);
        List<Quay> resultList = typedQuery.getResultList();

        for (Quay quay : resultList) {
            Hibernate.initialize(quay.getKeyValues());

            quay.getKeyValues().forEach((k,v) ->{
                        Hibernate.initialize(v.getItems());
                    }
                    );

        }

        return resultList;
    }

    @Override
    public StopPlace findByQuay(Quay quay) {
        final String queryString = "select s from StopPlace s where :quay member of s.quays";
        final TypedQuery<StopPlace> typedQuery = entityManager.createQuery(queryString, StopPlace.class);
        typedQuery.setParameter("quay", quay);
        StopPlace stopPlace = getOneOrNull(typedQuery);
        Hibernate.initialize(stopPlace.getKeyValues());
        return stopPlace;
    }

    /**
     * Returns parent stops only if multi modal stops
     * @param search
     * @return
     */
    public Page<StopPlace> findStopPlacesWithEffectiveChangeInPeriod(ChangedStopPlaceSearch search) {
        final String queryString = "select sp.* " + STOP_PLACE_WITH_EFFECTIVE_CHANGE_QUERY_BASE + " order by sp.from_Date";

        long firstResult = search.getPageable().getOffset();

        List<StopPlace> stopPlaces = entityManager.createNativeQuery(queryString, StopPlace.class)
                                             .setParameter("from", Date.from(search.getFrom()))
                                             .setParameter("to", Date.from(search.getTo()))
                                             .setFirstResult(Math.toIntExact(firstResult))
                                             .setMaxResults(search.getPageable().getPageSize())
                                             .getResultList();


        if(logger.isDebugEnabled()) {
            final String generatedSql = basicFormatter.format(queryString.toString());
            logger.debug("sql: {}\nSearch object: {}", generatedSql, search);
        }

        int totalCnt = stopPlaces.size();
        if (totalCnt == search.getPageable().getPageSize()) {
            totalCnt = countStopPlacesWithEffectiveChangeInPeriod(search);
        }

        return new PageImpl<>(stopPlaces, search.getPageable(), totalCnt);
    }

    /**
     * Return jbv code mapping for rail stations. The stop place contains jbc code mapping. The quay contains the public code.
     * @return
     */
    @Override
    public List<JbvCodeMappingDto> findJbvCodeMappingsForStopPlace() {
        String sql = "SELECT DISTINCT vi.items, s.netex_id " +
                "FROM stop_place_key_values skv " +
                "   INNER JOIN stop_place s " +
                "       ON s.id = skv.stop_place_id AND s.stop_place_type = :stopPlaceType " +
                SQL_LEFT_JOIN_PARENT_STOP +
                "   INNER JOIN value_items vi " +
                "       ON skv.key_values_id = vi.value_id AND vi.items NOT LIKE '' AND skv.key_values_key = :mappingIdKeys " +
                "WHERE " + SQL_STOP_PLACE_OR_PARENT_IS_VALID_AT_POINT_IN_TIME +
                "ORDER BY items ";
        Query nativeQuery = entityManager.createNativeQuery(sql);

        nativeQuery.setParameter("stopPlaceType", StopTypeEnumeration.RAIL_STATION.toString());
        nativeQuery.setParameter("mappingIdKeys", Arrays.asList(JBV_CODE));
        nativeQuery.setParameter("pointInTime", Date.from(Instant.now()));

        @SuppressWarnings("unchecked")
        List<Object[]> result = nativeQuery.getResultList();

        List<JbvCodeMappingDto> mappingResult = new ArrayList<>();
        for (Object[] row : result) {
            mappingResult.add(new JbvCodeMappingDto(row[0].toString(), null, row[1].toString(), null));
        }

        return mappingResult;
    }

    /**
     * Return id mappings for stopPlaces between original Id (send by producers) and netex id (generated by Tiamat)
     *
     * @return the list of mappings
     */
    @Override
    public List<JbvCodeMappingDto> findIdMappingsForStopPlace() {
        String sql = "SELECT DISTINCT sp_id,vi.items, netex_id FROM " +
                " (SELECT MAX(id) AS sp_id, netex_id FROM stop_place sp group by netex_id) spref " +
                " INNER JOIN stop_place_key_values spkv ON spref.sp_id = spkv.stop_place_id  " +
                " INNER JOIN value_items vi ON vi.value_id = spkv.key_values_id " +
                "  WHERE spkv.key_values_key = 'imported-id'  ";
        Query nativeQuery = entityManager.createNativeQuery(sql);


        @SuppressWarnings("unchecked")
        List<Object[]> result = nativeQuery.getResultList();

        List<JbvCodeMappingDto> mappingResult = new ArrayList<>();
        for (Object[] row : result) {
            mappingResult.add(new JbvCodeMappingDto(row[1].toString(), null, row[2].toString(), null));
        }

        return mappingResult;
    }

    /**
     * Return selected id mappings for stopPlaces between original Id (chosen by producers) and netex id (generated by Tiamat)
     *
     * @return the list of mappings
     */
    @Override
    public List<JbvCodeMappingDto> findSelectedIdMappingsForStopPlace() {
        String sql = "SELECT DISTINCT sp_id,vi.items, netex_id FROM " +
                " (SELECT MAX(id) AS sp_id, netex_id FROM stop_place sp group by netex_id) spref " +
                " INNER JOIN stop_place_key_values spkv ON spref.sp_id = spkv.stop_place_id  " +
                " INNER JOIN value_items vi ON vi.value_id = spkv.key_values_id " +
                "  WHERE spkv.key_values_key = 'selected-id'  ";
        Query nativeQuery = entityManager.createNativeQuery(sql);


        @SuppressWarnings("unchecked")
        List<Object[]> result = nativeQuery.getResultList();

        List<JbvCodeMappingDto> mappingResult = new ArrayList<>();
        for (Object[] row : result) {
            mappingResult.add(new JbvCodeMappingDto(row[1].toString(), null, row[2].toString(), null));
        }

        return mappingResult;
    }

    private int countStopPlacesWithEffectiveChangeInPeriod(ChangedStopPlaceSearch search) {
        String queryString = "select count(sp.id) " + STOP_PLACE_WITH_EFFECTIVE_CHANGE_QUERY_BASE;
        return ((Number)entityManager.createNativeQuery(queryString).setParameter("from", Date.from(search.getFrom()))
                       .setParameter("to",  Date.from(search.getTo())).getSingleResult()).intValue();
    }

    private static final String STOP_PLACE_WITH_EFFECTIVE_CHANGE_QUERY_BASE =
            " from stop_place sp INNER JOIN " +
                    "(SELECT spinner.netex_id, MAX(spinner.version) AS maxVersion " +
                    "   FROM stop_place spinner " +
                    " WHERE " +
                    "   (spinner.from_date BETWEEN :from AND :to OR spinner.to_date BETWEEN :from AND :to ) " +
                    "   AND spinner.parent_site_ref IS NULL " +
                    // Make sure we do not fetch stop places that have become children of parent stops in "future" versions
                    "   AND NOT EXISTS( " +
                    "      SELECT sp2.id FROM stop_place sp2 " +
                    "      INNER JOIN stop_place parent " +
                    "        ON parent.netex_id = sp2.parent_site_ref " +
                    "          AND cast(parent.version AS TEXT) = sp2.parent_site_ref_version " +
                    "          AND (parent.from_date BETWEEN :from AND :to OR parent.to_date BETWEEN :from AND :to ) " +
                    "        WHERE sp2.netex_id = spinner.netex_id " +
                    "          AND sp2.version > spinner.version " +
                    "  )" +
                    " GROUP BY spinner.netex_id " +
                    ") sub " +
                    "   ON sub.netex_id = sp.netex_id " +
                    "   AND sub.maxVersion = sp.version";

    private <T> T getOneOrNull(TypedQuery<T> query) {
        try {
            List<T> resultList = query.getResultList();
            return resultList.isEmpty() ? null : resultList.get(0);
        } catch (NoResultException e) {
            return null;
        }
    }

    private String getOneOrNull(Query query) {
        try {
            @SuppressWarnings("unchecked")
            List<String> results = query.getResultList();
            if (results.isEmpty()) {
                return null;
            } else {
                return results.get(0);
            }
        } catch (NoResultException noResultException) {
            return null;
        }
    }

    private Set<String> getSetResult(Query query) {
        try {
            @SuppressWarnings("unchecked")
            List<String> results = query.getResultList();
            if (results.isEmpty()) {
                return Sets.newHashSet();
            } else {
                return results.stream().collect(toSet());
            }
        } catch (NoResultException noResultException) {
            return Sets.newHashSet();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean deleteAllStopPlacesQuaysByOrganisation(String organisation){
        Query query = entityManager.createNativeQuery("SELECT clean_orga(:organisation)");
        query.setParameter("organisation", organisation);
        return (boolean) query.getSingleResult();
    }

    public List<StopPlace> findTADStopPlacesForArea(String area){
        List<StopPlace> results = new ArrayList<>();
        results.addAll(findTADStopPlacesForTypeAndArea("yes",area));
        results.addAll(findTADStopPlacesForTypeAndArea("partial",area));
        return results;
    }

    List<StopPlace> findTADStopPlacesForTypeAndArea(String type, String area){
        String queryStr = "SELECT s.* " +
                "FROM stop_place_key_values spkv " +
                "INNER JOIN value_items v " +
                "ON spkv.key_values_id = v.value_id " +
                "INNER JOIN stop_place s " +
                "ON spkv.stop_place_id = s.id " +
                SQL_LEFT_JOIN_PARENT_STOP +
                "WHERE  spkv.key_values_key = :key " +
                "AND v.items LIKE ( :value ) " +
                " AND ST_contains(ST_GEOMFROMTEXT(:polygon ,4326),s.centroid) " +
                "AND " + SQL_STOP_PLACE_OR_PARENT_IS_VALID_AT_POINT_IN_TIME;



        Query query = entityManager.createNativeQuery(queryStr, StopPlace.class);
        query.setParameter("key", "zonalStopPlace");
        query.setParameter("value", type);
        query.setParameter("polygon", area);
        query.setParameter("pointInTime", Timestamp.from(Instant.now()));

        try {
           return query.getResultList();
        } catch (NoResultException noResultException) {
            logger.info("no TAD stop found found for type :" + type + " and area:" + area );
            return new ArrayList<>();
        }
    }

    @Override
    public StopPlace findFirstByNetexIdOrderByVersionDescAndInitialize(String netexId){
        String sql = "SELECT tad.* FROM stop_place tad WHERE tad.netex_id = :netexId AND tad.version = (SELECT max(tad2.version) FROM stop_place tad2 WHERE tad2.netex_id = :netexId)";

        Query stopPlaceTypedQuery = entityManager.createNativeQuery(sql, StopPlace.class);

        stopPlaceTypedQuery.setParameter("netexId",netexId);

        List<StopPlace> results = stopPlaceTypedQuery.getResultList();

        results.forEach(this::initializeStopPlace);

        return results.isEmpty() ? null : results.get(0);
    }

    private void initializeStopPlace(StopPlace stopPlace){
        Hibernate.initialize(stopPlace.getKeyValues());
        stopPlace.getKeyValues().values().forEach(value -> Hibernate.initialize(value.getItems()));
        Hibernate.initialize(stopPlace.getAccessibilityAssessment());
        if (stopPlace.getAccessibilityAssessment() != null){
            Hibernate.initialize(stopPlace.getAccessibilityAssessment().getLimitations());
        }
        Hibernate.initialize(stopPlace.getAlternativeNames());
        Hibernate.initialize(stopPlace.getPolygon());
        Hibernate.initialize(stopPlace.getTariffZones());
        Hibernate.initialize(stopPlace.getPlaceEquipments());
        Hibernate.initialize(stopPlace.getEquipmentPlaces());
        Hibernate.initialize(stopPlace.getAccessSpaces());

        Hibernate.initialize(stopPlace.getChildren());

        Hibernate.initialize(stopPlace.getTopographicPlace());

        if (stopPlace.getPlaceEquipments() != null){
            Hibernate.initialize(stopPlace.getPlaceEquipments().getInstalledEquipment());
        }


        stopPlace.getQuays().forEach(quay->{
            Hibernate.initialize(quay.getKeyValues());
            quay.getKeyValues().values().forEach(value -> Hibernate.initialize(value.getItems()));
            Hibernate.initialize(quay.getAlternativeNames());
            Hibernate.initialize(quay.getPolygon());
            Hibernate.initialize(quay.getPlaceEquipments());
            if (quay.getPlaceEquipments() != null){
                Hibernate.initialize(quay.getPlaceEquipments().getInstalledEquipment());
            }
            Hibernate.initialize(quay.getBoardingPositions());
            Hibernate.initialize(quay.getEquipmentPlaces());
            Hibernate.initialize(quay.getCheckConstraints());
            Hibernate.initialize(quay.getAccessibilityAssessment());
            if (quay.getAccessibilityAssessment() != null){
                Hibernate.initialize(quay.getAccessibilityAssessment().getLimitations());
            }
        });
    }

    @Override
    public void deleteStopPlaceChildrenByChildren(List<StopPlace> stopPlaces){
        for(StopPlace stopPlace : stopPlaces){
            entityManager
                    .createNativeQuery("DELETE FROM stop_place_children WHERE children_id = :stopPlaceId")
                    .setParameter("stopPlaceId", stopPlace.getId())
                    .executeUpdate();
        }
    }

    @Override
    public void deleteStopPlaceChildrenByParent(List<StopPlace> stopPlaces){
        for(StopPlace stopPlace : stopPlaces){
            entityManager
                    .createNativeQuery("DELETE FROM stop_place_children WHERE stop_place_id = :stopPlaceId")
                    .setParameter("stopPlaceId", stopPlace.getId())
                    .executeUpdate();
        }
    }

}

