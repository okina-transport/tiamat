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

import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.netex.id.ValidPrefixList;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.repository.search.ChangedStopPlaceSearch;
import org.rutebanken.tiamat.service.stopplace.ChildStopPlacesFetcher;
import org.rutebanken.tiamat.service.stopplace.ParentStopPlacesFetcher;
import org.rutebanken.tiamat.time.ExportTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toSet;
import static org.rutebanken.tiamat.exporter.PublicationDeliveryExporter.MultiModalFetchMode.PARENTS;

/**
 * This class should be removed.
 * The reason is that we have two ways of exporting betex (sync and async) in Tiamat, but we want to maintain only one, for reduced complexity.
 * So, the regular synchronous export has been pointed at @{{@link StreamingPublicationDelivery}}, which also is used for async export.
 * The remeaining code to migrate is ChangedStopPlaceSearch and the ability to fetch children as @{{@link ChildStopPlacesFetcher}}.
 */
@Component
@Transactional
@Deprecated
public class PublicationDeliveryExporter {

    private static final Logger logger = LoggerFactory.getLogger(PublicationDeliveryExporter.class);
    private static final AtomicLong publicationDeliveryId = new AtomicLong();

    private final StopPlaceRepository stopPlaceRepository;
    private final NetexMapper netexMapper;
    private final TiamatSiteFrameExporter tiamatSiteFrameExporter;
    private final TopographicPlacesExporter topographicPlacesExporter;
    private final TariffZonesFromStopsExporter tariffZonesFromStopsExporter;
    private final ParentStopPlacesFetcher parentStopPlacesFetcher;
    private final ChildStopPlacesFetcher childStopPlacesFetcher;
    private final ValidPrefixList validPrefixList;
    private final ExportTimeZone exportTimeZone;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");


    public enum MultiModalFetchMode {CHILDREN, PARENTS}

    @Autowired
    public PublicationDeliveryExporter(StopPlaceRepository stopPlaceRepository,
                                       NetexMapper netexMapper,
                                       TiamatSiteFrameExporter tiamatSiteFrameExporter,
                                       TopographicPlacesExporter topographicPlacesExporter,
                                       TariffZonesFromStopsExporter tariffZonesFromStopsExporter,
                                       ParentStopPlacesFetcher parentStopPlacesFetcher,
                                       ChildStopPlacesFetcher childStopPlacesFetcher,
                                       ValidPrefixList validPrefixList, ExportTimeZone exportTimeZone) {
        this.stopPlaceRepository = stopPlaceRepository;
        this.netexMapper = netexMapper;
        this.tiamatSiteFrameExporter = tiamatSiteFrameExporter;
        this.topographicPlacesExporter = topographicPlacesExporter;
        this.tariffZonesFromStopsExporter = tariffZonesFromStopsExporter;
        this.parentStopPlacesFetcher = parentStopPlacesFetcher;
        this.childStopPlacesFetcher = childStopPlacesFetcher;
        this.validPrefixList = validPrefixList;
        this.exportTimeZone = exportTimeZone;
    }

    @Transactional(readOnly = true)
    public PublicationDeliveryStructurePage exportStopPlacesWithEffectiveChangeInPeriod(ChangedStopPlaceSearch search, ExportParams exportParams) {
        logger.info("Finding changed stop places with search params: {}", search);
        Page<StopPlace> stopPlacePage = stopPlaceRepository.findStopPlacesWithEffectiveChangeInPeriod(search);
        logger.debug("Found {} changed stop places", stopPlacePage.getSize());


        //Feed children objects with information from parent
        stopPlacePage.forEach(this::feedChildrenInfo);

        PublicationDeliveryStructure publicationDelivery = exportPublicationDeliveryWithStops(stopPlacePage.getContent(), exportParams, MultiModalFetchMode.CHILDREN);

        PublicationDeliveryStructurePage publicationDeliveryStructure = new PublicationDeliveryStructurePage(
                publicationDelivery,
                stopPlacePage.getSize(),
                stopPlacePage.getTotalElements(),
                stopPlacePage.hasNext());
        logger.debug("Returning publication delivery structure: {}", publicationDeliveryStructure);
        return publicationDeliveryStructure;
    }


    /**
     * Feed children objects with information from parent
     * @param stopPlace
     *  The parent stop place
     */
    private void feedChildrenInfo(StopPlace stopPlace){
        for (Quay quay : stopPlace.getQuays()) {
            feedQuayWithParentInfo(quay,stopPlace);
        }
    }

    /**
     * Feed a quay with information from parent stop place
     * @param childQuay
     *  Child for which information must be filled
     * @param parentStopPlace
     *  Parent that contain information
     */
    private void feedQuayWithParentInfo(Quay childQuay, StopPlace parentStopPlace){
        childQuay.setTransportMode(parentStopPlace.getTransportMode());
    }

    public PublicationDeliveryStructure createPublicationDelivery(String idSite) {
        return new PublicationDeliveryStructure()
                .withVersion("1.1:FR-NETEX-2.2")
                .withPublicationTimestamp(LocalDateTime.now().withNano(0))
                .withParticipantRef(idSite)
                .withParticipantRef("MOBIITI");
    }

    public PublicationDeliveryStructure createPublicationDelivery(String idSite, LocalDateTime localDateTime) {
        return new PublicationDeliveryStructure()
                .withVersion("1.1:FR-NETEX-2.2")
                .withPublicationTimestamp(localDateTime)
                .withParticipantRef(idSite);
    }

