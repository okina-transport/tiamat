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

package org.rutebanken.tiamat.importer;

import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.importer.handler.ParkingsImportHandler;
import org.rutebanken.tiamat.importer.handler.PointOfInterestsImportHandler;
import org.rutebanken.tiamat.importer.handler.StopPlacesImportHandler;
import org.rutebanken.tiamat.netex.NetexUtils;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.repository.CacheProviderRepository;
import org.rutebanken.tiamat.repository.JobRepository;
import org.rutebanken.tiamat.validator.PublicationDeliveryValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;

import jakarta.xml.bind.JAXBElement;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.rutebanken.tiamat.netex.mapping.NetexMappingContextThreadLocal.updateMappingContext;

@Service
public class NetexImporter {

    public static final String IMPORT_CORRELATION_ID = "importCorrelationId";
    private static final Logger logger = LoggerFactory.getLogger(NetexImporter.class);
    private final PublicationDeliveryHelper publicationDeliveryHelper;
    private final ParkingsImportHandler parkingsImportHandler;
    private final StopPlacesImportHandler stopPlacesImportHandler;
    private final PointOfInterestsImportHandler pointOfInterestsImportHandler;
    private final PublicationDeliveryValidator publicationDeliveryValidator;
    @Autowired
    protected CacheProviderRepository providerRepository;
    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private NetexMapper netexMapper;

    @Autowired
    public NetexImporter(PublicationDeliveryHelper publicationDeliveryHelper,
                         ParkingsImportHandler parkingsImportHandler,
                         StopPlacesImportHandler stopPlacesImportHandler,
                         PointOfInterestsImportHandler pointOfInterestsImportHandler,
                         PublicationDeliveryValidator publicationDeliveryValidator) {
        this.publicationDeliveryHelper = publicationDeliveryHelper;
        this.parkingsImportHandler = parkingsImportHandler;
        this.stopPlacesImportHandler = stopPlacesImportHandler;
        this.pointOfInterestsImportHandler = pointOfInterestsImportHandler;
        this.publicationDeliveryValidator = publicationDeliveryValidator;
    }

    public void importProcessTest(PublicationDeliveryStructure publicationDeliveryStructure, Boolean containsMobiitiIds) throws BindException {
        importProcess(publicationDeliveryStructure, new ImportParams(), containsMobiitiIds);
    }


    @SuppressWarnings("unchecked")
    public void importProcess(PublicationDeliveryStructure publicationDeliveryStructure, ImportParams importParams, Boolean containsMobiitiIds) throws BindException {

        if (publicationDeliveryStructure.getDataObjects() == null) {
            String responseMessage = "Received publication delivery but it does not contain any data objects.";
            logger.warn(responseMessage);
            throw new RuntimeException(responseMessage);
        }

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(publicationDeliveryStructure, "publicationDelivery");
        publicationDeliveryValidator.validate(publicationDeliveryStructure, bindingResult);

        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }

        logger.info("Got publication delivery with {} site frames and description {}",
                publicationDeliveryStructure.getDataObjects().getCompositeFrameOrCommonFrame().size(),
                publicationDeliveryStructure.getDescription());


        AtomicInteger atomicInteger = new AtomicInteger(0);
        List<jakarta.xml.bind.JAXBElement<? extends org.rutebanken.netex.model.Common_VersionFrameStructure>> findedFrameType = publicationDeliveryStructure.getDataObjects().getCompositeFrameOrCommonFrame();

        try {
            List<GeneralOrganisation> generalOrganisations = new ArrayList<>();
            List<ResponsibilitySet> responsibilitySets = new ArrayList<>();
            List<JAXBElement<? extends EntityStructure>> members = null;


            if (hasGeneralFrame(publicationDeliveryStructure)){
                members = NetexUtils.getMembersFromPublicationDelivery(publicationDeliveryStructure);
                assert members != null;
                generalOrganisations = NetexUtils.getMembers(GeneralOrganisation.class, members);
                responsibilitySets = NetexUtils.getMembers(ResponsibilitySet.class, members);
                generalFrameProcess(members, importParams, atomicInteger, generalOrganisations, responsibilitySets, containsMobiitiIds);
            }

            if (hasFrameOfType(publicationDeliveryStructure, SiteFrame.class)){
                siteFrameProcess(publicationDeliveryStructure, importParams, atomicInteger, generalOrganisations, responsibilitySets);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            MDC.remove(IMPORT_CORRELATION_ID);
        }
    }

