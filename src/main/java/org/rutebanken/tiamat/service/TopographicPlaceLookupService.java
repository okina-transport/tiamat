package org.rutebanken.tiamat.service;


import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.rutebanken.tiamat.general.ResettableMemoizer;
import org.rutebanken.tiamat.model.Site_VersionStructure;
import org.rutebanken.tiamat.model.TopographicPlace;
import org.rutebanken.tiamat.model.TopographicPlaceTypeEnumeration;
import org.rutebanken.tiamat.repository.TopographicPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

@Service
@Transactional
public class TopographicPlaceLookupService {

    private static final Logger logger = LoggerFactory.getLogger(TopographicPlaceLookupService.class);

    private static final List<TopographicPlaceTypeEnumeration> ADMIN_LEVEL_ORDER = Arrays.asList(TopographicPlaceTypeEnumeration.TOWN, TopographicPlaceTypeEnumeration.COUNTY, TopographicPlaceTypeEnumeration.STATE);


    private final Supplier<List<ImmutableTriple<String, TopographicPlaceTypeEnumeration, Polygon>>> topographicPlaces = Suppliers.memoizeWithExpiration(getTopographicPlaceSupplier(), 10, TimeUnit.HOURS);

//    private final ResettableMemoizer<List<ImmutableTriple<String, TopographicPlaceTypeEnumeration, Polygon>>> memoizedTopographicPlaces = new ResettableMemoizer<>(getTopographicPlaceSupplier2());

    @Autowired
    private TopographicPlaceRepository topographicPlaceRepository;

    public boolean populateTopographicPlaceRelation(Site_VersionStructure siteVersionStructure) {

        if (!siteVersionStructure.hasCoordinates()) {
            return false;
        }

        Optional<TopographicPlace> topographicPlace = findTopographicPlace(siteVersionStructure.getCentroid());

        if (topographicPlace.isPresent()) {
            logger.debug("Found topographic place {} for site {}", siteVersionStructure.getTopographicPlace(), siteVersionStructure);
            siteVersionStructure.setTopographicPlace(topographicPlace.get());
            return true;
        } else {
            logger.warn("Could not find topographic places from site's point: {}", siteVersionStructure);
            return false;
        }
    }

    public Optional<TopographicPlace> findTopographicPlace(Point point) {
        return topographicPlaces.get()
                       .stream()
                       .filter(triple -> point.within(triple.getRight()))
                       .map(triple -> topographicPlaceRepository.findFirstByNetexIdOrderByVersionDesc(triple.getLeft()))
                       .filter(topographicPlace -> topographicPlace != null)
                       .findAny();
    }

    public Optional<TopographicPlace> findTopographicPlaceByReference(List<String> topographicPlaceReferences, Point point) {
        return topographicPlaces.get()
                .stream()
                .filter(triple -> topographicPlaceReferences.contains(triple.getLeft()))
                .filter(triple -> point.within(triple.getRight()))
                .map(triple -> topographicPlaceRepository.findFirstByNetexIdOrderByVersionDesc(triple.getLeft()))
                .peek(topographicPlace -> logger.debug("Found topographic place match: {}", topographicPlace.getNetexId()))
                .findAny();
    }

//    private java.util.function.Supplier<List<ImmutableTriple<String, TopographicPlaceTypeEnumeration, Polygon>>> getTopographicPlaceSupplier2() {
//        return () -> {
//            logger.info("Fetching topographic places from repository");
//            return topographicPlaceRepository.findAllMaxVersion()
//                    .stream()
//                    .filter(topographicPlace -> topographicPlace.getPolygon() != null)
//                    .filter(topographicPlace -> ADMIN_LEVEL_ORDER.contains(topographicPlace.getTopographicPlaceType()))
//                    .sorted(new TopographicPlaceByAdminLevelComparator())
//                    .map(topographicPlace -> ImmutableTriple.of(topographicPlace.getNetexId(), topographicPlace.getTopographicPlaceType(), topographicPlace.getPolygon()))
//                    .collect(toList());
//        };
//    }

    private Supplier<List<ImmutableTriple<String, TopographicPlaceTypeEnumeration, Polygon>>> getTopographicPlaceSupplier() {
        return () -> {
            logger.info("Fetching topographic places from repository");
            return topographicPlaceRepository.findAllMaxVersion()
                    .stream()
                    .filter(topographicPlace -> topographicPlace.getPolygon() != null)
                    .filter(topographicPlace -> ADMIN_LEVEL_ORDER.contains(topographicPlace.getTopographicPlaceType()))
                    .sorted(new TopographicPlaceByAdminLevelComparator())
                    .map(topographicPlace -> ImmutableTriple.of(topographicPlace.getNetexId(), topographicPlace.getTopographicPlaceType(), topographicPlace.getPolygon()))
                    .collect(toList());
        };
    }

    private class TopographicPlaceByAdminLevelComparator implements Comparator<TopographicPlace> {
        @Override
        public int compare(TopographicPlace tp1, TopographicPlace tp2) {
            return ADMIN_LEVEL_ORDER.indexOf(tp1.getTopographicPlaceType()) - ADMIN_LEVEL_ORDER.indexOf(tp2.getTopographicPlaceType());
        }
    }


}
