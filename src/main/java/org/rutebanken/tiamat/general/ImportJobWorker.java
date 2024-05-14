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

import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class ImportJobWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ImportJobWorker.class);

    private final Job job;

    public ImportJobWorker(Job job) {
        this.job = job;
    }


    public void run() {
        logger.info("Started import job: {}", job);
        try {
            job.setStatus(JobStatus.FINISHED);
            job.setFinished(Instant.now());
            logger.info("Import job done: {}", job);
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            String message = "Error executing import job " + job.getId() + ". " + e.getClass().getSimpleName() + " - " + e.getMessage();
            logger.error("{}.\nImport job was {}", message, job, e);
            job.setMessage(message);
            if (e instanceof InterruptedException) {
                logger.info("The import job was interrupted: {}", job);
                Thread.currentThread().interrupt();
            }
        }
    }
}
