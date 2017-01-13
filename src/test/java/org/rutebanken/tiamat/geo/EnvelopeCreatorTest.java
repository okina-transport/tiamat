package org.rutebanken.tiamat.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.rutebanken.tiamat.config.GeometryFactoryConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


public class EnvelopeCreatorTest {

    private GeometryFactory geometryFactory = new GeometryFactoryConfig().geometryFactory();
    private EnvelopeCreator envelopeCreator = new EnvelopeCreator(geometryFactory);

    @Test
    public void envelopeIntersectsWithCoordinate() throws FactoryException, TransformException {
        Coordinate coordinate = new Coordinate(59.858690, 10.493860);
        Coordinate coveredCoordinate = new Coordinate(59.858616, 10.493858);
        Point point = geometryFactory.createPoint(coordinate);

        final int meters = 9;

        verifyDistanceInMetersLessThanOrEqualTo(coordinate, coveredCoordinate, meters);

        Envelope envelope = envelopeCreator.createFromPoint(point, meters);

        assertThat(envelope.intersects(coveredCoordinate)).isTrue();

    }

    /**
     * Test that a coordinate does not intersect with the created envelope.
     */
    @Test
    public void envelopeDoesNotIntersectWithCoordinate() throws FactoryException, TransformException {
        Coordinate coordinate = new Coordinate(59.858690, 10.493860);
        Coordinate notCoveredCoordinate = new Coordinate(59.858684, 10.493682);
        Point point = geometryFactory.createPoint(coordinate);

        final int meters = 9;

        Envelope envelope = envelopeCreator.createFromPoint(point, meters);

        assertThat(envelope.intersects(notCoveredCoordinate)).isFalse();
    }

    @Test
    public void findUtmZone32() {
        // Nesbru, Asker: 59.858690, 10.493860
        String zone = envelopeCreator.findUtmCrs(10.493860);
        assertThat(zone).isEqualTo("EPSG:32632");
    }

    @Test
    public void findUtmZone33() {
        // Somewhere in Narvik: 68.437437, 17.426283
        String zone = envelopeCreator.findUtmCrs(17.426283);
        assertThat(zone).isEqualTo("EPSG:32633");
    }

    @Test
    public void findUtmZone35() {
        // Mehamn: 71.035717, 27.848786
        String zone = envelopeCreator.findUtmCrs(27.848786);
        assertThat(zone).isEqualTo("EPSG:32635");
    }

    /**
     * When testing with coordinates, verify that they are less than or equal in meters to each other
     */
    private void verifyDistanceInMetersLessThanOrEqualTo(Coordinate coordinate1, Coordinate coordinate2, int meters) throws TransformException {
        double distanceInMeters = JTS.orthodromicDistance(
                coordinate1,
                coordinate2,
                DefaultGeographicCRS.WGS84);
        assertThat(distanceInMeters).isLessThanOrEqualTo(meters);
    }

}