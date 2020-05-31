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

package org.rutebanken.tiamat.exporter;

import org.junit.Test;
import org.rutebanken.tiamat.geo.geo.Lambert;
import org.rutebanken.tiamat.geo.geo.LambertPoint;
import org.rutebanken.tiamat.geo.geo.LambertZone;

import static javax.xml.bind.JAXBContext.newInstance;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test streaming publication delivery with h2 database
 * {@link StreamingPublicationDeliveryTest} is without database and spring context.
 */
public class TestLambert {


    @Test
    public void test() {
        LambertPoint lambertPoint = Lambert.convertToLambert(2.34, 48.86, LambertZone.LambertIIExtended);
        System.out.printf("x : %f\n", lambertPoint.getX());
        System.out.printf("y : %f\n", lambertPoint.getY());
    }
}
