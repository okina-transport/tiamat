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

import com.vividsolutions.jts.geom.Coordinate;
import org.junit.Test;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.validation.NeTExValidator;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.rutebanken.tiamat.geo.geo.Lambert;
import org.rutebanken.tiamat.geo.geo.LambertPoint;
import org.rutebanken.tiamat.geo.geo.LambertZone;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.GroupOfStopPlaces;
import org.rutebanken.tiamat.model.PrivateCodeStructure;
import org.rutebanken.tiamat.model.Provider;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.StopPlaceReference;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.model.TariffZone;
import org.rutebanken.tiamat.model.TariffZoneRef;
import org.rutebanken.tiamat.model.TopographicPlace;
import org.rutebanken.tiamat.model.TopographicPlaceRefStructure;
import org.rutebanken.tiamat.model.TopographicPlaceTypeEnumeration;
import org.rutebanken.tiamat.model.ValidBetween;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.netex.validation.NetexReferenceValidatorException;
import org.rutebanken.tiamat.netex.validation.NetexXmlReferenceValidator;
import org.rutebanken.tiamat.repository.ProviderRepository;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryTestHelper;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryUnmarshaller;
import org.rutebanken.tiamat.versioning.save.GroupOfStopPlacesSaverService;
import org.rutebanken.tiamat.versioning.save.TariffZoneSaverService;
import org.rutebanken.tiamat.versioning.save.TopographicPlaceVersionedSaverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;

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
