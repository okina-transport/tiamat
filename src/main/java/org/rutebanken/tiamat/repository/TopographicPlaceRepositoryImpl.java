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


import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.rutebanken.tiamat.exporter.params.TopographicPlaceSearch;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingArea;
import org.rutebanken.tiamat.model.TopographicPlace;
import org.rutebanken.tiamat.model.TopographicPlaceTypeEnumeration;
import org.rutebanken.tiamat.repository.iterator.ScrollableResultIterator;
import org.rutebanken.tiamat.repository.search.SearchHelper;
import org.rutebanken.tiamat.repository.search.TopographicPlaceQueryFromSearchBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Transactional
public class TopographicPlaceRepositoryImpl implements TopographicPlaceRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private SearchHelper searchHelper;


	@Autowired
	private TopographicPlaceQueryFromSearchBuilder topographicPlaceQueryFromSearchBuilder;

	@Override
	public List<TopographicPlace> findTopographicPlace(TopographicPlaceSearch topographicPlaceSearch) {

		Pair<String, Map<String, Object>> queryWithParams = topographicPlaceQueryFromSearchBuilder.buildQueryString(topographicPlaceSearch);

		final Query nativeQuery = entityManager.createNativeQuery(queryWithParams.getFirst(), TopographicPlace.class);

		queryWithParams.getSecond().forEach(nativeQuery::setParameter);

		List<TopographicPlace> topographicPlaces = nativeQuery.getResultList();
		return topographicPlaces;
	}

	@Override
	public String findFirstByKeyValues(String key, Set<String> originalIds) {
		throw new NotImplementedException("findByKeyvalue not implemented for topographic place");
	}

	@Override
	public List<TopographicPlace> findByNameAndTypeMaxVersion(String name, TopographicPlaceTypeEnumeration topographicPlaceType) {

		Map<String, Object> parameters = new HashMap<>();
		StringBuilder sql = new StringBuilder("SELECT tp.* FROM topographic_place tp WHERE " +
				"tp.version = (SELECT MAX(tpv.version) FROM topographic_place tpv WHERE tpv.netex_id = tp.netex_id " +
				"and (tpv.to_date is null or tpv.to_date > :pointInTime) and (tpv.from_date is null or tpv.from_date < :pointInTime)) ");
		Instant pointInTime = Instant.now();
		parameters.put("pointInTime", pointInTime);

        if(topographicPlaceType != null) {
            sql.append("AND tp.topographic_place_type = :topographicPlaceType ");
            parameters.put("topographicPlaceType", topographicPlaceType.name());
        }

        if(!StringUtils.isBlank(name)) {
            sql.append("AND similarity(tp.name_value, :name) > 0.2 ");
            parameters.put("name", name);
            sql.append("ORDER BY SIMILARITY(tp.name_value, :name) DESC");
        }

		Query query = entityManager.createNativeQuery(sql.toString(), TopographicPlace.class);
        parameters.forEach(query::setParameter);

		return query.getResultList();
	}

	@Override
	public Iterator<TopographicPlace> scrollTopographicPlaces(Set<Long> stopPlaceDbIds) {

		if(stopPlaceDbIds == null || stopPlaceDbIds.isEmpty()) {
			return new ArrayList<TopographicPlace>().iterator();
		}

		return scrollTopographicPlaces(generateTopographicPlacesQueryFromStopPlaceIds(stopPlaceDbIds));
	}

	@Override
	public Iterator<TopographicPlace> scrollTopographicPlaces() {
		return scrollTopographicPlaces("SELECT t.* FROM topographic_place t");
	}

	public Iterator<TopographicPlace> scrollTopographicPlaces(String sql) {
		Session session = entityManager.unwrap(Session.class);
		NativeQuery sqlQuery = session.createNativeQuery(sql);

		sqlQuery.addEntity(TopographicPlace.class);
		sqlQuery.setReadOnly(true);
		sqlQuery.setFetchSize(1000);
		sqlQuery.setCacheable(false);
		ScrollableResults results = sqlQuery.scroll(ScrollMode.FORWARD_ONLY);
		ScrollableResultIterator<TopographicPlace> topographicPlaceIterator = new ScrollableResultIterator<>(results, 100, session);
		return  topographicPlaceIterator;
	}

	@Override
	public List<TopographicPlace> getTopographicPlacesFromStopPlaceIds(Set<Long> stopPlaceDbIds) {
		if(stopPlaceDbIds == null || stopPlaceDbIds.isEmpty()) {
			return new ArrayList<>();
		}
		Query query = entityManager.createNativeQuery(generateTopographicPlacesQueryFromStopPlaceIds(stopPlaceDbIds), TopographicPlace.class);

		try {
			@SuppressWarnings("unchecked")
			List<TopographicPlace> results = query.getResultList();
			if (results.isEmpty()) {
				return null;
			} else {
				return results;
			}
		} catch (NoResultException noResultException) {
			return null;
		}
	}

	private String generateTopographicPlacesQueryFromStopPlaceIds(Set<Long> stopPlaceDbIds) {

		Set<String> stopPlaceStringDbIds = stopPlaceDbIds.stream().map(lvalue -> String.valueOf(lvalue)).collect(Collectors.toSet());
		String joinedStopPlaceDbIds = String.join(",", stopPlaceStringDbIds);
		StringBuilder sql = new StringBuilder("SELECT tp.* " +
				"FROM ( " +
				"  SELECT tp1.id " +
				"  FROM topographic_place tp1 " +
				"  INNER JOIN stop_place sp " +
				"    ON sp.topographic_place_id = tp1.id " +
				"  WHERE sp.id IN(");
		sql.append(joinedStopPlaceDbIds);
		sql.append(") " +
				"  GROUP BY tp1.id " +
				") tp1 " +
				"JOIN topographic_place tp ON tp.id = tp1.id");
		return sql.toString();
	}

	/**
	 * Initialize export job table with stop ids that must be exported
	 * @param exportJobId
	 *  id of the export job
	 */
	@org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
	public void initExportJobTable( Long exportJobId){

		Map<String, Object> parameters = new HashMap<>();

		String queryStr = "INSERT INTO export_job_id_list \n" +
				" SELECT :exportJobId,req1.topo_id     \n" +
				" FROM ( \n" +
				" SELECT max(tp.id)as topo_id,MAX(tp.version) as version FROM topographic_place tp  WHERE  (tp.from_date <= :pointInTime OR  tp.from_date IS NULL) \n" +
				" AND (   tp.to_date >= :pointInTime  OR tp.to_date IS NULL) GROUP BY tp.netex_id  ) req1";


		parameters.put("exportJobId", exportJobId);
		parameters.put("pointInTime", Date.from(Instant.now()));

		Session session = entityManager.unwrap(Session.class);
		NativeQuery query = session.createNativeQuery(queryStr);
		searchHelper.addParams(query, parameters);

		query.executeUpdate();

	}

	/**
	 * Add parent_ topographic places that must be exported to table export_job_id_list
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void addParentTopographicPlacesToExportJobTable(Long exportJobId){


		Map<String, Object> parameters = new HashMap<>();
		String queryStr = "INSERT INTO export_job_id_list \n" +
				" SELECT :exportJobId, tp.id FROM topographic_place tp WHERE \n" +
				" tp.from_date <= :pointInTime \n" +
				" AND (   tp.to_date >= :pointInTime  or tp.to_date IS NULL) \n" +
				" AND tp.netex_id in ( \n" +
				" SELECT distinct tp2.parent_ref FROM topographic_place tp2 WHERE tp2.parent_ref IS NOT NULL \n" +
				" AND tp2.id IN (SELECT exported_object_id FROM export_job_id_list WHERE job_id = :exportJobId) ) \n";



		Session session = entityManager.unwrap(Session.class);
		parameters.put("exportJobId", exportJobId);
		parameters.put("pointInTime",  Date.from(Instant.now()));

		NativeQuery query = session.createNativeQuery(queryStr);
		searchHelper.addParams(query, parameters);
		query.executeUpdate();

	}

	public List<TopographicPlace> getTopoPlacesInitializedForExport(Set<Long> topoIds) {

		Set<String> topoIdsString = topoIds.stream().map(lvalue -> String.valueOf(lvalue)).collect(Collectors.toSet());

		String joinedTopoIds = String.join(",", topoIdsString);
		StringBuilder sql = new StringBuilder("SELECT tp FROM TopographicPlace tp WHERE tp.id IN(");
		sql.append(joinedTopoIds);
		sql.append(")");


		TypedQuery<TopographicPlace> q = entityManager.createQuery(sql.toString(), TopographicPlace.class);

		List<TopographicPlace> results = q.getResultList();


		results.forEach(topoPlace-> {
			Hibernate.initialize(topoPlace.getKeyValues());
			topoPlace.getKeyValues().values().forEach(value -> Hibernate.initialize(value.getItems()));
			Hibernate.initialize(topoPlace.getPolygon());

		});

		return results;
	}
}
