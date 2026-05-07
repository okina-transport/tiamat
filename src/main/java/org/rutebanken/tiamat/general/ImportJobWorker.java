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

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.rutebanken.netex.model.Common_VersionFrameStructure;
import org.rutebanken.netex.model.DataManagedObjectStructure;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.GeneralFrame;
import org.rutebanken.netex.model.KeyListStructure;
import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.QuayRefStructure;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.tiamat.config.Messages;
import org.rutebanken.tiamat.importer.GbfsParkingImporter;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.NetexImporter;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingLayoutEnumeration;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.gbfs.GbfsParkingImportParams;
import org.rutebanken.tiamat.model.job.AnalyzeImportError;
import org.rutebanken.tiamat.model.job.AnalyzeImportErrorType;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.netex.NetexUtils;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.rest.dto.DtoBikeParking;
import org.rutebanken.tiamat.rest.dto.DtoParking;
import org.rutebanken.tiamat.rest.dto.DtoPointOfInterest;
import org.rutebanken.tiamat.rest.exception.TiamatBusinessException;
import org.rutebanken.tiamat.rest.netex.publicationdelivery.PublicationDeliveryUnmarshaller;
import org.rutebanken.tiamat.service.batch.MissingInseeCodeService;
import org.rutebanken.tiamat.service.parking.ParkingsImportedService;
import org.rutebanken.tiamat.validator.NetexValidationErrorClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindException;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ImportJobWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ImportJobWorker.class);

    private final Job job;
    private final JobRepository jobRepository;
    private final PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;
    private final PointOfInterestCSVHelper poiHelper;
    private final NetexImporter netexImporter;
    private final ParkingsImportedService parkingsImportedService;
    private final MissingInseeCodeService missingInseeCodeService;
    private final GbfsParkingImporter gbfsParkingImporter;
    private final Messages messages;

    private InputStream inputStream;
    private boolean containsMobiitiIds;
    private Authentication authentication;
    private String parkingLayoutParam;
    private String parkingTypeParam;
    private boolean parkAndRideDetection;
    private String superIdPrefix;
    private boolean keepStopNames;
    private boolean keepStopGeolocalisation;
    private boolean updateStopAccessibility;
    private GbfsParkingImportParams gbfsParkingImportParams;

    protected ImportJobWorker(Job job, JobRepository jobRepository,
                              PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller, PointOfInterestCSVHelper poiHelper, NetexImporter netexImporter, ParkingsImportedService parkingsImportedService, MissingInseeCodeService missingInseeCodeService, GbfsParkingImporter gbfsParkingImporter, Messages messages) {
        this.job = job;
        this.jobRepository = jobRepository;
        this.publicationDeliveryUnmarshaller = publicationDeliveryUnmarshaller;
        this.poiHelper = poiHelper;
        this.netexImporter = netexImporter;
        this.parkingsImportedService = parkingsImportedService;
        this.missingInseeCodeService = missingInseeCodeService;
        this.gbfsParkingImporter = gbfsParkingImporter;
        this.messages = messages;
    }

    public void run() {

        logger.info("Started import job: {}", job);
        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }


        try {
            switch (job.getType()) {
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
                case MISSING_INSEE_CODE:
                    launchMissingInseeCodeService();
                    break;
                case GBFS_PARKING:
                    launchGbfsParkingImport();
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
            String exceptionMessage;
            if (e instanceof BindException bindException) {
                exceptionMessage = bindException.getAllErrors().stream()
                        .map(oe -> messages.get(oe.getCode(), oe.getArguments()))
                        .collect(Collectors.joining(System.lineSeparator()));
                if (job.getType() == JobType.NETEX_PARKING || job.getType() == JobType.NETEX_POI) {
                    job.setErrors(NetexValidationErrorClassifier.classify(bindException));
                }
            } else {
                exceptionMessage = e.getMessage();
            }
            if (e instanceof AnalyzeImportException analyzeImportException) {
                Set<AnalyzeImportErrorType> errorTypes = analyzeImportException.getErrors().stream()
                        .map(AnalyzeImportError::getType)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                job.setErrors(errorTypes);
            }
            String message = "Error executing import job " + job.getId() + ". " + e.getClass().getSimpleName() + " - " + exceptionMessage;
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

    private void launchCSVRentalBikeParkingImport() throws IOException {
        List<DtoBikeParking> dtoParkingCSV = BikesCSVHelper.parseDocument(inputStream);
        BikesCSVHelper.checkDuplicatedBikeParkings(dtoParkingCSV);
        List<Parking> parkings = BikesCSVHelper.mapFromDtoToEntityParking(dtoParkingCSV, true);
        parkingsImportedService.createOrUpdateParkings(parkings);
    }

    private void launchCSVBikeParkingImport() throws IOException {
        List<DtoBikeParking> dtoBikeParkingsCSV = BikesCSVHelper.parseDocument(inputStream);
        BikesCSVHelper.checkDuplicatedBikeParkings(dtoBikeParkingsCSV);
        List<Parking> bikeParkings = BikesCSVHelper.mapFromDtoToEntityParking(dtoBikeParkingsCSV, false);
        parkingsImportedService.createOrUpdateParkings(bikeParkings);
    }

    private void launchCSVParkingImport() throws IOException {
        ParkingLayoutEnumeration parkingLayoutEnumeration = ParkingLayoutEnumeration.fromValue(parkingLayoutParam);
        ParkingTypeEnumeration parkingTypeEnumeration = ParkingTypeEnumeration.fromValue(parkingTypeParam);
        List<DtoParking> dtoParkingCSV = ParkingsCSVHelper.parseDocument(inputStream);
        ParkingsCSVHelper.checkDuplicatedParkings(dtoParkingCSV);
        List<Parking> parkings = ParkingsCSVHelper.mapFromDtoToEntity(dtoParkingCSV, parkingLayoutEnumeration, parkingTypeEnumeration, parkAndRideDetection);
        parkingsImportedService.createOrUpdateParkings(parkings);
    }

    private void launchGbfsParkingImport() throws TiamatBusinessException {
        gbfsParkingImporter.importProcess(gbfsParkingImportParams);
    }

    private void launchNetexParkingImport() throws IOException, BindException {
        PublicationDeliveryStructure incomingPublicationDelivery = unmarshalNetex();
        netexImporter.importProcess(incomingPublicationDelivery, new ImportParams(), false);
    }

    private PublicationDeliveryStructure unmarshalNetex() throws IOException {
        byte[] netexBytes = NetexXmlHelper.readAllBytes(inputStream);
        Utf8Helper.decodeStrictUtf8(netexBytes, "NeTEx");

        try {
            return publicationDeliveryUnmarshaller.unmarshal(new ByteArrayInputStream(netexBytes));
        } catch (JAXBException | SAXException e) {
            throw new AnalyzeImportException(AnalyzeImportErrorType.TEMPLATE, "Le fichier NeTEx est invalide ou mal formé : " + e.getMessage());
        }
    }

    private void launchNetexStopPlaceImport() throws JAXBException, IOException, SAXException, BindException {
        PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(inputStream);
        containsMobiitiIds = isUsingSuperIds(incomingPublicationDelivery);
        if (!containsMobiitiIds) {
            replaceIdsAndRemoveImportedIds(incomingPublicationDelivery);
        }
        ImportParams importParams = new ImportParams();
        importParams.updateStopAccessibility = updateStopAccessibility;
        importParams.keepStopNames = keepStopNames;
        importParams.keepStopGeolocalisation = keepStopGeolocalisation;
        importParams.isNetex = true;

        netexImporter.importProcess(incomingPublicationDelivery, importParams, containsMobiitiIds);

    }

    private void replaceIdsAndRemoveImportedIds(PublicationDeliveryStructure incomingPublicationDelivery) {
        List<jakarta.xml.bind.JAXBElement<? extends org.rutebanken.netex.model.Common_VersionFrameStructure>> findedFrameType = incomingPublicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame();
        List<JAXBElement<? extends EntityStructure>> members = null;

        for (JAXBElement<? extends Common_VersionFrameStructure> frameType : findedFrameType) {
            if (frameType.getValue() instanceof GeneralFrame) {
                members = NetexUtils.getMembersFromPublicationDelivery(incomingPublicationDelivery);

                for (JAXBElement<? extends EntityStructure> member : members) {
                    if (member.getValue() instanceof StopPlace stopPlace) {
                        stopPlace.setId(replaceId("StopPlace", stopPlace.getId()));
                        removeImportedIds(stopPlace);

                        for (JAXBElement<?> jaxbElement : stopPlace.getQuays().getQuayRefOrQuay()) {
                            if (jaxbElement.getValue() instanceof QuayRefStructure quayRef) {
                                quayRef.setRef(replaceId("Quay", quayRef.getRef()));
                            } else if (jaxbElement.getValue() instanceof Quay quay) {
                                quay.setId(replaceId("Quay", quay.getId()));
                                removeImportedIds(quay);
                                quay.setSiteRef(null);
                            }
                        }
                    } else if (member.getValue() instanceof Quay quay) {
                        quay.setId(replaceId("Quay", quay.getId()));
                        removeImportedIds(quay);
                        quay.setSiteRef(null);
                    }
                }
            }
        }
    }

    private void removeImportedIds(DataManagedObjectStructure object) {
        if (object.getKeyList() == null) {
            return;
        }

        KeyListStructure originalList = object.getKeyList();
        KeyListStructure newKeyList = new KeyListStructure();

        List<KeyValueStructure> originalKeys = originalList.getKeyValue();
        List<KeyValueStructure> newKeyValues = new ArrayList<>();

        for (KeyValueStructure originalKey : originalKeys) {
            if (!originalKey.getKey().equals("imported-id")) {
                newKeyValues.add(originalKey);
            }
        }
        newKeyList.withKeyValue(newKeyValues);
        object.setKeyList(newKeyList);
    }

    private String replaceId(String type, String rawId) {
        String provider = job.getSubFolder().toUpperCase();
        return provider + ":" + type + ":" + rawId.replaceAll(":", "##3A##");
    }

    /**
     * Detects if the stops are using super id of the current stack or not     *
     *
     * @param incomingPublicationDelivery
     * @return true : the publication
     */
    private boolean isUsingSuperIds(PublicationDeliveryStructure incomingPublicationDelivery) {

        List<jakarta.xml.bind.JAXBElement<? extends org.rutebanken.netex.model.Common_VersionFrameStructure>> findedFrameType = incomingPublicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame();
        List<JAXBElement<? extends EntityStructure>> members = null;

        for (JAXBElement<? extends Common_VersionFrameStructure> frameType : findedFrameType) {
            if (frameType.getValue() instanceof GeneralFrame) {
                members = NetexUtils.getMembersFromPublicationDelivery(incomingPublicationDelivery);

                for (JAXBElement<? extends EntityStructure> member : members) {
                    if (member.getValue() instanceof StopPlace stopPlace) {
                        if (stopPlace.getId().startsWith(superIdPrefix + ":StopPlace:")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void launchNetexPoiImport() throws IOException, BindException {
        PublicationDeliveryStructure incomingPublicationDelivery = unmarshalNetex();

        poiHelper.clearClassificationCache();
        netexImporter.importProcess(incomingPublicationDelivery, new ImportParams(), false);
    }

    private void launchCSVPoiImport() throws IOException {
        poiHelper.clearClassificationCache();
        List<DtoPointOfInterest> dtoPointOfInterest = poiHelper.parseDocument(inputStream);
        PointOfInterestCSVHelper.checkDuplicatedPois(dtoPointOfInterest);
        List<DtoPointOfInterest> poiWithClassification = poiHelper.filterPoisWithClassificationOrShop(dtoPointOfInterest, job);
        poiHelper.persistPointsOfInterest(poiWithClassification);
    }

    private void launchMissingInseeCodeService() {
        missingInseeCodeService.getMissingInseeCode();
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setContainsMobiitiIds(boolean containsMobiitiIds) {
        this.containsMobiitiIds = containsMobiitiIds;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public void setParkingLayoutParam(String parkingLayoutParam) {
        this.parkingLayoutParam = parkingLayoutParam;
    }

    public void setParkingTypeParam(String parkingTypeParam) {
        this.parkingTypeParam = parkingTypeParam;
    }

    public void setParkAndRideDetection(boolean parkAndRideDetection) {
        this.parkAndRideDetection = parkAndRideDetection;
    }

    public void setSuperIdPrefix(String superIdPrefix) {
        this.superIdPrefix = superIdPrefix;
    }

    public void setKeepStopNames(boolean keepStopNames) {
        this.keepStopNames = keepStopNames;
    }

    public void setKeepStopGeolocalisation(boolean keepStopGeolocalisation) {
        this.keepStopGeolocalisation = keepStopGeolocalisation;
    }

    public void setUpdateStopAccessibility(boolean updateStopAccessibility) {
        this.updateStopAccessibility = updateStopAccessibility;
    }

    public void setGbfsParkingImportParams(GbfsParkingImportParams gbfsParkingImportParams) {
        this.gbfsParkingImportParams = gbfsParkingImportParams;
    }
}
