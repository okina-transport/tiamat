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

import org.apache.commons.lang3.StringUtils;
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.externalapis.DtoGeocode;
import org.rutebanken.tiamat.externalapis.gbfs.mapper.StationInformationMapper;
import org.rutebanken.tiamat.externalapis.gbfs.mapper.SystemInformationMapper;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.ImporterUtils;
import org.rutebanken.tiamat.importer.NetexImporter;
import org.rutebanken.tiamat.model.Parking;
import org.rutebanken.tiamat.model.ParkingLayoutEnumeration;
import org.rutebanken.tiamat.model.ParkingTypeEnumeration;
import org.rutebanken.tiamat.model.gbfs.GbfsParkingImportData;
import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.netex.NetexUtils;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.repository.OrganisationRepository;
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

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImportJobWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ImportJobWorker.class);

    private final Job job;
    private JobRepository jobRepository;

    private PointOfInterestCSVHelper poiHelper;
    private InputStream inputStream;
    private PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller;
    private NetexImporter netexImporter;
    private boolean containsMobiitiIds;
    private Authentication authentication;
    private String parkingLayoutParam;
    private String parkingTypeParam;
    private boolean parkAndRideDetection;
    private ParkingsImportedService parkingsImportedService;
    private BikeParkingsImportedService bikeParkingsImportedService;
    private RentalBikeParkingsImportedService rentalBikeparkingsImportedService;
    private MissingPostCodeService missingPostalCodeService;
    private String superIdPrefix;
    private boolean keepStopNames;
    private boolean keepStopGeolocalisation;
    private boolean updateStopAccessibility;
    private GbfsParkingImportData gbfsParkingImportData;
    private OrganisationRepository organisationRepository;

    public ImportJobWorker(Job job) {
        this.job = job;
    }

    public ImportJobWorker(Job job, JobRepository jobRepository) {
        this.job = job;
        this.jobRepository = jobRepository;
    }

    public ImportJobWorker(Job job, InputStream inputStream, JobRepository jobRepository) throws IOException {
        this.job = job;
        this.inputStream = StreamUtils.copyToInputStream(inputStream);
        this.jobRepository = jobRepository;
    }

    public ImportJobWorker(Job job, PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller, NetexImporter netexImporter, InputStream inputStream, JobRepository jobRepository) throws IOException {
        this.job = job;
        this.publicationDeliveryUnmarshaller = publicationDeliveryUnmarshaller;
        this.netexImporter = netexImporter;
        this.inputStream = StreamUtils.copyToInputStream(inputStream);
        this.jobRepository = jobRepository;
    }

    public ImportJobWorker(Job job, PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller, InputStream inputStream, boolean containsMobiitiIds, JobRepository jobRepository, NetexImporter netexImporter, Authentication authentication) throws IOException {
        this.job = job;
        this.publicationDeliveryUnmarshaller = publicationDeliveryUnmarshaller;
        this.inputStream = StreamUtils.copyToInputStream(inputStream);
        this.containsMobiitiIds = containsMobiitiIds;
        this.jobRepository = jobRepository;
        this.netexImporter = netexImporter;
        this.authentication = authentication;
    }


    public ImportJobWorker(Job job, PublicationDeliveryUnmarshaller publicationDeliveryUnmarshaller, NetexImporter netexImporter, JobRepository jobRepository, InputStream inputStream, PointOfInterestCSVHelper poiHelper) throws IOException {
        this.job = job;
        this.publicationDeliveryUnmarshaller = publicationDeliveryUnmarshaller;
        this.netexImporter = netexImporter;
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
        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }


        try {
            switch (job.getType()) {
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

    private void launchGbfsParkingImport() {
        SystemInformationMapper systemInformationMapper = new SystemInformationMapper();
        org.rutebanken.tiamat.model.Organisation organisation = systemInformationMapper.toOrganisation(gbfsParkingImportData.systemInformation());
        Optional<org.rutebanken.tiamat.model.Organisation> optionalOrganisation = organisationRepository.findByName(organisation.getName());
        optionalOrganisation.ifPresent(value -> organisation.setId(value.getId()));
        organisationRepository.save(organisation);
        StationInformationMapper stationInformationMapper = new StationInformationMapper();
        List<Parking> parkingList = stationInformationMapper.toParkingList(organisation, gbfsParkingImportData);
        updateInseeCode(parkingList);
        parkingsImportedService.createOrUpdateParkings(parkingList);
    }

    private static void updateInseeCode(List<Parking> parkingList) {
        for (Parking parking : parkingList) {
            DtoGeocode dtoGeocode = new DtoGeocode();
            try {
                dtoGeocode = ImporterUtils.getGeocodeDataByReverseGeocoding(parking.getCentroid().getCoordinate().x, parking.getCentroid().getCoordinate().y);
            } catch (Exception e) {
                logger.error("Erreur lors de la récupération du code postal du parking : {}", parking.getNetexId(), e);
            }
            if (StringUtils.isNotBlank(dtoGeocode.getCityCode())) {
                parking.setInsee(dtoGeocode.getCityCode());
            } else {
                logger.error("Code postal non trouvé pour le parking {} ", parking.getNetexId());
            }
        }
    }

    private void launchNetexParkingImport() throws JAXBException, IOException, SAXException {
        PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(inputStream);
        netexImporter.importProcess(incomingPublicationDelivery, new ImportParams(), false);

    }

    private void launchNetexStopPlaceImport() throws JAXBException, IOException, SAXException {
        PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(inputStream);
        containsMobiitiIds = isUsingSuperIds(incomingPublicationDelivery);
        if (!containsMobiitiIds) {
            replaceIdsAndRemoveImportedIds(incomingPublicationDelivery);
        }
        ImportParams importParams = new ImportParams();
        importParams.updateStopAccessibility = updateStopAccessibility;
        importParams.keepStopNames = keepStopNames;
        importParams.keepStopGeolocalisation = keepStopGeolocalisation;

        netexImporter.importProcess(incomingPublicationDelivery, importParams, containsMobiitiIds);

    }

    private void replaceIdsAndRemoveImportedIds(PublicationDeliveryStructure incomingPublicationDelivery) {
        List<javax.xml.bind.JAXBElement<? extends org.rutebanken.netex.model.Common_VersionFrameStructure>> findedFrameType = incomingPublicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame();
        List<JAXBElement<? extends EntityStructure>> members = null;

        for (JAXBElement<? extends Common_VersionFrameStructure> frameType : findedFrameType) {
            if (frameType.getValue() instanceof GeneralFrame) {
                members = NetexUtils.getMembersFromPublicationDelivery(incomingPublicationDelivery);

                for (JAXBElement<? extends EntityStructure> member : members) {
                    if (member.getValue() instanceof StopPlace) {
                        StopPlace stopPlace = (StopPlace) member.getValue();
                        stopPlace.setId(replaceId("StopPlace", stopPlace.getId()));
                        removeImportedIds(stopPlace);

                        for (JAXBElement<?> jaxbElement : stopPlace.getQuays().getQuayRefOrQuay()) {
                            if (jaxbElement.getValue() instanceof QuayRefStructure) {
                                QuayRefStructure quayRef = (QuayRefStructure) jaxbElement.getValue();
                                quayRef.setRef(replaceId("Quay", quayRef.getRef()));
                            } else if (jaxbElement.getValue() instanceof Quay) {
                                Quay quay = (Quay) jaxbElement.getValue();
                                quay.setId(replaceId("Quay", quay.getId()));
                                removeImportedIds(quay);
                                quay.setSiteRef(null);
                            }
                        }
                    } else if (member.getValue() instanceof Quay) {
                        Quay quay = (Quay) member.getValue();
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

        List<javax.xml.bind.JAXBElement<? extends org.rutebanken.netex.model.Common_VersionFrameStructure>> findedFrameType = incomingPublicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame();
        List<JAXBElement<? extends EntityStructure>> members = null;

        for (JAXBElement<? extends Common_VersionFrameStructure> frameType : findedFrameType) {
            if (frameType.getValue() instanceof GeneralFrame) {
                members = NetexUtils.getMembersFromPublicationDelivery(incomingPublicationDelivery);

                for (JAXBElement<? extends EntityStructure> member : members) {
                    if (member.getValue() instanceof StopPlace) {
                        StopPlace stopPlace = (StopPlace) member.getValue();
                        if (stopPlace.getId().startsWith(superIdPrefix + ":StopPlace:")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void launchNetexPoiImport() throws JAXBException, IOException, SAXException {
        PublicationDeliveryStructure incomingPublicationDelivery = publicationDeliveryUnmarshaller.unmarshal(inputStream);
        poiHelper.clearClassificationCache();
        netexImporter.importProcess(incomingPublicationDelivery, new ImportParams(), false);
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

    public String getSuperIdPrefix() {
        return superIdPrefix;
    }

    public void setSuperIdPrefix(String superIdPrefix) {
        this.superIdPrefix = superIdPrefix;
    }

    public boolean isKeepStopNames() {
        return keepStopNames;
    }

    public void setKeepStopNames(boolean keepStopNames) {
        this.keepStopNames = keepStopNames;
    }

    public boolean isKeepStopGeolocalisation() {
        return keepStopGeolocalisation;
    }

    public void setKeepStopGeolocalisation(boolean keepStopGeolocalisation) {
        this.keepStopGeolocalisation = keepStopGeolocalisation;
    }

    public boolean isUpdateStopAccessibility() {
        return updateStopAccessibility;
    }

    public void setUpdateStopAccessibility(boolean updateStopAccessibility) {
        this.updateStopAccessibility = updateStopAccessibility;
    }

    public GbfsParkingImportData getGbfsParkingImportData() {
        return gbfsParkingImportData;
    }

    public void setGbfsParkingImportData(GbfsParkingImportData gbfsParkingImportData) {
        this.gbfsParkingImportData = gbfsParkingImportData;
    }

    public void setOrganisationRepository(OrganisationRepository organisationRepository) {
        this.organisationRepository = organisationRepository;
    }
}
