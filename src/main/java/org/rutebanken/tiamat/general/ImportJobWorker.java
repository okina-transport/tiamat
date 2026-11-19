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
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.config.Messages;
import org.rutebanken.tiamat.importer.GbfsParkingImporter;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.NetexImporter;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingLayoutEnumeration;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.gbfs.GbfsParkingImportParams;
import org.rutebanken.tiamat.model.job.*;
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
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    private NetexLineLocator netexLineLocator;

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
        ImportWarnings.start();

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
                    job.setErrors(NetexValidationErrorClassifier.toJobErrors(bindException, netexLineLocator));
                }
            } else {
                exceptionMessage = e.getMessage();
            }
            if (e instanceof AnalyzeImportException analyzeImportException) {
                job.setErrors(analyzeImportException.getErrors().stream()
                        .map(ImportJobWorker::toJobError)
                        .distinct()
                        .collect(Collectors.toList()));
            }
            if (e instanceof InvalidXmlException invalidXmlException) {
                job.setErrors(List.of(invalidXmlException.toJobError()));
            }
            if (e instanceof FieldTooLongException fieldTooLongException) {
                job.setErrors(fieldTooLongException.toJobErrors(netexLineLocator));
            }
            if (e instanceof InvalidTemplateException invalidTemplateException) {
                job.setErrors(List.of(invalidTemplateException.toJobError(job.getFileName())));
            }
            if (e instanceof DuplicatedIdentifierException duplicatedIdentifierException) {
                job.setErrors(duplicatedIdentifierException.toJobErrors());
            }
            String message = "Error executing import job " + job.getId() + ". " + e.getClass().getSimpleName() + " - " + exceptionMessage;
            logger.error("{}.\nImport job was {}", message, job, e);
            job.setMessage(message);
            if (e instanceof InterruptedException) {
                logger.info("The import job was interrupted: {}", job);
                Thread.currentThread().interrupt();
            }
        } finally {
            addWarnings(ImportWarnings.stop());
            job.setErrors(ImportErrorLimit.cap(job.getErrors()));
            jobRepository.save(job);
            closeInputStream();
        }
    }

    /**
     * Warnings (ex: truncated values) are reported after the errors, whatever the status of the job
     */
    private void addWarnings(List<JobError> warnings) {
        if (warnings.isEmpty()) {
            return;
        }
        List<JobError> errorsAndWarnings = new ArrayList<>();
        if (job.getErrors() != null) {
            errorsAndWarnings.addAll(job.getErrors());
        }
        errorsAndWarnings.addAll(warnings);
        job.setErrors(errorsAndWarnings);
    }

    private void closeInputStream() {
        if (inputStream == null) {
            return;
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            logger.warn("Could not close input stream of import job {}", job.getId(), e);
        }
    }

    private void launchCSVRentalBikeParkingImport() throws IOException {
        List<DtoBikeParking> dtoParkingCSV = BikesCSVHelper.parseDocument(inputStream);
        BikesCSVHelper.checkDuplicatedBikeParkings(dtoParkingCSV, job.getFileName());
        List<Parking> parkings = BikesCSVHelper.mapFromDtoToEntityParking(dtoParkingCSV, true);
        parkingsImportedService.createOrUpdateParkings(parkings);
    }

    private void launchCSVBikeParkingImport() throws IOException {
        List<DtoBikeParking> dtoBikeParkingsCSV = BikesCSVHelper.parseDocument(inputStream);
        BikesCSVHelper.checkDuplicatedBikeParkings(dtoBikeParkingsCSV, job.getFileName());
        List<Parking> bikeParkings = BikesCSVHelper.mapFromDtoToEntityParking(dtoBikeParkingsCSV, false);
        parkingsImportedService.createOrUpdateParkings(bikeParkings);
    }

    /**
     * Invalid number, coordinate, date and enum errors, as well as missing data detected on a CSV row,
     * are reported one by one with their details. The other errors are only reported once, without params.
     */
    private static JobError toJobError(AnalyzeImportError error) {
        if (error.getType() == AnalyzeImportErrorType.INVALID_FORMAT_ENUM) {
            return JobError.error(error.getType())
                    .withParam("field", error.getField())
                    .withParam("allowedValues", String.join(", ", error.getAllowedValues()))
                    .withParam("line", String.valueOf(error.getLine()))
                    .withParam("errorValue", error.getErrorValue());
        }
        if (error.getType() == AnalyzeImportErrorType.INVALID_FORMAT_NUMBER
                || error.getType() == AnalyzeImportErrorType.INVALID_FORMAT_COORDINATE
                || error.getType() == AnalyzeImportErrorType.INVALID_FORMAT_DATE) {
            return JobError.error(error.getType())
                    .withParam("field", error.getField())
                    .withParam("line", String.valueOf(error.getLine()))
                    .withParam("errorValue", error.getErrorValue());
        }
        if (error.getType() == AnalyzeImportErrorType.MISSING_DATA && error.getField() != null && error.getLine() != null) {
            return JobError.error(error.getType())
                    .withParam("field", error.getField())
                    .withParam("line", String.valueOf(error.getLine()));
        }
        return JobError.error(error.getType());
    }

    private void launchCSVParkingImport() throws IOException {
        ParkingLayoutEnumeration parkingLayoutEnumeration = ParkingLayoutEnumeration.fromValue(parkingLayoutParam);
        ParkingTypeEnumeration parkingTypeEnumeration = ParkingTypeEnumeration.fromValue(parkingTypeParam);
        List<DtoParking> dtoParkingCSV = ParkingsCSVHelper.parseDocument(inputStream);
        ParkingsCSVHelper.checkDuplicatedParkings(dtoParkingCSV, job.getFileName());
        List<Parking> parkings = ParkingsCSVHelper.mapFromDtoToEntity(dtoParkingCSV, parkingLayoutEnumeration, parkingTypeEnumeration, parkAndRideDetection);
        parkingsImportedService.createOrUpdateParkings(parkings);
    }

    private void launchGbfsParkingImport() throws TiamatBusinessException {
        gbfsParkingImporter.importProcess(gbfsParkingImportParams);
    }

    private void launchNetexParkingImport() throws IOException, BindException {
        PublicationDeliveryStructure incomingPublicationDelivery = unmarshalNetex();
        if (!containsParking(incomingPublicationDelivery)) {
            throw new AnalyzeImportException(AnalyzeImportErrorType.NO_DATA_ROWS, "The NeTEx file does not contain any parking");
        }
        netexImporter.importProcess(incomingPublicationDelivery, new ImportParams(), false);
    }

    /**
     * Two objects of the file must not share the same id: the whole file is rejected, before any import.
     */
    private void checkDuplicatedNetexIds() {
        List<DuplicatedIdentifierException.Duplicate> duplicates = netexLineLocator.findDuplicatedIds();
        if (!duplicates.isEmpty()) {
            String duplicatesMsg = duplicates.stream()
                    .map(duplicate -> duplicate.id() + " (line " + duplicate.line() + ")")
                    .collect(Collectors.joining(", "));
            throw new DuplicatedIdentifierException("There are objects sharing the same id in your NeTEx file. Duplicates: " + duplicatesMsg,
                    job.getFileName(), "id", duplicates);
        }
    }

    /**
     * Points of interest are only imported from the first site frame: at the top level of the file, otherwise in a
     * composite frame (see {@link org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper#findSiteFrame})
     */
    private static boolean containsPointOfInterest(PublicationDeliveryStructure publicationDelivery) {
        if (publicationDelivery.getDataObjects() == null) {
            return false;
        }
        List<Common_VersionFrameStructure> frames = publicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame().stream()
                .map(JAXBElement::getValue)
                .collect(Collectors.toList());
        SiteFrame siteFrame = frames.stream()
                .filter(SiteFrame.class::isInstance)
                .map(SiteFrame.class::cast)
                .findFirst()
                .orElseGet(() -> frames.stream()
                        .filter(CompositeFrame.class::isInstance)
                        .map(CompositeFrame.class::cast)
                        .filter(compositeFrame -> compositeFrame.getFrames() != null)
                        .flatMap(compositeFrame -> compositeFrame.getFrames().getCommonFrame().stream())
                        .map(JAXBElement::getValue)
                        .filter(SiteFrame.class::isInstance)
                        .map(SiteFrame.class::cast)
                        .findFirst()
                        .orElse(null));
        return siteFrame != null
                && siteFrame.getPointsOfInterest() != null
                && !siteFrame.getPointsOfInterest().getPointOfInterest().isEmpty();
    }

    /**
     * Parkings are only imported from the members of the top level general frames (see {@link NetexImporter}).
     */
    private static boolean containsParking(PublicationDeliveryStructure publicationDelivery) {
        if (publicationDelivery.getDataObjects() == null) {
            return false;
        }
        return publicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame().stream()
                .map(JAXBElement::getValue)
                .filter(GeneralFrame.class::isInstance)
                .map(GeneralFrame.class::cast)
                .filter(generalFrame -> generalFrame.getMembers() != null)
                .flatMap(generalFrame -> generalFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity().stream())
                .anyMatch(member -> member.getValue() instanceof org.rutebanken.netex.model.Parking);
    }

    /**
     * Checks the encoding and the duplicated ids of the file, then unmarshals it.
     * Duplicated ids are checked before unmarshalling: the NeTEx XSD already rejects an id used twice with the
     * same version, but only with a generic schema error.
     */
    private PublicationDeliveryStructure unmarshalNetex() throws IOException {
        byte[] netexBytes = NetexXmlHelper.readAllBytes(inputStream);
        netexLineLocator = new NetexLineLocator(Utf8Helper.decodeStrictUtf8(netexBytes, "NeTEx"));
        checkDuplicatedNetexIds();

        try {
            return publicationDeliveryUnmarshaller.unmarshal(new ByteArrayInputStream(netexBytes));
        } catch (JAXBException | SAXException e) {
            SAXParseException parseException = findSaxParseException(e);
            if (parseException != null) {
                throw InvalidXmlException.from(parseException);
            }
            throw new AnalyzeImportException(AnalyzeImportErrorType.TEMPLATE, "Le fichier NeTEx est invalide ou mal formé : " + e.getMessage());
        }
    }

    /**
     * JAXB wraps the parser error (well-formedness or XSD violation) in its linked exception.
     */
    private static SAXParseException findSaxParseException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SAXParseException saxParseException) {
                return saxParseException;
            }
            Throwable next = current instanceof JAXBException jaxbException && jaxbException.getLinkedException() != null
                    ? jaxbException.getLinkedException()
                    : current.getCause();
            current = next == current ? null : next;
        }
        return null;
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
        if (!containsPointOfInterest(incomingPublicationDelivery)) {
            throw new AnalyzeImportException(AnalyzeImportErrorType.NO_DATA_ROWS, "The NeTEx file does not contain any point of interest");
        }

        poiHelper.clearClassificationCache();
        netexImporter.importProcess(incomingPublicationDelivery, new ImportParams(), false);
    }

    private void launchCSVPoiImport() throws IOException {
        poiHelper.clearClassificationCache();
        List<DtoPointOfInterest> dtoPointOfInterest = poiHelper.parseDocument(inputStream);
        PointOfInterestCSVHelper.checkDuplicatedPois(dtoPointOfInterest, job.getFileName());
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
