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

package org.rutebanken.tiamat.model;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.junit.Test;
import org.rutebanken.tiamat.config.GeometryFactoryConfig;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class QuayEqualsTest {

    private GeometryFactory geometryFactory = new GeometryFactoryConfig().geometryFactory();

    @Test
    public void quaysWithSameNameAndCoordinatesEquals() {
        /**
         * Quay{id=52036,
         *      name=Ellas minne (no),
         *      keyValues={imported-id=Value{id=52042,
         *      items=[NOR:StopArea:1805014601]}}}
         *
         *  Quay{name=Ellas minne (no),
         *      keyValues={imported-id=Value{id=0, items=[TRO:StopArea:1805014601]}}}
         */

        double longitude = 39.61441;
        double latitude = -144.22765;

        Quay quay1 = new Quay(new EmbeddableMultilingualString("Ellas minne"));
        Quay quay2 = new Quay(new EmbeddableMultilingualString("Ellas minne"));

        quay1.setCentroid(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        assertThat(quay1).isEqualTo(quay2);
    }

    @Test
    public void quaysWithDifferentNameButSameCoordinates() {

        double longitude = 39.61441;
        double latitude = -144.22765;

        Quay quay1 = new Quay(new EmbeddableMultilingualString("Ellas minne"));
        Quay quay2 = new Quay(new EmbeddableMultilingualString("Different"));

        quay1.setCentroid(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(longitude, latitude)));
        assertThat(quay1).isNotEqualTo(quay2);
    }

    @Test
    public void quaysWithSameNameButDifferentCoordinates() {
        Quay quay1 = new Quay(new EmbeddableMultilingualString("Ellas minne"));
        Quay quay2 = new Quay(new EmbeddableMultilingualString("Ellas minne"));

        quay1.setCentroid(geometryFactory.createPoint(new Coordinate(70, 80)));
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(60, 50)));
        assertThat(quay1).isNotEqualTo(quay2);
    }

    @Test
    public void quaysWithNoCoordinatesAndSameName() {
        Quay quay1 = new Quay(new EmbeddableMultilingualString("Ellas minne"));
        Quay quay2 = new Quay(new EmbeddableMultilingualString("Ellas minne"));
        assertThat(quay1).isEqualTo(quay2);
    }

    @Test
    public void quaysWithDifferentIdInKeyValNotEqual() {
        Quay quay1 = new Quay(new EmbeddableMultilingualString("Ellas minne"));
        quay1.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("1");
        Quay quay2 = new Quay(new EmbeddableMultilingualString("Ellas minne"));
        quay2.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("12");
        assertThat(quay1).isNotEqualTo(quay2);
    }

    @Test
    public void quaysWithSameNameAndIdEquals() {
        Quay quay1 = new Quay(new EmbeddableMultilingualString("Ellas minne"));
        quay1.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("1");
        Quay quay2 = new Quay(new EmbeddableMultilingualString("Ellas minne"));
        quay2.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("1");
        assertThat(quay1).isEqualTo(quay2);
    }

    @Test
    public void quaysWithSameNameAndIdEqualsEvenIfOrderDiffers() {
        Quay quay1 = new Quay(new EmbeddableMultilingualString("Ellas minne"));
        quay1.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("1");
        quay1.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("2");
        Quay quay2 = new Quay(new EmbeddableMultilingualString("Ellas minne"));
        quay2.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("2");
        quay2.getOrCreateValues(NetexIdMapper.ORIGINAL_ID_KEY).add("1");
        assertThat(quay1).isEqualTo(quay2);
    }

    @Test
    public void quaysWithSlightlyDifferentCoordinatesShouldNotBeEqaual() {
        double quayLatitude = 59.4221750629462661663637845776975154876708984375;
        double quayLongitude = 5.2646351097871768587310725706629455089569091796875;

        String name = "Name";
        Quay quay1 = new Quay(new EmbeddableMultilingualString(name));
        Quay quay2 = new Quay(new EmbeddableMultilingualString(name));

        quay1.setCentroid(geometryFactory.createPoint(new Coordinate(quayLongitude, quayLatitude)));
        quay2.setCentroid(geometryFactory.createPoint(new Coordinate(quayLongitude + 0.01, quayLatitude + 0.01)));
        assertThat(quay1).isNotEqualTo(quay2);
    }

    @Test
    public void quaysWithDifferentPublicCodeIsNotEqual() {
        Quay first = new Quay();
        first.setPublicCode("X");

        Quay second = new Quay();
        second.setPublicCode("Y");
        assertThat(first).isNotEqualTo(second);
    }
}
