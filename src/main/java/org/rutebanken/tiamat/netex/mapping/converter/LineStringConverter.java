package org.rutebanken.tiamat.netex.mapping.converter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.LineStringType;
import org.rutebanken.tiamat.geo.DoubleValuesToCoordinateSequence;
import org.rutebanken.tiamat.netex.mapping.NetexMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;

@Component
public class LineStringConverter extends BidirectionalConverter<LineStringType, LineString> {

    private static final Logger logger = LoggerFactory.getLogger(LineStringConverter.class);

    private final GeometryFactory geometryFactory;

    private final DoubleValuesToCoordinateSequence doubleValuesToCoordinateSequence;

    @Autowired
    public LineStringConverter(GeometryFactory geometryFactory, DoubleValuesToCoordinateSequence doubleValuesToCoordinateSequence) {
        this.geometryFactory = geometryFactory;
        this.doubleValuesToCoordinateSequence = doubleValuesToCoordinateSequence;
    }

    @Override
    public LineString convertTo(LineStringType lineStringType, Type<LineString> type) {

        if(lineStringType.getPosList() != null) {
            List<Double> values = lineStringType.getPosList().getValue();

            CoordinateSequence coordinateSequence = doubleValuesToCoordinateSequence.convert(values);
            LineString lineString = new LineString(coordinateSequence, geometryFactory);

            return lineString;
        }

        return null;
    }

    @Override
    public LineStringType convertFrom(LineString lineString, Type<LineStringType> type) {

        LineStringType lineStringType = new LineStringType();

        DirectPositionListType directPositionListType = new DirectPositionListType();

        if(lineString.getCoordinates() != null) {
            logger.debug("Converting coordinates {}", lineString.getCoordinates());
            List<Double> positions = directPositionListType.getValue();
            for(Coordinate coordinate : lineString.getCoordinates()) {
                positions.add(coordinate.y);
                positions.add(coordinate.x);
            }
            directPositionListType.setCount(BigInteger.valueOf(positions.size()));
            directPositionListType.setSrsDimension(BigInteger.valueOf(2L));
        }
        lineStringType.setPosList(directPositionListType);
        lineStringType.setId(LineString.class.getSimpleName());
        lineStringType.setSrsDimension(BigInteger.valueOf(2L));

        return lineStringType;
    }
}
