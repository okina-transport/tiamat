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

package org.rutebanken.tiamat.general;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.StringUtils;
import org.h2.util.IOUtils;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.exporter.TypeEnumeration;
import org.rutebanken.tiamat.exporter.StreamingPublicationDelivery;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.netex.validation.NetexXmlReferenceValidator;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.service.BlobStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportJobWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ExportJobWorker.class);
    private static final ExecutorService exportService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("exporter-%d").build());

    private final Job job;
    private static StreamingPublicationDelivery streamingPublicationDelivery;
    private static String localExportPath;
    private final String fileNameWithoutExtention;
    private static BlobStoreService blobStoreService;
    private static JobRepository jobRepository;
    private static NetexXmlReferenceValidator netexXmlReferenceValidator;
    private final Provider provider;
    private LocalDateTime localDateTime; // ne peut pas être final. Vu qu'on ne le bouge pas, pas gênant mais dommage
    private static String tiamatExportDestination;
    private final TypeEnumeration exportType;

    public ExportJobWorker(Job job,
                           StreamingPublicationDelivery streamingPublicationDelivery,
                           String localExportPath,
                           String fileNameWithoutExtention,
                           BlobStoreService blobStoreService,
                           JobRepository jobRepository,
                           NetexXmlReferenceValidator netexXmlReferenceValidator,
                           Provider provider,
                           LocalDateTime localDateTime,
                           String tiamatExportDestination,
                           TypeEnumeration exportType) {
        this.job = job;
        this.streamingPublicationDelivery = streamingPublicationDelivery;
        this.localExportPath = localExportPath;
        this.fileNameWithoutExtention = fileNameWithoutExtention;
        this.blobStoreService = blobStoreService;
        this.jobRepository = jobRepository;
        this.netexXmlReferenceValidator = netexXmlReferenceValidator;
        this.provider = provider;
        this.localDateTime = localDateTime;
        this.tiamatExportDestination = tiamatExportDestination;
        this.exportType = exportType;
    }


    public void run() {
        logger.info("Started export job: {}", job);

        File providerDir = new File(localExportPath + File.separator + provider.name);
        final File localExportZipFile = new File(providerDir, File.separator+ job.getFileName());
        File localExportXmlFile = new File(localExportPath + File.separator + fileNameWithoutExtention + ".xml");
        try {

            switch (this.exportType) {
                case POI:
                    exportPOIToLocalXmlFile(localExportXmlFile, localDateTime);
                    break;
                case PARKING:
                    exportParkingsToLocalXmlFile(localExportXmlFile, localDateTime);
                    break;
                default:
                    exportToLocalXmlFile(localExportXmlFile, provider, localDateTime);
                    break;
            }

            logger.info("starting file validation");
            netexXmlReferenceValidator.validateNetexReferences(localExportXmlFile);
            logger.info("validation completed");
            localExportZipFile.getParentFile().mkdirs();
            localExportZipFile.createNewFile();

            logger.info("destination:" + tiamatExportDestination);
            if(StringUtils.equals(tiamatExportDestination, "local") || StringUtils.equals(tiamatExportDestination, "both")){
                exportToLocalZipFile(localExportZipFile, localExportXmlFile);
            }
            if(StringUtils.equals(tiamatExportDestination, "gcs") || StringUtils.equals(tiamatExportDestination, "both")){
                uploadToGcp(localExportZipFile);
            }

            job.setStatus(JobStatus.FINISHED);
            job.setFinished(Instant.now());
            logger.info("Export job done: {}", job);

        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            String message = "Error executing export job " + job.getId() + ". " + e.getClass().getSimpleName() + " - " + e.getMessage();
            logger.error("{}.\nExport job was {}", message, job, e);
            job.setMessage(message);
            if (e instanceof InterruptedException) {
                logger.info("The export job was interrupted: {}", job);
                Thread.currentThread().interrupt();
            }
        } finally {
            jobRepository.save(job);
            logger.info("Removing local file: {}", localExportXmlFile);
            localExportXmlFile.delete();
        }
    }

    private void exportToLocalXmlFile(File localExportXmlFile, Provider provider, LocalDateTime localDateTime) throws IOException, SAXException, JAXBException {
        logger.info("Start streaming publication delivery to local file {}", localExportXmlFile);
        FileOutputStream fileOutputStream = new FileOutputStream(localExportXmlFile);
        streamingPublicationDelivery.stream(fileOutputStream, provider, localDateTime, job.getId());
        logger.info("export to local file completed");
    }

    private void exportPOIToLocalXmlFile(File localExportXmlFile, LocalDateTime localDateTime) throws IOException, SAXException, JAXBException {
        logger.info("Start streaming publication delivery to local file {}", localExportXmlFile);
        FileOutputStream fileOutputStream = new FileOutputStream(localExportXmlFile);
        streamingPublicationDelivery.streamPOI(job.getExportParams(), fileOutputStream, localDateTime, job.getId());
    }

    private void exportParkingsToLocalXmlFile(File localExportXmlFile, LocalDateTime localDateTime) throws IOException, SAXException, JAXBException {
        logger.info("Start streaming publication delivery to local file {}", localExportXmlFile);
        FileOutputStream fileOutputStream = new FileOutputStream(localExportXmlFile);
        streamingPublicationDelivery.streamParkings(fileOutputStream, localDateTime, job.getId());
    }

    private void uploadToGcp(File localExportFile) {
        logger.info("Uploading to gcp: {} in folder: {}", job.getFileName(), job.getSubFolder());
        blobStoreService.upload(job.getSubFolder() + "/" + job.getFileName(), localExportFile);
    }

    private void exportToLocalZipFile(File localZipFile, File localExportZipFile) throws IOException {
        logger.info("Adding {} to zip file: {}", localExportZipFile, localZipFile);

        final FileOutputStream fileOutputStream = new FileOutputStream(localZipFile);
        final ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

        try {
            zipOutputStream.putNextEntry(new ZipEntry(fileNameWithoutExtention + ".xml"));

            InputStream fileInputStream = new FileInputStream(localExportZipFile);
            IOUtils.copy(fileInputStream, zipOutputStream);
            zipOutputStream.closeEntry();
            logger.info("Written to disk {}", localZipFile);
        } finally {
            try {
                zipOutputStream.close();
            } catch (IOException e) {
                throw new IOException("Could not close zipoutput stream for file: " + localZipFile, e);
            }
        }
    }
}