    private boolean hasFrameOfType(PublicationDeliveryStructure publicationDeliveryStructure, Class<?> frameToSearch){
        List<jakarta.xml.bind.JAXBElement<? extends org.rutebanken.netex.model.Common_VersionFrameStructure>> findedFrameType = publicationDeliveryStructure.getDataObjects().getCompositeFrameOrCommonFrame();
        for (JAXBElement<? extends Common_VersionFrameStructure> frameType : findedFrameType) {
            if (frameToSearch.isInstance(frameType.getValue())) {
                return true;
            }
            if(frameType.getValue() instanceof CompositeFrame){
                CompositeFrame compositeFrame = (CompositeFrame) frameType.getValue();
                for (JAXBElement<? extends Common_VersionFrameStructure> subFrame : compositeFrame.getFrames().getCommonFrame()) {
                    if (frameToSearch.isInstance(subFrame.getValue())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasGeneralFrame(PublicationDeliveryStructure publicationDeliveryStructure){
        List<jakarta.xml.bind.JAXBElement<? extends org.rutebanken.netex.model.Common_VersionFrameStructure>> findedFrameType = publicationDeliveryStructure.getDataObjects().getCompositeFrameOrCommonFrame();
        for (JAXBElement<? extends Common_VersionFrameStructure> frameType : findedFrameType) {
            if (frameType.getValue() instanceof GeneralFrame) {
                return true;
            }
            if(frameType.getValue() instanceof CompositeFrame){
                CompositeFrame compositeFrame = (CompositeFrame) frameType.getValue();
                for (JAXBElement<? extends Common_VersionFrameStructure> subFrame : compositeFrame.getFrames().getCommonFrame()) {
                    if (subFrame.getValue() instanceof GeneralFrame) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void siteFrameProcess(PublicationDeliveryStructure publicationDeliveryStructure, ImportParams importParams, AtomicInteger atomicInteger, List<GeneralOrganisation> generalOrganisations, List<ResponsibilitySet> responsibilitySets) {
        SiteFrame netexSiteFrame = publicationDeliveryHelper.findSiteFrame(publicationDeliveryStructure);
        String requestId = netexSiteFrame.getId();
        updateMappingContext(netexSiteFrame);

        SiteFrame responseSiteFrame = new SiteFrame();
        MDC.put(IMPORT_CORRELATION_ID, requestId);
        logger.info("Publication delivery contains site frame created at {}", netexSiteFrame.getCreated());
        responseSiteFrame.withId(requestId + "-response").withVersion("1");

        if (publicationDeliveryHelper.hasPointOfInterests(netexSiteFrame)) {
            pointOfInterestImport(importParams, atomicInteger, netexSiteFrame, responseSiteFrame, generalOrganisations, responsibilitySets);
        }
    }

    private void pointOfInterestImport(ImportParams importParams, AtomicInteger atomicInteger, SiteFrame netexSiteFrame, SiteFrame responseSiteFrame, List<GeneralOrganisation> generalOrganisations, List<ResponsibilitySet> responsibilitySets) {
        pointOfInterestsImportHandler.handlePointOfInterests(netexSiteFrame, importParams, atomicInteger, responseSiteFrame, generalOrganisations, responsibilitySets);
    }

    private void generalFrameProcess(List<JAXBElement<? extends EntityStructure>> members, ImportParams importParams, AtomicInteger atomicInteger, List<GeneralOrganisation> generalOrganisations, List<ResponsibilitySet> responsibilitySets, Boolean containsMobiitiIds) {
        if (!members.isEmpty()) {
            if (members.stream().anyMatch(mem -> mem.getValue() instanceof Parking)) {
                parkingsImport(importParams, atomicInteger, members, generalOrganisations, responsibilitySets);
            } else if (members.stream().anyMatch(mem -> mem.getValue() instanceof StopPlace || mem.getValue() instanceof Quay)) {
                stopPlaceAndQuayImport(importParams, atomicInteger, members, containsMobiitiIds);
            }
        }
    }

    private void stopPlaceAndQuayImport(ImportParams importParams, AtomicInteger atomicInteger, List<JAXBElement<? extends EntityStructure>> members, Boolean containsMobiitiIds) {
        // Récupération de tous les quay présents dans le netex
        List<Quay> tiamatQuays = members.stream()
                .filter(member -> member.getValue() instanceof Quay)
                .map(member -> (Quay) member.getValue())
                .collect(Collectors.toList());

        // Récupération de tous les stop places présents dans le netex
        List<StopPlace> tiamatStopPlaces = members.stream()
                .filter(member -> member.getValue() instanceof StopPlace)
                .map(member -> (StopPlace) member.getValue())
                .collect(Collectors.toList());

        List<org.rutebanken.tiamat.model.Quay> quaysParsed = mapQuaysToTiamatModel(tiamatQuays);
        stopPlacesImportHandler.handleStopPlacesGeneralFrame(tiamatStopPlaces, importParams, members, atomicInteger, quaysParsed, containsMobiitiIds);
    }

    private void parkingsImport(ImportParams importParams, AtomicInteger atomicInteger, List<JAXBElement<? extends EntityStructure>> members, List<GeneralOrganisation> generalOrganisations, List<ResponsibilitySet> responsibilitySets) {
        List<Parking> tiamatParking = NetexUtils.getMembers(Parking.class, members);
        var transportTypes = NetexUtils.getMembers(TransportType.class, members);
        var topms = NetexUtils.getMembers(TypeOfPaymentMethod.class, members);
        parkingsImportHandler.handleParkingsGeneralFrame(tiamatParking, importParams, members, atomicInteger, generalOrganisations, responsibilitySets, transportTypes, topms);
    }

    public Provider getCurrentProvider(String providerId) {
        providerRepository.populate();
        Collection<Provider> providers = providerRepository.getProviders();

        try {
            Long id = Long.valueOf(providerId);
            Optional<Provider> findProvider = providers.stream()
                    .filter(provider -> Objects.equals(provider.getId(), id))
                    .findFirst();

            return findProvider.orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<org.rutebanken.tiamat.model.Quay> mapQuaysToTiamatModel(List<org.rutebanken.netex.model.Quay> netexQuaysInFrame) {
        if (netexQuaysInFrame.isEmpty())
            return null;

        List<org.rutebanken.tiamat.model.Quay> quaysList = new ArrayList<>();
        netexQuaysInFrame.forEach(netexQuay -> {
            org.rutebanken.tiamat.model.Quay currentQuay = netexMapper.mapToTiamatModel(netexQuay);
            if (currentQuay.getNetexId() == null) currentQuay.setNetexId(netexQuay.getId());
            quaysList.add(currentQuay);

        });
        return quaysList;
    }
}
