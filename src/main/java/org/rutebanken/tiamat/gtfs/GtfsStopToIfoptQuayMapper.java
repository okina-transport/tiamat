package org.rutebanken.tiamat.gtfs;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.rutebanken.tiamat.model.*;
import org.onebusaway.gtfs.model.Stop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GtfsStopToIfoptQuayMapper {

    private static final Logger logger = LoggerFactory.getLogger(GtfsStopToIfoptQuayMapper.class);

    private static final int DECIMAL_PLACES = 8;

    @Autowired
    private GeometryFactory geometryFactory;

    public Quay mapToQuay(Stop stop){
    	logger.debug("Mapping data from GTFS stop to IFOPT quay {} - {}", stop.getId(), stop.getName());
    	Quay quay = new Quay();
    	quay.setName(new MultilingualString(stop.getName(), "no"));
    	quay.setDescription(new MultilingualString(stop.getDesc(), "no"));
    	
    	centroid(quay, stop);
    	
    	return quay;
    }

    public void centroid(Zone_VersionStructure site, Stop stop) {
        logger.trace("Setting location for {} latitude: {}, longitude: {}", site.getName(), stop.getLat(), stop.getLon());
        site.setCentroid(geometryFactory.createPoint(new Coordinate(stop.getLon(), stop.getLat())));
    }
}

