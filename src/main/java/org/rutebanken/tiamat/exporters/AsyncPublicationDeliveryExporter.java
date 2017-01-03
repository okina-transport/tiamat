package org.rutebanken.tiamat.exporters;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.tiamat.model.job.ExportJob;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.repository.ExportJobRepository;
import org.rutebanken.tiamat.repository.StopPlaceSearch;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryStreamingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AsyncPublicationDeliveryExporter {

    private static final Logger logger = LoggerFactory.getLogger(AsyncPublicationDeliveryExporter.class);

    private static final ExecutorService exportService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setNameFormat("publication-delivery-exporter-%d").build());

    private final PublicationDeliveryExporter publicationDeliveryExporter;

    private final PublicationDeliveryStreamingOutput publicationDeliveryStreamingOutput;

    private final ExportJobRepository exportJobRepository;

    @Autowired
    public AsyncPublicationDeliveryExporter(PublicationDeliveryExporter publicationDeliveryExporter, PublicationDeliveryStreamingOutput publicationDeliveryStreamingOutput, ExportJobRepository exportJobRepository) {
        this.publicationDeliveryExporter = publicationDeliveryExporter;
        this.publicationDeliveryStreamingOutput = publicationDeliveryStreamingOutput;
        this.exportJobRepository = exportJobRepository;
    }

    public ExportJob startExportJob(StopPlaceSearch stopPlaceSearch) {

        ExportJob exportJob = new ExportJob(JobStatus.PROCESSING);
        exportJobRepository.save(exportJob);


        exportService.submit(new Callable<String>() {
            @Override
            public String call() {
                logger.info("Started export job {}", exportJob);
                PublicationDeliveryStructure publicationDeliveryStructure = publicationDeliveryExporter.exportStopPlaces(stopPlaceSearch);

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                try {
                    StreamingOutput streamingOutput = publicationDeliveryStreamingOutput.stream(publicationDeliveryStructure);
                    streamingOutput.write(byteArrayOutputStream);
                    String xml = byteArrayOutputStream.toString();
                    Thread.sleep(5000);

                    exportJob.setStatus(JobStatus.FINISHED);
                    logger.info("Export job {} done", exportJob);
                    return xml;

                } catch (JAXBException | IOException | SAXException | InterruptedException e) {
                    exportJob.setStatus(JobStatus.FAILED);
                    String message = "Error executing export job " + exportJob;
                    logger.error(message, e);
                    return message;
                } finally {
                    exportJobRepository.save(exportJob);
                }
            }
        });
        exportJob.setJobUrl("export_job/" + exportJob.getId());
        exportJobRepository.save(exportJob);
        return exportJob;
    }

    public Collection<ExportJob> getJobs() {
        return exportJobRepository.findAll();
    }


}
