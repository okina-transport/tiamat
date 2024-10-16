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

import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.tiamat.importer.NetexImporter;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingLayoutEnumeration;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.rest.dto.DtoBikeParking;
import org.rutebanken.tiamat.rest.dto.DtoParking;
import org.rutebanken.tiamat.rest.dto.DtoPointOfInterest;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryUnmarshaller;
import org.rutebanken.tiamat.rest.utils.StreamUtils;
import org.rutebanken.tiamat.service.batch.MissingPostCodeService;
import org.rutebanken.tiamat.service.parking.BikeParkingsImportedService;
import org.rutebanken.tiamat.service.parking.ParkingsImportedService;
import org.rutebanken.tiamat.service.parking.RentalBikeParkingsImportedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

public class ImportJobWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ImportJobWorker.class);

    private final Job job;
    private static JobRepository jobRepository;

    private PointOfInterestCSVHelper poiHelper;
    private InputStream inputStream;
    private PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;
    private NetexImporter netexImporter;
    private String provider;
    private boolean containsMobiitiIds;
    private Authentication authentication;
    private String parkingLayoutParam;
    private String parkingTypeParam;
    private boolean parkAndRideDetection;
    private ParkingsImportedService parkingsImportedService;
    private BikeParkingsImportedService bikeParkingsImportedService;
    private RentalBikeParkingsImportedService rentalBikeparkingsImportedService;
    private MissingPostCodeService missingPostalCodeService;

    public ImportJobWorker(Job job) {
        this.job = job;
    }

    public ImportJobWorker(Job job, JobRepository jobRepository) throws IOException {
        this.job = job;
        this.jobRepository = jobRepository;
    }

    public ImportJobWorker(Job job, InputStream inputStream, JobRepository jobRepository) throws IOException {
        this.job = job;
        this.inputStream = StreamUtils.copyToInputStream(inputStream);
        this.jobRepository = jobRepository;
    }

    public ImportJobWorker(Job job,PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller, NetexImporter netexImporter, InputStream inputStream, JobRepository jobRepository) throws IOException {
        this.job = job;
        this.publicationDeliveryUnmarshaller = publicationDeliveryUnmarshaller;
        this.netexImporter = netexImporter;
        this.inputStream = StreamUtils.copyToInputStream(inputStream);
        this.jobRepository = jobRepository;
    }

    public ImportJobWorker(Job job, PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller, InputStream inputStream,  boolean containsMobiitiIds, JobRepository jobRepository, NetexImporter netexImporter, String provider, Authentication authentication) throws IOException {
        this.job = job;
        this.publicationDeliveryUnmarshaller = publicationDeliveryUnmarshaller;
        this.inputStream = StreamUtils.copyToInputStream(inputStream);
        this.containsMobiitiIds = containsMobiitiIds;
        this.jobRepository = jobRepository;
        this.netexImporter = netexImporter;
        this.provider = provider;
        this.authentication = authentication;
    }



    public ImportJobWorker(Job job, PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller, NetexImporter netexImporter, String provider,JobRepository jobRepository, InputStream inputStream, PointOfInterestCSVHelper poiHelper) throws IOException {
        this.job = job;
        this.publicationDeliveryUnmarshaller = publicationDeliveryUnmarshaller;
        this.netexImporter = netexImporter;
        this.provider = provider;
        this.jobRepository = jobRepository;
        this.inputStream = StreamUtils.copyToInputStream(inputStream);
        this.poiHelper = poiHelper;
    }

    public ImportJobWorker(Job job, PointOfInterestCSVHelper poiHelper, InputStream inputStream, JobRepository jobRepository) throws IOException {
        this.job = job;
        this.poiHelper = poiHelper;
        this.inputStream = StreamUtils.copyToInputStream(inputStream);
        this.jobRepository = jobRepository;
    }


    public void run() {

        logger.info("Started import job: {}", job);
        if (authentication != null){
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }


        try {
            switch(job.getType()){
                case CSV_SHOP:
                    launchCSVShopImport();
                    break;
                case CSV_RENTAL_BIKE_PARKING:
                    launchCSVRentalBikeParkingImport();
                    break;
                case CSV_BIKE_PARKING:
                    launchCSVBikeParkingImport();
                    break;
                case CSV_PARKING:
                    launchCSVParkingImport();
                    break;
                case CSV_POI:
                    launchCSVPoiImport();
                    break;
                case NETEX_POI:
                    launchNetexPoiImport();
                    break;
                case NETEX_STOP_PLACE_QUAY:
                    launchNetexStopPlaceImport();
                    break;
                case NETEX_PARKING:
                    launchNetexParkingImport();
                    break;
                case MISSING_POSTAL_CODE:
                    launchMissingPostalCodeService();
                    break;
                default:
                    logger.warn("No process associated to this job type: {}", job.getType());
            }

            job.setStatus(JobStatus.FINISHED);
            job.setFinished(Instant.now());
            logger.info("Import job done: {}", job);
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setFinished(Instant.now());
            String message = "Error executing import job " + job.getId() + ". " + e.getClass().getSimpleName() + " - " + e.getMessage();
            logger.error("{}.\nImport job was {}", message, job, e);
            job.setMessage(message);
            if (e instanceof InterruptedException) {
                logger.info("The import job was interrupted: {}", job);
                Thread.currentThread().interrupt();
            }
        } finally {
            jobRepository.save(job);
        }
    }

    private void launchCSVShopImport() throws IOException {
        List<DtoPointOfInterest> dtoPointOfInterest = poiHelper.parseDocument(inputStream);
        PointOfInterestCSVHelper.checkDuplicatedPois(dtoPointOfInterest);
        poiHelper.checkShops(dtoPointOfInterest);
        poiHelper.persistPointsOfInterest(dtoPointOfInterest);
    }

    private void launchCSVRentalBikeParkingImport() throws IOException {
        List<DtoBikeParking> dtoParkingCSV = BikesCSVHelper.parseDocument(inputStream);
        BikesCSVHelper.checkDuplicatedBikeParkings(dtoParkingCSV);
        List<Parking> parkings = BikesCSVHelper.mapFromDtoToEntityParking(dtoParkingCSV, true);
        rentalBikeparkingsImportedService.createOrUpdateParkings(parkings);
    }

    private void launchCSVBikeParkingImport() throws IOException {
        List<DtoBikeParking> dtoBikeParkingsCSV = BikesCSVHelper.parseDocument(inputStream);
        BikesCSVHelper.checkDuplicatedBikeParkings(dtoBikeParkingsCSV);
        List<Parking> bikeParkings = BikesCSVHelper.mapFromDtoToEntityParking(dtoBikeParkingsCSV, false);
        bikeParkingsImportedService.createBikeParkings(bikeParkings);
    }

    private void launchCSVParkingImport() throws IOException {
        ParkingLayoutEnumeration parkingLayoutEnumeration = ParkingLayoutEnumeration.fromValue(parkingLayoutParam);
        ParkingTypeEnumeration parkingTypeEnumeration = ParkingTypeEnumeration.fromValue(parkingTypeParam);
        List<DtoParking> dtoParkingCSV = ParkingsCSVHelper.parseDocument(inputStream);
        ParkingsCSVHelper.checkDuplicatedParkings(dtoParkingCSV);
        List<Parking> parkings = ParkingsCSVHelper.mapFromDtoToEntity(dtoParkingCSV, parkingLayoutEnumeration, parkingTypeEnumeration, parkAndRideDetection);
        parkingsImportedService.createOrUpdateParkings(parkings);
    }

    private void launchNetexParkingImport() throws JAXBException, IOException, SAXException{
        PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(inputStream);
        netexImporter.importProcess(incomingPublicationDelivery, false);

    }

    private void launchNetexStopPlaceImport() throws JAXBException, IOException, SAXException{
        PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(inputStream);
        netexImporter.importProcess(incomingPublicationDelivery,  containsMobiitiIds);

    }

    private void launchNetexPoiImport() throws JAXBException, IOException, SAXException {
        PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(inputStream);
        poiHelper.clearClassificationCache();
        netexImporter.importProcess(incomingPublicationDelivery,   false);
    }

    private void launchCSVPoiImport() throws IOException {
        poiHelper.clearClassificationCache();
        List<DtoPointOfInterest> dtoPointOfInterest = poiHelper.parseDocument(inputStream);
        PointOfInterestCSVHelper.checkDuplicatedPois(dtoPointOfInterest);
        List<DtoPointOfInterest> poiWithClassification = poiHelper.filterPoisWithClassification(dtoPointOfInterest);
        poiHelper.persistPointsOfInterest(poiWithClassification);

    }

    private void launchMissingPostalCodeService() {
        missingPostalCodeService.getMissingPostCode();
    }

    public void setParkAndRideDetection(boolean parkAndRideDetection) {
        this.parkAndRideDetection = parkAndRideDetection;
    }



    public void setParkingTypeParam(String parkingTypeParam) {
        this.parkingTypeParam = parkingTypeParam;
    }



    public void setParkingLayoutParam(String parkingLayoutParam) {
        this.parkingLayoutParam = parkingLayoutParam;
    }



    public void setParkingsImportedService(ParkingsImportedService parkingsImportedService) {
        this.parkingsImportedService = parkingsImportedService;
    }



    public void setBikeParkingsImportedService(BikeParkingsImportedService bikeParkingsImportedService) {
        this.bikeParkingsImportedService = bikeParkingsImportedService;
    }

    public void setRentalBikeparkingsImportedService(RentalBikeParkingsImportedService rentalBikeparkingsImportedService) {
        this.rentalBikeparkingsImportedService = rentalBikeparkingsImportedService;
    }

    public void setPoiHelper(PointOfInterestCSVHelper poiHelper) {
        this.poiHelper = poiHelper;
    }

    public void setMissingPostalCodeService(MissingPostCodeService missingPostalCodeService) {
        this.missingPostalCodeService = missingPostalCodeService;
    }
}
