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

import org.hibernate.Hibernate;
import org.rutebanken.tiamat.dtoassembling.dto.IdMappingDto;
import org.rutebanken.tiamat.dtoassembling.dto.JbvCodeMappingDto;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.MERGED_ID_KEY;
import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;
import static org.rutebanken.tiamat.repository.StopPlaceRepositoryImpl.*;

@Transactional
public class QuayRepositoryImpl implements QuayRepositoryCustom {
    public static final String JBV_CODE = "jbvCode";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * This repository method does not use validity for returning the right version.
     * It should probably join stop place and parent stop place to check validity.
     */
    @Override
    public String findFirstByKeyValues(String key, Set<String> values) {

        Query query = entityManager.createNativeQuery("SELECT q.netex_id " +
                "FROM quay_key_values qkv " +
                "INNER JOIN value_items v " +
                "ON qkv.key_values_id = v.value_id " +
                "INNER JOIN quay q " +
                "ON quay_id = quay_id " +
                "WHERE qkv.key_values_key = :key " +
                "AND v.items IN ( :values ) " +
                "AND q.version = (SELECT MAX(qv.version) FROM quay qv WHERE q.netex_id = qv.netex_id)");

        query.setParameter("key", key);
        query.setParameter("values", values);

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

    public List<String> searchByKeyValue(String key, String value) {

        Query query = entityManager.createNativeQuery("SELECT q.netex_id " +
                "FROM quay_key_values qkv " +
                "INNER JOIN value_items v " +
                "   ON qkv.key_values_id = v.value_id " +
                "INNER JOIN stop_place_quays spq " +
                "	ON spq.quays_id = qkv.quay_id " +
                "INNER JOIN quay q " +
                "	ON spq.quays_id = q.id " +
                "INNER JOIN stop_place s " +
                "	ON s.id = spq.stop_place_id " +
                SQL_LEFT_JOIN_PARENT_STOP +
                "WHERE  qkv.key_values_key = :key " +
                "AND v.items LIKE ( :value ) " +
                "AND " + SQL_STOP_PLACE_OR_PARENT_IS_VALID_AT_POINT_IN_TIME +
                "AND q.version = (SELECT max(qv.version) FROM quay qv WHERE q.netex_id = qv.netex_id)");

        query.setParameter("key", key);
        query.setParameter("value", value);
        query.setParameter("pointInTime", Date.from(Instant.now()));


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

    @Override
    public List<IdMappingDto> findKeyValueMappingsForQuay(Instant validFrom, Instant validTo, int recordPosition, int recordsPerRoundTrip) {
        String sql = "SELECT vi.items, q.netex_id, s.stop_place_type, s.from_date sFrom, s.to_date sTo, p.from_date pFrom, p.to_date pTo " +
                "FROM quay_key_values qkv " +
                "	INNER JOIN stop_place_quays spq " +
                "		ON spq.quays_id = qkv.quay_id " +
                "	INNER JOIN quay q " +
                "		ON spq.quays_id = q.id " +
                "	INNER JOIN stop_place s " +
                "		ON s.id = spq.stop_place_id " +
                "	INNER JOIN value_items vi " +
                "		ON qkv.key_values_id = vi.value_id AND vi.items NOT LIKE '' AND qkv.key_values_key in (:mappingIdKeys) " +
                SQL_LEFT_JOIN_PARENT_STOP +
                "WHERE " +
                SQL_STOP_PLACE_OR_PARENT_IS_VALID_IN_INTERVAL +
                "ORDER BY q.id,qkv.key_values_id";


        Query nativeQuery = entityManager.createNativeQuery(sql).setFirstResult(recordPosition).setMaxResults(recordsPerRoundTrip);

        if (validTo == null) {
            // Assuming 1000 years into the future is the same as forever
            validTo = Instant.from(ZonedDateTime.now().plusYears(1000).toInstant());
        }

        nativeQuery.setParameter("mappingIdKeys", Arrays.asList(ORIGINAL_ID_KEY, MERGED_ID_KEY));
        nativeQuery.setParameter("validFrom", Date.from(validFrom));
        nativeQuery.setParameter("validTo", Date.from(validTo));
        @SuppressWarnings("unchecked")
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
            return ((Timestamp) timestampObject).toInstant();
        }
        return null;
    }

    @Override
    public Set<String> findUniqueQuayIds(Instant validFrom, Instant validTo) {
        String sql = "SELECT distinct q.netex_id " +
                "FROM quay q " +
                "INNER JOIN stop_place_quays spq " +
                "   ON spq.quays_id = q.id " +
                "INNER JOIN stop_place s " +
                "   ON spq.stop_place_id = s.id " +
                SQL_LEFT_JOIN_PARENT_STOP +
                "WHERE " +
                SQL_STOP_PLACE_OR_PARENT_IS_VALID_IN_INTERVAL +
                "ORDER BY q.netex_id";


        Query nativeQuery = entityManager.createNativeQuery(sql);

        if (validTo == null) {
            // Assuming 1000 years into the future is the same as forever
            validTo = Instant.from(ZonedDateTime.now().plusYears(1000).toInstant());
        }

        nativeQuery.setParameter("validFrom", Date.from(validFrom));
        nativeQuery.setParameter("validTo", Date.from(validTo));

        List<String> results = nativeQuery.getResultList();

        Set<String> ids = new HashSet<>();
        for (String result : results) {
            ids.add(result);
        }
        return ids;
    }

    private StopTypeEnumeration parseStopType(Object o) {
        if (o != null) {
            return StopTypeEnumeration.valueOf(o.toString());
        }
        return null;
    }

    /**
     * Return jbv code mapping for rail stations. The stop place contains jbc code mapping. The quay contains the public code.
     *
     * @return
     */
    @Override
    public List<JbvCodeMappingDto> findJbvCodeMappingsForQuay() {
        String sql = "SELECT DISTINCT vi.items, q.public_code, q.netex_id " +
                "FROM stop_place_key_values skv " +
                "   INNER JOIN stop_place_quays spq " +
                "       ON spq.stop_place_id = skv.stop_place_id " +
                "   INNER JOIN stop_place s " +
                "       ON s.id = skv.stop_place_id AND s.stop_place_type = :stopPlaceType " +
                SQL_LEFT_JOIN_PARENT_STOP +
                "   INNER JOIN quay q " +
                "       ON spq.quays_id = q.id  AND q.public_code NOT LIKE '' " +
                "   INNER JOIN value_items vi " +
                "       ON skv.key_values_id = vi.value_id AND vi.items NOT LIKE '' AND skv.key_values_key = :mappingIdKeys " +
                "WHERE " + SQL_STOP_PLACE_OR_PARENT_IS_VALID_AT_POINT_IN_TIME +
                "ORDER BY items, public_code ";
        Query nativeQuery = entityManager.createNativeQuery(sql);

        nativeQuery.setParameter("stopPlaceType", StopTypeEnumeration.RAIL_STATION.toString());
        nativeQuery.setParameter("mappingIdKeys", Arrays.asList(JBV_CODE));
        nativeQuery.setParameter("pointInTime", Date.from(Instant.now()));

        @SuppressWarnings("unchecked")
        List<Object[]> result = nativeQuery.getResultList();

        List<JbvCodeMappingDto> mappingResult = new ArrayList<>();
        for (Object[] row : result) {
            mappingResult.add(new JbvCodeMappingDto(row[0].toString(), row[1].toString(), row[2].toString(), null));
        }

        return mappingResult;
    }
    /**
     * Return id mappings for quays between original Id (send by producers) and netex id (generated by Tiamat)
     *
     * @return the list of mappings
     */
    @Override
    public List<JbvCodeMappingDto> findIdMappingsForQuay() {
        String sql = "WITH qref AS (" +
                "    SELECT MAX(q.id) AS q_id, q.netex_id, q.name_value" +
                "    FROM quay q" +
                "    INNER JOIN quay_key_values qkv ON q.id = qkv.quay_id" +
                "    WHERE qkv.key_values_key = 'imported-id'" +
                "    GROUP BY q.netex_id, q.name_value" +
                ")" +
                "SELECT DISTINCT qref.q_id, vi.items, qref.netex_id," +
                "    CASE WHEN qref.name_value IS NOT NULL AND qref.name_value <> '' THEN qref.name_value" +
                "         ELSE s.name_value END AS name_value" +
                " FROM qref" +
                " INNER JOIN quay_key_values qkv ON qref.q_id = qkv.quay_id AND qkv.key_values_key = 'imported-id'" +
                " INNER JOIN value_items vi ON vi.value_id = qkv.key_values_id" +
                " LEFT JOIN stop_place_quays spq ON spq.quays_id = qref.q_id" +
                " LEFT JOIN stop_place s ON s.id = spq.stop_place_id";

        Query nativeQuery = entityManager.createNativeQuery(sql);

        List<Object[]> result = nativeQuery.getResultList();

        List<JbvCodeMappingDto> mappingResult = new ArrayList<>();
        for (Object[] row : result) {
            mappingResult.add(new JbvCodeMappingDto(row[1].toString(), null, row[2].toString(), row[3].toString()));
        }

        return mappingResult;
    }


    /**
     * Return selected id mappings for quays between original Id (chosen by producers) and netex id (generated by Tiamat)
     *
     * @return the list of mappings
     */
    @Override
    public List<JbvCodeMappingDto> findSelectedIdMappingsForQuay() {
        String sql = "SELECT DISTINCT q_id,vi.items, netex_id FROM " +
                " (SELECT MAX(id) AS q_id, netex_id FROM quay q group by netex_id) qref " +
                " INNER JOIN quay_key_values qkv ON qref.q_id = qkv.quay_id  " +
                " INNER JOIN value_items vi ON vi.value_id = qkv.key_values_id " +
                "  WHERE qkv.key_values_key = 'selected-id'  ";
        Query nativeQuery = entityManager.createNativeQuery(sql);


        @SuppressWarnings("unchecked")
        List<Object[]> result = nativeQuery.getResultList();

        List<JbvCodeMappingDto> mappingResult = new ArrayList<>();
        for (Object[] row : result) {
            mappingResult.add(new JbvCodeMappingDto(row[1].toString(), null, row[2].toString(), null));
        }

        return mappingResult;
    }

    @Override
    public String findImportedIdByNetexIdQuay(Long quayId) {
        Query query = entityManager.createNativeQuery("" +
                "SELECT v.items FROM value_items v " +
                "  JOIN quay_key_values qkv ON qkv.key_values_id = v.value_id " +
                "  JOIN quay q ON q.id = qkv.quay_id " +
                " WHERE qkv.key_values_key = :keyValueImportedId " +
                "   AND q.id = :quayId");

        query.setParameter("quayId", quayId);
        query.setParameter("keyValueImportedId", "imported-id");

        @SuppressWarnings("unchecked")
        List<String> results = query.getResultList();
        return results
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Quay> findAllByImportedId(String importedId) {

        Query query = entityManager.createNativeQuery("SELECT q.* " +
                "FROM quay q " +
                "INNER JOIN quay_key_values qkv " +
                "ON qkv.quay_id = q.id " +
                "INNER JOIN value_items v " +
                "ON qkv.key_values_id = v.value_id " +
                "WHERE qkv.key_values_key = 'imported-id' " +
                "AND UPPER(v.items) = UPPER(:importedId) ", Quay.class );

        query.setParameter("importedId", importedId);


        List<Quay> resultList = query.getResultList();

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
    public Optional<Quay> findActiveQuayForImportedId(String importedId) {

        String searchString = "'%:Quay:" + importedId + "'";


        Query query = entityManager.createNativeQuery("SELECT q.* " +
                "FROM quay q " +
                "INNER JOIN quay_key_values qkv ON qkv.quay_id = q.id " +
                "INNER JOIN value_items v  ON qkv.key_values_id = v.value_id " +
                "INNER JOIN stop_place_quays spq ON spq.quays_id = qkv.quay_id " +
                "INNER JOIN stop_place s ON s.id = spq.stop_place_id  " +

                "WHERE qkv.key_values_key = 'imported-id' " +
                " AND  (s.from_date IS NULL OR s.from_date <= :pointInTime) " +
                " AND (s.to_date IS NULL OR s.to_date >= :pointInTime) " +
                "AND UPPER(v.items) like UPPER(" + searchString + ")", Quay.class );

        query.setParameter("pointInTime", Date.from(Instant.now()));


        List<Quay> resultList = query.getResultList();

         if (resultList.isEmpty()){
             return Optional.empty();
         }

        return Optional.of(resultList.get(0));

    }

    @Override
    public Optional<Quay> findTADQuay(String netexId) {


        Query query = entityManager.createNativeQuery("SELECT q.* " +
                "FROM quay q " +
                "INNER JOIN quay_key_values qkv ON qkv.quay_id = q.id " +
                "INNER JOIN value_items v  ON qkv.key_values_id = v.value_id " +
                "INNER JOIN stop_place_quays spq ON spq.quays_id = qkv.quay_id " +
                "INNER JOIN stop_place s ON s.id = spq.stop_place_id  " +

                "WHERE qkv.key_values_key = 'zonalStopPlace' " +
                " AND q.netex_id = :netexIdParam " +
                " AND  (s.from_date IS NULL OR s.from_date <= :pointInTime) " +
                " AND (s.to_date IS NULL OR s.to_date >= :pointInTime) " +
                "AND v.items IN ('yes','partial')", Quay.class );

        query.setParameter("netexIdParam", netexId);
        query.setParameter("pointInTime", Date.from(Instant.now()));


        List<Quay> resultList = query.getResultList();

        if (resultList.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(resultList.get(0));
    }

}