    @SuppressWarnings("unchecked")
    public PublicationDeliveryStructure createPublicationDelivery(org.rutebanken.netex.model.SiteFrame siteFrame) {
        PublicationDeliveryStructure publicationDeliveryStructure = createPublicationDelivery("");
        publicationDeliveryStructure.withDataObjects(
                new PublicationDeliveryStructure.DataObjects()
                        .withCompositeFrameOrCommonFrame(new ObjectFactory().createSiteFrame(siteFrame)));

        logger.info("Returning publication delivery {} with site frame", publicationDeliveryStructure);
        return publicationDeliveryStructure;
    }

    public PublicationDeliveryStructure createPublicationDelivery(org.rutebanken.netex.model.GeneralFrame generalFrame, String idSite, LocalDateTime localDateTime) {
        PublicationDeliveryStructure publicationDeliveryStructure = createPublicationDelivery(idSite, localDateTime);
        publicationDeliveryStructure.withDataObjects(
                new PublicationDeliveryStructure.DataObjects()
                        .withCompositeFrameOrCommonFrame(new ObjectFactory().createGeneralFrame(generalFrame)));

        logger.info("Returning publication delivery {} with general frame", publicationDeliveryStructure);
        return publicationDeliveryStructure;
    }

    /**
     *
     * @param stopPlaces
     * @param exportParams
     * @param multiModalFetchMode if parents or children should be fetched
     * @return
     */
    public PublicationDeliveryStructure exportPublicationDeliveryWithStops(List<StopPlace> stopPlaces, ExportParams exportParams, MultiModalFetchMode multiModalFetchMode) {
        logger.info("Preparing publication delivery export");

        if(multiModalFetchMode == null) {
            multiModalFetchMode = PARENTS;
        }

        if(multiModalFetchMode.equals(MultiModalFetchMode.CHILDREN)) {
            stopPlaces = childStopPlacesFetcher.resolveChildren(stopPlaces);
        } else if( multiModalFetchMode.equals(PARENTS)){
            stopPlaces = parentStopPlacesFetcher.resolveParents(stopPlaces, true);
        }

        stopPlaces.forEach(stopPlace -> {
            if(stopPlace.getValidBetween() != null && stopPlace.getValidBetween().getToDate() != null && stopPlace.getValidBetween().getFromDate() == null){
                logger.info("Stop place with to_date but not from_date : " + stopPlace.getNetexId() + " - version : " + stopPlace.getVersion());
            }
        });

        org.rutebanken.tiamat.model.SiteFrame siteFrame = tiamatSiteFrameExporter.createTiamatSiteFrame("Site frame with stops");

        tiamatSiteFrameExporter.addStopsToTiamatSiteFrame(siteFrame, stopPlaces);
        topographicPlacesExporter.addTopographicPlacesToTiamatSiteFrame(exportParams.getTopographicPlaceExportMode(), siteFrame);

        boolean relevantTariffZones = ExportParams.ExportMode.RELEVANT.equals(exportParams.getTariffZoneExportMode());

        if (!relevantTariffZones && ExportParams.ExportMode.ALL.equals(exportParams.getTariffZoneExportMode())) {
            tiamatSiteFrameExporter.addAllTariffZones(siteFrame);
        }

        Set<Long> stopPlaceIds = StreamSupport.stream(stopPlaces.spliterator(), false).map(stopPlace -> stopPlace.getId()).collect(toSet());
        tiamatSiteFrameExporter.addRelevantPathLinks(stopPlaceIds, siteFrame);

        logger.info("Mapping site frame to netex model");
        org.rutebanken.netex.model.SiteFrame convertedSiteFrame = netexMapper.mapToNetexModel(siteFrame);

        if (convertedSiteFrame.getStopPlaces() != null) {
            if (relevantTariffZones) {
                tariffZonesFromStopsExporter.resolveTariffZones(convertedSiteFrame.getStopPlaces().getStopPlace_().stream()
                        .map(sp -> (org.rutebanken.netex.model.StopPlace) sp.getValue())
                        .collect(Collectors.toList()), convertedSiteFrame);
            } else if (ExportParams.ExportMode.NONE.equals(exportParams.getTariffZoneExportMode())) {
                logger.info("TariffZone export mode is NONE. Removing references from {} converted stop places", convertedSiteFrame.getStopPlaces().getStopPlace_().size());
                convertedSiteFrame.getStopPlaces().getStopPlace_()
                        .forEach(convertedStop -> ((org.rutebanken.netex.model.StopPlace) convertedStop.getValue()).setTariffZones(null));
            }
        }

        if (ExportParams.ExportMode.NONE.equals(exportParams.getTopographicPlaceExportMode())) {
            removeVersionFromTopographicPlaceReferences(convertedSiteFrame);
        }

        return createPublicationDelivery(convertedSiteFrame);
    }

    private void removeVersionFromTopographicPlaceReferences(org.rutebanken.netex.model.SiteFrame convertedSiteFrame) {
        if (convertedSiteFrame.getStopPlaces() != null) {
            convertedSiteFrame.getStopPlaces().getStopPlace_()
                    .stream()
                    .filter(sp -> sp.getValue().getTopographicPlaceRef() != null)
                    .forEach(sp -> sp.getValue().getTopographicPlaceRef().setVersion(null));
        }
    }

}
