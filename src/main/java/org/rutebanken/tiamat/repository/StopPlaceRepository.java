package org.rutebanken.tiamat.repository;

import com.vividsolutions.jts.geom.Point;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.rutebanken.tiamat.model.StopPlace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Iterator;
import java.util.List;

public interface StopPlaceRepository extends StopPlaceRepositoryCustom, EntityInVersionRepository<StopPlace> {

    Page<StopPlace> findAllByOrderByChangedDesc(Pageable pageable);

    StopPlace findByNameValueAndCentroid(String name, Point geometryPoint);

    @Query(value = "select s.* from stop_place s where s.parent_site_ref = :#{#ref} and s.parent_site_ref_version = :#{#version}", nativeQuery = true)
    List<StopPlace> findByParentRef(@Param("ref") String ref, @Param("version") String version);

    Page<StopPlace> findByNameValueContainingIgnoreCaseOrderByChangedDesc(String name, Pageable pageable);

    @Override
    Iterator<StopPlace> scrollStopPlaces();

    @Override
    Iterator<StopPlace> scrollStopPlaces(ExportParams exportParams);

}

