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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.general.JobWorker;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.netex.validation.NetexXmlReferenceValidator;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.repository.ProviderRepository;
import org.rutebanken.tiamat.service.BlobStoreService;
import org.rutebanken.tiamat.time.ExportTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toList;
import static org.rutebanken.tiamat.rest.netex.publicationdelivery.AsyncExportResource.ASYNC_JOB_PATH;

@Service
public class AsyncPublicationDeliveryExporter {

    private static final Logger logger = LoggerFactory.getLogger(AsyncPublicationDeliveryExporter.class);

    private static final ExecutorService exportService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("exporter-%d").build());

    private final JobRepository jobRepository;

    private final BlobStoreService blobStoreService;

    StreamingPublicationDelivery streamingPublicationDelivery;

    private final NetexXmlReferenceValidator netexXmlReferenceValidator;

    private final String localExportPath;

    ProviderRepository providerRepository;

    private final String tiamatExportDestination;

    @Autowired
    public AsyncPublicationDeliveryExporter(JobRepository jobRepository,
                                            BlobStoreService blobStoreService,
                                            @Qualifier("asyncStreamingPublicationDelivery") StreamingPublicationDelivery streamingPublicationDelivery,
                                            NetexXmlReferenceValidator netexXmlReferenceValidator, ExportTimeZone exportTimeZone,
                                            @Value("${async.export.path:/deployments/data/}") String localExportPath, ProviderRepository providerRepository,
                                            @Value("${tiamat.export.destination:both}") String tiamatExportDestination) {
        this.jobRepository = jobRepository;
        this.blobStoreService = blobStoreService;
        this.streamingPublicationDelivery = streamingPublicationDelivery;
        this.netexXmlReferenceValidator = netexXmlReferenceValidator;
        this.localExportPath = localExportPath;
        this.providerRepository = providerRepository;
        this.tiamatExportDestination = tiamatExportDestination;

        File exportFolder = new File(localExportPath);
        if (!exportFolder.exists() && !exportFolder.mkdirs()) {
            throw new RuntimeException("Cannot find or create export directory from path: " + localExportPath +
                    ". Please create the directory with correct permissions, or configure a different path with the property async.export.path");
        }
        if (!exportFolder.canWrite()) {
            throw new RuntimeException("Cannot write to path: " + localExportPath +
                    ". Please create the directory with correct permissions, or configure a different path with the property async.export.path");
        }
        logger.info("Verified local export path {}", localExportPath);
    }

    /**
     * Start export job with upload to google cloud storage
     *
     * @param exportParams search params for stops
     * @return export job with information about the started process
     */
    public Job startExportJob(ExportParams exportParams) {

        Iterable<Provider> providers;

        providers = Collections.singletonList(providerRepository.getProvider(exportParams.getProviderId()));

        Job job = new Job(JobStatus.PROCESSING);

        providers.forEach(provider -> {
            if(provider != null) {
                logger.info("Starting export {} for provider {}", job.getId(), provider.id + "/" + provider.chouetteInfo.codeIdfm);
                job.setStarted(Instant.now());
                job.setExportParams(exportParams);
                job.setSubFolder(provider.name);

                LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
                jobRepository.save(job);
                String idSite = provider.getChouetteInfo().getCodeIdfm();

                String nameSite = provider.name;
                boolean isPrefix = StringUtils.isNotBlank(provider.getChouetteInfo().getNameNetexStop());
                if(isPrefix) {
                    nameSite = provider.getChouetteInfo().getNameNetexStop();
                }

                String fileNameWithoutExtention = createFileNameWithoutExtention(idSite, nameSite, localDateTime,isPrefix);

                String nameFileZip = null;
                try {
                    nameFileZip = URLEncoder.encode(fileNameWithoutExtention, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                job.setFileName(nameFileZip + ".zip");

                JobWorker jobWorker = new JobWorker(job, streamingPublicationDelivery, localExportPath, fileNameWithoutExtention, blobStoreService, jobRepository, netexXmlReferenceValidator, provider, localDateTime, tiamatExportDestination, TypeEnumeration.STOP_PLACE);
                exportService.submit(jobWorker);
                logger.info("Returning started export job {}", job);
                setJobUrl(job);
            }
        });

        return job;
    }

    /**
     * Start Parkings export job
     *
     * @param exportParams search params for parkings
     * @return export job with information about the started process
     */
    public Job startParkingsExportJob(ExportParams exportParams) {
        Iterable<Provider> providers;
        providers = Collections.singletonList(providerRepository.getProvider(exportParams.getProviderId()));

        Job job = new Job(JobStatus.PROCESSING);

        providers.forEach(provider -> {
            if(provider != null) {
                logger.info("Starting parkings export {} for provider {}", job.getId(), provider.id + "/" + provider.chouetteInfo.codeIdfm);
                job.setStarted(Instant.now());
                job.setExportParams(exportParams);
                job.setSubFolder(provider.name);

                LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
                jobRepository.save(job);
                String idSite = provider.getChouetteInfo().getCodeIdfm();

                String nameSite = provider.name;
                if(StringUtils.isNotBlank(provider.getChouetteInfo().getNameNetexStop())) {
                    nameSite = provider.getChouetteInfo().getNameNetexStop();
                }

                String fileNameWithoutExtention = createParkingsFileNameWithoutExtension(idSite, nameSite, localDateTime);

                String nameFileZip = null;
                try {
                    nameFileZip = URLEncoder.encode(fileNameWithoutExtention, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                job.setFileName(nameFileZip + ".zip");

                JobWorker jobWorker = new JobWorker(job, streamingPublicationDelivery, localExportPath, fileNameWithoutExtention, blobStoreService, jobRepository, netexXmlReferenceValidator, provider, localDateTime, tiamatExportDestination, TypeEnumeration.PARKING);
                exportService.submit(jobWorker);
                logger.info("Returning started parkings export job {}", job);
                setJobUrl(job);
            }
        });

        return job;
    }

    /**
     * Start POI export job with upload to google cloud storage
     *
     * @param exportParams search params for points of interest
     * @return export job with information about the started process
     */
    public Job startPOIExportJob(ExportParams exportParams) {

        Iterable<Provider> providers;
        providers = Collections.singletonList(providerRepository.getProvider(exportParams.getProviderId()));

        Job job = new Job(JobStatus.PROCESSING);

        providers.forEach(provider -> {
            if(provider != null) {
                logger.info("Starting poi export {} for provider {}", job.getId(), provider.id + "/" + provider.chouetteInfo.codeIdfm);
                job.setStarted(Instant.now());
                job.setExportParams(exportParams);
                job.setSubFolder(provider.name);

                LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
                jobRepository.save(job);
                String idSite = provider.getChouetteInfo().getCodeIdfm();

                String nameSite = provider.name;
                if(StringUtils.isNotBlank(provider.getChouetteInfo().getNameNetexStop())) {
                    nameSite = provider.getChouetteInfo().getNameNetexStop();
                }

                String fileNameWithoutExtention = createPOIFileNameWithoutExtension(idSite, nameSite, localDateTime);

                String nameFileZip = null;
                try {
                    nameFileZip = URLEncoder.encode(fileNameWithoutExtention, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                job.setFileName(nameFileZip + ".zip");

                JobWorker jobWorker = new JobWorker(job, streamingPublicationDelivery, localExportPath, fileNameWithoutExtention, blobStoreService, jobRepository, netexXmlReferenceValidator, provider, localDateTime, tiamatExportDestination, TypeEnumeration.POI);
                exportService.submit(jobWorker);
                logger.info("Returning started POI export job {}", job);
                setJobUrl(job);
            }
        });

        return job;
    }

    public String createFileNameWithoutExtention(String idSite, String nameSite, LocalDateTime localDateTime, boolean isPrefix) {
        if (isPrefix){
            return "ARRET_" + nameSite + "_" + nameSite + "_T_" + localDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "T" + localDateTime.format(DateTimeFormatter.ofPattern("HHmmss")) + "Z";
        } else {
            return "ARRET_" + idSite + "_" + nameSite + "_T_" + localDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "T" + localDateTime.format(DateTimeFormatter.ofPattern("HHmmss")) + "Z";
        }
    }

        public String createPOIFileNameWithoutExtension(String idSite, String nameSite, LocalDateTime localDateTime) {
            return "POI_" + idSite + "_" + nameSite + "_T_" + localDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "T" + localDateTime.format(DateTimeFormatter.ofPattern("HHmmss")) + "Z";
        }

    public String createParkingsFileNameWithoutExtension(String idSite, String nameSite, LocalDateTime localDateTime) {
        return "PARKING_" + idSite + "_" + nameSite + "_T_" + localDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "T" + localDateTime.format(DateTimeFormatter.ofPattern("HHmmss")) + "Z";
    }

    public Job getExportJob(long exportJobId) {

        Optional<Job> exportJob = jobRepository.findById(exportJobId);
        if (exportJob.isPresent()) {
            return setJobUrl(exportJob.get());
        }
        return null;
    }

    public InputStream getJobFileContent(Job job) {
        return blobStoreService.download(job.getSubFolder() + "/" + job.getFileName());
    }

    public File getJobFileContent(String providerName, String filePath) {

        if (StringUtils.equals(tiamatExportDestination, "local")){
            File providerDir = new File(localExportPath + File.separator +providerName);
            return new File(providerDir,filePath);

        }else{
            return blobStoreService.downloadFromAbsolutePath(filePath);
        }
    }

    public Collection<Job> getJobs() {

        return jobRepository.findAll()
                .stream()
                .map(this::setJobUrl)
                .collect(toList());
    }

    private Job setJobUrl(Job jobWithId) {
        jobWithId.setJobUrl(ASYNC_JOB_PATH + "/" + jobWithId.getId());
        return jobWithId;
    }

    public List<String> getStopPlaceFileListByProviderName(String providerName, int maxNbResults){
        List<String> stopPlaceFileList = new ArrayList<>();

        if (StringUtils.equals(tiamatExportDestination, "local") || StringUtils.equals(tiamatExportDestination, "both")){
            stopPlaceFileList.addAll(getStopPlaceFileListFromLocalStorage(providerName));
        }

        if (StringUtils.equals(tiamatExportDestination, "gcs") || StringUtils.equals(tiamatExportDestination, "both")){
            stopPlaceFileList.addAll(blobStoreService.listStopPlacesInBlob(providerName,maxNbResults));
        }
        Collections.sort(stopPlaceFileList, Collections.reverseOrder());
        return stopPlaceFileList;
    }

    public List<String> getPointsOfInterestFileListByProviderName(String providerName, int maxNbResults){
        List<String> poiFileList = new ArrayList<>();

        if (StringUtils.equals(tiamatExportDestination, "local") || StringUtils.equals(tiamatExportDestination, "both")){
            poiFileList.addAll(getPointsOfInterestFileListFromLocalStorage(providerName));
        }

        if (StringUtils.equals(tiamatExportDestination, "gcs") || StringUtils.equals(tiamatExportDestination, "both")){
            poiFileList.addAll(blobStoreService.listPointsOfInterestInBlob(providerName,maxNbResults));
        }
        Collections.sort(poiFileList, Collections.reverseOrder());
        return poiFileList;
    }

    public List<String> getParkingsFileListByProviderName(String providerName, int maxNbResults){
        List<String> parkingsFileList = new ArrayList<>();

        if (StringUtils.equals(tiamatExportDestination, "local") || StringUtils.equals(tiamatExportDestination, "both")){
            parkingsFileList.addAll(getParkingsFileListFromLocalStorage(providerName));
        }

        if (StringUtils.equals(tiamatExportDestination, "gcs") || StringUtils.equals(tiamatExportDestination, "both")){
            parkingsFileList.addAll(blobStoreService.listParkingsInBlob(providerName,maxNbResults));
        }
        Collections.sort(parkingsFileList, Collections.reverseOrder());

        if (maxNbResults > 0 && maxNbResults < parkingsFileList.size()){
            parkingsFileList = parkingsFileList.subList(0,maxNbResults);
        }

        return parkingsFileList;
    }

    private List<String> getStopPlaceFileListFromLocalStorage(String providerName){
        File providerDir = new File(localExportPath + File.separator +providerName);

        if (!providerDir.exists())
            return new ArrayList<>();

        try {
            return Files.walk(providerDir.toPath())
                        .map(path -> path.getFileName().toString())
                        .filter(filename->filename.contains(".zip"))
                        .filter(filename->filename.contains("ARRET_"))
                        .sorted()
                        .collect(toList());

        }
        catch (IOException e) {
            logger.error("Error while reading local FileStore repository");
            logger.error(e.getMessage());
        }
        return new ArrayList<>();
    }

    private List<String> getPointsOfInterestFileListFromLocalStorage(String providerName){
        File providerDir = new File(localExportPath + File.separator +providerName);

        if (!providerDir.exists())
            return new ArrayList<>();

        try {
            return Files.walk(providerDir.toPath())
                    .map(path -> path.getFileName().toString())
                    .filter(filename->filename.contains(".zip"))
                    .filter(filename->filename.contains("POI_"))
                    .collect(toList());

        }
        catch (IOException e) {
            logger.error("Error while reading local FileStore repository");
            logger.error(e.getMessage());
        }
        return new ArrayList<>();
    }

    private List<String> getParkingsFileListFromLocalStorage(String providerName){
        File providerDir = new File(localExportPath + File.separator +providerName);

        if (!providerDir.exists())
            return new ArrayList<>();

        try {
            return Files.walk(providerDir.toPath())
                    .map(path -> path.getFileName().toString())
                    .filter(filename->filename.contains(".zip"))
                    .filter(filename->filename.contains("PARKING_"))
                    .collect(toList());

        }
        catch (IOException e) {
            logger.error("Error while reading local FileStore repository");
            logger.error(e.getMessage());
        }
        return new ArrayList<>();
    }
}
