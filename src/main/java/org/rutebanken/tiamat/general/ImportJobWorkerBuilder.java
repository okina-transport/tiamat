package org.rutebanken.tiamat.general;

import org.jetbrains.annotations.NotNull;
import org.rutebanken.tiamat.config.Messages;
import org.rutebanken.tiamat.importer.GbfsParkingImporter;
import org.rutebanken.tiamat.importer.NetexImporter;
import org.rutebanken.tiamat.model.gbfs.GbfsParkingImportParams;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryUnmarshaller;
import org.rutebanken.tiamat.rest.utils.StreamUtils;
import org.rutebanken.tiamat.service.batch.MissingInseeCodeService;
import org.rutebanken.tiamat.service.parking.ParkingsImportedService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequestScope
// request scope is required because each ImportJobWorkerBuilder carry an ImportJobWorker instance and calling init
// method concurrently in 2 threads would erase importJobWorker created by first init call
public class ImportJobWorkerBuilder {

    private final JobRepository jobRepository;
    private final PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;
    private final PointOfInterestCSVHelper poiHelper;
    private final NetexImporter netexImporter;
    private final ParkingsImportedService parkingsImportedService;
    private final MissingInseeCodeService missingInseeCodeService;
    private final GbfsParkingImporter gbfsParkingImporter;
    private final Messages messages;
    private ImportJobWorker importJobWorker;

    public ImportJobWorkerBuilder(JobRepository jobRepository, PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller, PointOfInterestCSVHelper poiHelper, NetexImporter netexImporter, ParkingsImportedService parkingsImportedService, MissingInseeCodeService missingInseeCodeService, GbfsParkingImporter gbfsParkingImporter, Messages messages) {
        this.jobRepository = jobRepository;
        this.publicationDeliveryUnmarshaller = publicationDeliveryUnmarshaller;
        this.poiHelper = poiHelper;
        this.netexImporter = netexImporter;
        this.parkingsImportedService = parkingsImportedService;
        this.missingInseeCodeService = missingInseeCodeService;
        this.gbfsParkingImporter = gbfsParkingImporter;
        this.messages = messages;
    }

    // must be called first
    public ImportJobWorkerBuilder init(@NotNull Job job) {
        importJobWorker = new ImportJobWorker(job,
                this.jobRepository,
                this.publicationDeliveryUnmarshaller,
                this.poiHelper,
                this.netexImporter,
                this.parkingsImportedService,
                this.missingInseeCodeService,
                this.gbfsParkingImporter,
                this.messages);
        return this;
    }

    public ImportJobWorkerBuilder withInputStream(InputStream inputStream) throws IOException {
        this.importJobWorker.setInputStream(StreamUtils.copyToInputStream((inputStream)));
        return this;
    }

    public ImportJobWorkerBuilder withContainsMobiitiIds(boolean containsMobiitiIds) {
        this.importJobWorker.setContainsMobiitiIds(containsMobiitiIds);
        return this;
    }

    public ImportJobWorkerBuilder withAuthentication(Authentication authentication) {
        this.importJobWorker.setAuthentication(authentication);
        return this;
    }

    public ImportJobWorkerBuilder withParkingLayoutParam(String parkingLayoutParam) {
        this.importJobWorker.setParkingLayoutParam(parkingLayoutParam);
        return this;
    }

    public ImportJobWorkerBuilder withParkingTypeParam(String parkingTypeParam) {
        this.importJobWorker.setParkingTypeParam(parkingTypeParam);
        return this;
    }

    public ImportJobWorkerBuilder withParkAndRideDetection(boolean parkAndRideDetection) {
        this.importJobWorker.setParkAndRideDetection(parkAndRideDetection);
        return this;
    }

    public ImportJobWorkerBuilder withSuperIdPrefix(String superIdPrefix) {
        this.importJobWorker.setSuperIdPrefix(superIdPrefix);
        return this;
    }

    public ImportJobWorkerBuilder withKeepStopNames(Boolean keepStopNames) {
        if (keepStopNames != null) {
            this.importJobWorker.setKeepStopNames(keepStopNames);
        }
        return this;
    }

    public ImportJobWorkerBuilder withKeepStopGeolocalisation(Boolean keepStopGeolocalisation) {
        if (keepStopGeolocalisation != null) {
            this.importJobWorker.setKeepStopGeolocalisation(keepStopGeolocalisation);
        }
        return this;
    }

    public ImportJobWorkerBuilder withUpdateStopAccessibility(Boolean updateStopAccessibility) {
        if (updateStopAccessibility != null) {
            this.importJobWorker.setUpdateStopAccessibility(updateStopAccessibility);
        }
        return this;
    }

    public ImportJobWorkerBuilder withGbfsParkingImportParams(GbfsParkingImportParams gbfsParkingImportData) {
        this.importJobWorker.setGbfsParkingImportParams(gbfsParkingImportData);
        return this;
    }

    // must be called last
    public ImportJobWorker build() {
        return importJobWorker;
    }

}
