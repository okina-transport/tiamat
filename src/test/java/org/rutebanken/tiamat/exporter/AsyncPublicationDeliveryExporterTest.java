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
import org.junit.Assert;
import org.junit.Test;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.Value;
import org.rutebanken.tiamat.model.job.ExportJob;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.repository.ExportJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ZDEP_KEY;

public class AsyncPublicationDeliveryExporterTest extends TiamatIntegrationTest {

    @Qualifier("syncStreamingPublicationDelivery")
    @Autowired
    private StreamingPublicationDelivery streamingPublicationDelivery;


    @Autowired
    private AsyncPublicationDeliveryExporter asyncPublicationDeliveryExporter;

    @Autowired
    private ExportJobRepository exportJobRepository;

    @Test
    public void test() throws InterruptedException, JAXBException, IOException, SAXException {

        asyncPublicationDeliveryExporter.providerRepository = getProviderRepository();

        final int numberOfStopPlaces = StopPlaceSearch.DEFAULT_PAGE_SIZE;
        for (int i = 0; i < numberOfStopPlaces; i++) {
            StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString("stop place numbber " + i));
            stopPlace.setVersion(1L);
            stopPlace.setProvider("test");


            Quay quay = new Quay();
            quay.setNetexId("NSR:Quay:" + i);
            quay.setName(new EmbeddableMultilingualString("Quay_" + i));
            quay.setPublicCode("quay" + i);
            quay.getKeyValues().put(ORIGINAL_ZDEP_KEY, new Value("yolo_" + i));
            quay.setCentroid(geometryFactory.createPoint(new Coordinate(48, 2)));
            quay.setZipCode("75000");

            stopPlace.getQuays().add(quay);

            stopPlaceRepository.save(stopPlace);

        }
        stopPlaceRepository.flush();


        Provider provider = getProviderRepository().getProviders().iterator().next();
        ExportParams exportParams = ExportParams.newExportParamsBuilder()
                .setStopPlaceSearch(
                        StopPlaceSearch
                                .newStopPlaceSearchBuilder()
                                .setVersionValidity(ExportParams.VersionValidity.ALL)
                                .build())
                .setProviderId(provider.getId())
                .build();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        streamingPublicationDelivery.stream(exportParams, byteArrayOutputStream, true, provider);
        asyncPublicationDeliveryExporter.streamingPublicationDelivery = streamingPublicationDelivery;

        ExportJob exportJob = asyncPublicationDeliveryExporter.startExportJob(exportParams);

        assertThat(exportJob.getId()).isGreaterThan(0L);

        long start = System.currentTimeMillis();
        long timeout = 10000;
        while (true) {
            Optional<ExportJob> actualExportJob = exportJobRepository.findById(exportJob.getId());
            if (actualExportJob.get().getStatus().equals(exportJob.getStatus())) {
                if (System.currentTimeMillis() - start > timeout) {
                    fail("Waited more than " + timeout + " millis for job status to change");
                }
                Thread.sleep(1000);
                continue;
            }

            if (actualExportJob.get().getStatus().equals(JobStatus.FAILED)) {
                fail("Job status is failed");
            } else if (actualExportJob.get().getStatus().equals(JobStatus.FINISHED)) {
                System.out.println("Job finished");

            }
        }
    }

    @Test
    public void testName() {
        // GIVEN

        // WHEN
        String sqybus = asyncPublicationDeliveryExporter.createFileNameWithoutExtention("41", "SQYBUS", LocalDateTime.now());

        // THEN
        Assert.assertTrue(sqybus.length() > 0);
    }
}
