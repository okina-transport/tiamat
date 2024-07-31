package org.rutebanken.tiamat.rest.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.general.ImportJobWorker;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.NetexImporter;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class Importer {

    private static final Logger logger = LoggerFactory.getLogger(NetexImporter.class);

    public static final String ASYNC_IMPORT_PARKING_JOB_PATH = "import_parking";
    public static final String ASYNC_IMPORT_POI_JOB_PATH = "import_poi";

    private static final ExecutorService importService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
            .setNameFormat("import-%d").build());

    public static Job manageJob(Job job, JobStatus state, ImportParams importParams, Provider provider, String fileName, String folder, JobType type) {
        return manageJob(job, state, importParams, provider, fileName, folder, null, type);
    }

    public static Job manageJob(Job job, JobStatus state, ImportParams importParams, Provider provider, String fileName, String folder, Exception exception, JobType type) {
        job.setStatus(state);

        if (state.equals(JobStatus.FAILED)) {
            String message = "Error executing import job " + job.getId() + ". " + exception.getClass().getSimpleName() + " - " + exception.getMessage();
            logger.error("{}.\nImport job was {}", message, job, exception);
            job.setMessage(message);
        }

        else if (state.equals(JobStatus.PROCESSING)) {
            if (provider != null) {
                logger.info("Starting import {} for provider {}", job.getId(), provider.id + "/" + provider.chouetteInfo.codeIdfm);
            } else {
                logger.info("Starting import {}", job.getId());
            }
            job.setStarted(Instant.now());
            job.setImportParams(importParams);
            job.setSubFolder(folder);
            job.setMessage(null);
            String nameFileXml = null;

            try {
                nameFileXml = URLEncoder.encode(fileName, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            job.setFileName(nameFileXml);

            ImportJobWorker importJobWorker = new ImportJobWorker(job);
            importService.submit(importJobWorker);
            logger.info("Returning started import job {}", job);
            setJobUrl(job, type);

        }
        else if (state.equals(JobStatus.FINISHED)) {
            job.setFinished(Instant.now());
        }
        return job;
    }

    private static Job setJobUrl(Job jobWithId, JobType type) {
        if (type.equals(JobType.NETEX_PARKING)) {
            jobWithId.setJobUrl(ASYNC_IMPORT_PARKING_JOB_PATH + "/" + jobWithId.getId());
        } else if (type.equals(JobType.NETEX_POI)) {
            jobWithId.setJobUrl(ASYNC_IMPORT_POI_JOB_PATH + "/" + jobWithId.getId());
        }
        return jobWithId;
    }

    public static void validate(ImportParams importParams) {
        if (importParams.targetTopographicPlaces != null && importParams.onlyMatchOutsideTopographicPlaces != null) {
            if (!importParams.targetTopographicPlaces.isEmpty() && !importParams.onlyMatchOutsideTopographicPlaces.isEmpty()) {
                throw new IllegalArgumentException("targetTopographicPlaces and onlyMatchOutsideTopographicPlaces cannot be specified at the same time!");
            }
        }
    }
}
