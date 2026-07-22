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

package org.rutebanken.tiamat.importer.merging;

import org.apache.commons.lang3.StringUtils;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.operation.TransformException;
import org.rutebanken.tiamat.client.mdm.OkinaIdentifier;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.ImporterUtils;
import org.rutebanken.tiamat.importer.KeyValueListAppender;
import org.rutebanken.tiamat.importer.mdm.MdmService;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;

@Component
public class QuayMerger {

    private static final Logger logger = LoggerFactory.getLogger(QuayMerger.class);

    /**
     * If two quays closed than this value, treat as match if no other conflict.
     */
    @Value("${quayMerger.mergeDistanceMetersIgnoreIdMatch:3}")
    public double MERGE_DISTANCE_METERS_IGNORE_ID_MATCH = 3;

    @Value("${quayMerger.mergeDistanceMeters:8}")
    public double MERGE_DISTANCE_METERS = 8;

    @Value("${quayMerger.mergeDistanceMetersExtended:30}")
    public double MERGE_DISTANCE_METERS_EXTENDED = 30;

    @Value("${quayMerger.maxCompassBearingDifference:60}")
    private int maxCompassBearingDifference = 60;

    private final AlternativeNameMerger alternativeNameMerger;

    private final AlternativeTextMerger alternativeTextMerger;

    private final MdmService mdmService;

    private final KeyValueListAppender keyValueListAppender;

    public QuayMerger(AlternativeNameMerger alternativeNameMerger, AlternativeTextMerger alternativeTextMerger, KeyValueListAppender keyValueListAppender,
                MdmService mdmService) {
        this.alternativeNameMerger = alternativeNameMerger;
        this.alternativeTextMerger = alternativeTextMerger;
        this.keyValueListAppender = keyValueListAppender;
        this.mdmService = mdmService;
    }

    public boolean mergeQuays(StopPlace newStopPlace, StopPlace existingStopPlace, boolean addNewQuays, ImportParams importParams) {
        return mergeQuays(newStopPlace, existingStopPlace, addNewQuays, false, importParams);
    }

    /**
     * Inspect quays from incoming AND matching stop place. If they do not exist from before, add them.
     */
    public boolean mergeQuays(StopPlace newStopPlace, StopPlace existingStopPlace, boolean addNewQuays, boolean lowDistanceCheckBeforeIdMatch, ImportParams importParams) {

        AtomicInteger updatedQuays = new AtomicInteger();
        AtomicInteger addedQuays = new AtomicInteger();

        logger.debug("About to compare quays for {}", existingStopPlace.getNetexId());

        if (newStopPlace.getQuays() == null) {
            newStopPlace.setQuays(new HashSet<>());
        }

        Set<Quay> result = mergeQuays(newStopPlace, newStopPlace.getQuays(), existingStopPlace.getQuays(), updatedQuays, addedQuays, addNewQuays, lowDistanceCheckBeforeIdMatch, importParams);

        existingStopPlace.setQuays(result);

        logger.debug("Created {} quays and updated {} quays for stop place {}", addedQuays.get(), updatedQuays.get(), existingStopPlace);
        return addedQuays.get() > 0 || updatedQuays.get() > 0;
    }

    public Set<Quay> mergeQuays(Set<Quay> newQuays, Set<Quay> existingQuays, AtomicInteger updatedQuaysCounter, AtomicInteger addedQuaysCounter, boolean addNewQuays) {
        ImportParams params = new ImportParams();
        params.keepStopGeolocalisation = false;
        params.keepStopNames = true;
        return mergeQuays(null, newQuays, existingQuays, updatedQuaysCounter, addedQuaysCounter, addNewQuays, true, params);
    }

    public Set<Quay> mergeQuays(StopPlace newStopPlace, Set<Quay> newQuays, Set<Quay> existingQuays, AtomicInteger updatedQuaysCounter, AtomicInteger addedQuaysCounter, boolean addNewQuays) {
        ImportParams params = new ImportParams();
        params.keepStopGeolocalisation = false;
        params.keepStopNames = true;
        return mergeQuays(newStopPlace, newQuays, existingQuays, updatedQuaysCounter, addedQuaysCounter, addNewQuays, true, params);
    }

    /**
     * Match new quays with existing quays, based on fields like geographic coordinates, compass bearing, name, original IDs and public code.
     *
     * @param newStopPlace        only used for logging
     * @param newQuays            incoming quays to match
     * @param existingQuays       existing quays to match against
     * @param updatedQuaysCounter how many quays were updated
     * @param addedQuaysCounter   how many quays were added
     * @param addNewQuays         if allowed to add quays if not match found
     * @return the resulting set of quays after matching, appending and adding.
     */
    public Set<Quay> mergeQuays(StopPlace newStopPlace, Set<Quay> newQuays, Set<Quay> existingQuays, AtomicInteger updatedQuaysCounter, AtomicInteger addedQuaysCounter, boolean addNewQuays, boolean lowDistanceCheckBeforeIdMatch, ImportParams importParams) {

        Set<Quay> result = new HashSet<>();
        if (existingQuays != null) {
            result.addAll(existingQuays);
        }

        for (Quay incomingQuay : newQuays) {

            Optional<Quay> matchingQuay;
            if (lowDistanceCheckBeforeIdMatch) {
                matchingQuay = findMatch(incomingQuay, result, MERGE_DISTANCE_METERS_IGNORE_ID_MATCH);
            } else {
                matchingQuay = Optional.empty();
            }

            if (matchingQuay.isEmpty() && incomingQuay != null && incomingQuay.getNetexId() != null) {
                matchingQuay = result.stream()
                        .filter(quay -> incomingQuay.getNetexId().equals(quay.getNetexId()))
                        .findFirst();
            }

            if (matchingQuay.isEmpty()) {
                matchingQuay = findMatchOnOriginalId(incomingQuay, result);
            }


            if (matchingQuay.isPresent()) {
                updateIfChanged(matchingQuay.get(), incomingQuay, updatedQuaysCounter, importParams);
            } else if (addNewQuays) {
                logger.info("Found no match for existing quay {}. Adding it!", incomingQuay);
                result.add(incomingQuay);
                incomingQuay.setCreated(Instant.now());
                incomingQuay.setChanged(Instant.now());
                addedQuaysCounter.incrementAndGet();
            } else {
                logger.warn("No match for quay belonging to stop place {}. Quay: {}. Full incoming quay toString: {}. Was looking in list of quays for match: {}",
                        newStopPlace != null ? newStopPlace.importedIdAndNameToString() : null,
                        incomingQuay != null ? incomingQuay.getOriginalIds() : null,
                        incomingQuay, result);
            }
        }

        return result;
    }

    private Optional<Quay> findMatch(Quay incomingQuay, Set<Quay> result, double mergeDistanceMeters) {
        for (Quay alreadyAdded : result) {
            if (matches(incomingQuay, alreadyAdded, mergeDistanceMeters)) {
                return Optional.of(alreadyAdded);
            }
        }
        return Optional.empty();
    }

    private Optional<Quay> findMatchOnOriginalId(Quay incomingQuay, Set<Quay> result) {
        for (Quay alreadyAdded : result) {
            if (mdmService.isMdmEnabled()) {
                Optional<Quay> alreadySavedQuay = checkQuayImportedIdWithMdm(incomingQuay, alreadyAdded);
                if (alreadySavedQuay.isPresent()) {
                    return alreadySavedQuay;
                }
            } else {
                Optional<Quay> alreadySavedQuay = checkQuayImportedId(incomingQuay, alreadyAdded);
                if (alreadySavedQuay.isPresent()) {
                    return alreadySavedQuay;
                }
            }

        }
        return Optional.empty();
    }

    private Optional<Quay> checkQuayImportedId(Quay incomingQuay, Quay alreadyAdded) {
        for (String originalIdAlreadyExisting : alreadyAdded.getOriginalIds()) {
            for (String newOriginalId : incomingQuay.getOriginalIds()) {
                if (newOriginalId.equals(originalIdAlreadyExisting)) {
                    return Optional.of(alreadyAdded);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Quay> checkQuayImportedIdWithMdm(Quay incomingQuay, Quay alreadyAdded) {
        List<OkinaIdentifier> allQuaysFromSuperId = mdmService.getAllQuaysFromSuperId(Collections.singleton(MdmService.getIdentifierFromNetexId(alreadyAdded.getNetexId())));
        for (String newOriginalId : incomingQuay.getOriginalIds()) {
            String value = MdmService.getOriginalIdFromTridentImportedId(newOriginalId);
            if (allQuaysFromSuperId.stream().anyMatch(okinaIdentifier -> value.equals(okinaIdentifier.getOriginalId()))) {
                return Optional.of(alreadyAdded);
            }
        }
        return Optional.empty();
    }

    private void updateIfChanged(Quay alreadyAdded, Quay incomingQuay, AtomicInteger updatedQuaysCounter, ImportParams importParams) {
        // The incoming quay could for some reason already have multiple imported IDs.

        boolean quayAlone;
        boolean idUpdated;
        boolean changedByMerge;
        boolean centroidUpdated;
        boolean stopCodeUpdated;
        boolean inseeCodeUpdated;
        boolean urlUpdated;
        boolean descUpdated;
        boolean nameUpdated = false;
        boolean keyValueExternalRefUpdated;
        boolean keyValueFareZoneUpdated;
        boolean accessibilityUpdated = false;

        quayAlone = checkNumberProducers(alreadyAdded.getKeyValues(), incomingQuay.getKeyValues());
        idUpdated = alreadyAdded.getOriginalIds().addAll(incomingQuay.getOriginalIds());
        changedByMerge = mergeFields(incomingQuay, alreadyAdded);
        stopCodeUpdated = updateCodes(alreadyAdded, incomingQuay, quayAlone);
        inseeCodeUpdated = updateInseeCode(alreadyAdded, incomingQuay);
        urlUpdated = updateUrl(alreadyAdded, incomingQuay);
        descUpdated = updateDesc(alreadyAdded, incomingQuay);
        centroidUpdated = false;

        if (!importParams.keepStopGeolocalisation && quayAlone) {
            centroidUpdated = updateCentroid(alreadyAdded, incomingQuay);
        }

        if (!importParams.keepStopNames) {
            nameUpdated = updatePropName(alreadyAdded, incomingQuay);
        }

        if (importParams.updateStopAccessibility) {
            accessibilityUpdated = updateAccessibility(alreadyAdded, incomingQuay);
        }

        keyValueExternalRefUpdated = keyValueListAppender.appendKeyValueExternalRef(NetexIdMapper.EXTERNAL_REF, incomingQuay, alreadyAdded);

        keyValueFareZoneUpdated = keyValueListAppender.appendKeyValue(NetexIdMapper.FARE_ZONE, incomingQuay, alreadyAdded);

        boolean updateAlternativeName = alternativeNameMerger.updateSiteElementAlternativeName(incomingQuay, alreadyAdded);

        boolean alternativeTextsUpdated = alternativeTextMerger.updateAlternativeTexts(alreadyAdded.getAlternativeTexts(), incomingQuay.getAlternativeTexts());

        if (idUpdated || changedByMerge || centroidUpdated || stopCodeUpdated || inseeCodeUpdated || urlUpdated || descUpdated || nameUpdated || keyValueExternalRefUpdated || accessibilityUpdated || keyValueFareZoneUpdated || updateAlternativeName || alternativeTextsUpdated) {
            logger.debug("Quay changed. idUpdated: {},  merged fields? {}, centroidUpdated: {}, stopCodesUpdated: {}, inseeCodeUpdated: {}, urlUpdated: {}, descUpdated:{}, nameUpdated:{}, keyValueExternalRefUpdated:{}, accessibilityUpdated:{}, keyValueFareZoneUpdated:{}. Quay: {}", idUpdated, changedByMerge, centroidUpdated, stopCodeUpdated, alreadyAdded, inseeCodeUpdated, urlUpdated, descUpdated, nameUpdated, keyValueExternalRefUpdated, accessibilityUpdated, keyValueFareZoneUpdated);

            alreadyAdded.setChanged(Instant.now());
            updatedQuaysCounter.incrementAndGet();
        }
    }

    private boolean updateDesc(Quay alreadyAdded, Quay incomingQuay) {
        String alreadyDescription = null;
        String incomingDescription = null;

        if (alreadyAdded.getDescription() != null && alreadyAdded.getDescription().getValue() != null) {
            alreadyDescription = alreadyAdded.getDescription().getValue();
        }

        if (incomingQuay.getDescription() != null && incomingQuay.getDescription().getValue() != null) {
            incomingDescription = incomingQuay.getDescription().getValue();
        }

        if (!StringUtils.equals(alreadyDescription, incomingDescription) && incomingDescription != null) {
            alreadyAdded.setDescription(new EmbeddableMultilingualString(incomingQuay.getDescription().getValue()));
            return true;
        }
        return false;
    }

    private boolean updateUrl(Quay alreadyAdded, Quay incomingQuay) {
        if (!StringUtils.equals(alreadyAdded.getUrl(), incomingQuay.getUrl()) && incomingQuay.getUrl() != null) {
            alreadyAdded.setUrl(incomingQuay.getUrl());
            return true;
        }
        return false;
    }

    private boolean updateInseeCode(Quay alreadyAdded, Quay incomingQuay) {
        if (!StringUtils.equals(alreadyAdded.getInseeCode(), incomingQuay.getInseeCode()) && incomingQuay.getInseeCode() != null) {
            alreadyAdded.setInseeCode(incomingQuay.getInseeCode());
            return true;
        }
        return false;
    }


    private boolean updateCodes(Quay alreadyAdded, Quay incomingQuay, boolean quayAlone) {
        boolean codesUpdated = false;
        if (!alreadyAdded.getOriginalStopCodes().contains(incomingQuay.getPublicCode()) && incomingQuay.getPublicCode() != null) {
            codesUpdated = alreadyAdded.getOriginalStopCodes().add(alreadyAdded.getPublicCode());
        }

        if (incomingQuay.getPublicCode() != null && !incomingQuay.getPublicCode().equals(alreadyAdded.getPublicCode())) {
            codesUpdated = alreadyAdded.getOriginalStopCodes().add(incomingQuay.getPublicCode());
            if (quayAlone) {
                alreadyAdded.setPublicCode(incomingQuay.getPublicCode());
            }
        }

        if (incomingQuay.getPrivateCode() != null && !incomingQuay.getPrivateCode().equals(alreadyAdded.getPrivateCode()) && quayAlone) {
            alreadyAdded.setPrivateCode(incomingQuay.getPrivateCode());
            codesUpdated = true;
        }

        return codesUpdated;
    }

    /**
     * Update wheelchair accessibility, if needed
     *
     * @param alreadyAdded existing quay
     * @param incomingQuay incoming quay from user's file
     * @return true : quay has been updated
     * false : no update has been done on the quay
     */
    private boolean updateAccessibility(Quay alreadyAdded, Quay incomingQuay) {
        boolean updated = false;

        Optional<LimitationStatusEnumeration> existingWheelchairLimitationOpt = ImporterUtils.getWheelchairLimitation(alreadyAdded);
        Optional<LimitationStatusEnumeration> incomingWheelchairLimitationOpt = ImporterUtils.getWheelchairLimitation(incomingQuay);


        if (incomingWheelchairLimitationOpt.isPresent() && !existingWheelchairLimitationOpt.equals(incomingWheelchairLimitationOpt)) {
            updated = true;
            ImporterUtils.updateWheelchairLimitation(alreadyAdded, incomingWheelchairLimitationOpt.get());
        }

        return updated;
    }

    private boolean updatePropName(Quay alreadyAdded, Quay incomingQuay) {
        boolean updated = false;
        List<String> oldList = new ArrayList<>(alreadyAdded.getOriginalNames());
        List<String> newList = new ArrayList<>(incomingQuay.getOriginalNames());


        if (alreadyAdded.getOriginalNames().size() != incomingQuay.getOriginalNames().size()) {
            updated = true;
        } else {
            Collections.sort(oldList);
            Collections.sort(newList);

            for (int i = 0; i < oldList.size(); i++) {
                if (!oldList.get(i).equals(newList.get(i))) {
                    updated = true;
                }
            }
        }

        alreadyAdded.getOriginalNames().clear();
        alreadyAdded.getOriginalNames().addAll(newList);

        return updated;
    }

    private boolean updateCentroid(Quay alreadyAdded, Quay incomingQuay) {
        if (alreadyAdded.getCentroid() != null && incomingQuay.getCentroid() != null && !incomingQuay.getCentroid().equals(alreadyAdded.getCentroid())) {
            alreadyAdded.setCentroid(incomingQuay.getCentroid());
            return true;
        }
        return false;
    }

    public boolean checkNumberProducers(Map<String, org.rutebanken.tiamat.model.Value> existing, Map<String, org.rutebanken.tiamat.model.Value> incoming) {
        Set<String> prefixProducersList = new HashSet<>();

        for (String key : existing.get(ORIGINAL_ID_KEY).getItems()) {
            String[] prefixList = key.split(":");
            String prefix = prefixList[0];
            prefixProducersList.add(prefix);
        }

        for (String key : incoming.get(ORIGINAL_ID_KEY).getItems()) {
            String[] prefixList = key.split(":");
            String prefix = prefixList[0];
            prefixProducersList.add(prefix);
        }

        return prefixProducersList.size() == 1;
    }


    private boolean mergeFields(Quay from, Quay to) {
        boolean changed = false;
        if (hasNameValue(from.getName()) && !hasNameValue(to.getName())) {
            to.setName(from.getName());
            changed = true;
        }
        if (from.getCompassBearing() != null && to.getCompassBearing() == null) {
            to.setCompassBearing(from.getCompassBearing());
            changed = true;
        }

        return changed;
    }

    private boolean matches(Quay incomingQuay, Quay alreadyAdded, double mergeDistance) {
        boolean nameMatch = haveMatchingNameOrOneIsMissing(incomingQuay, alreadyAdded);
        boolean haveSimilarOrNullCompassBearing = haveSimilarOrAnyNullCompassBearing(incomingQuay, alreadyAdded);
        if (areClose(incomingQuay, alreadyAdded, mergeDistance)
                && haveSimilarOrNullCompassBearing
                && nameMatch) {
            logger.trace("Matches: nameMatch true, haveSimilarOrAnyNullCompassBearing: true");
            return true;
        } else if (nameMatch && haveMatchingPublicCodeOrOneIsMissing(incomingQuay, alreadyAdded) && haveSimilarOrNullCompassBearing) {
            logger.trace("Name and compass bearing match (or null). Will compare with a greater limit of distance between quays. {}  {}", incomingQuay, alreadyAdded);
            return areClose(incomingQuay, alreadyAdded, MERGE_DISTANCE_METERS_EXTENDED);
        }
        return false;
    }


    public boolean haveMatchingNameOrOneIsMissing(Quay quay1, Quay quay2) {
        boolean quay1HasName = hasNameValue(quay1.getName());
        boolean quay2HasName = hasNameValue(quay2.getName());

        if (!quay1HasName && !quay2HasName) {
            logger.debug("None of the quays have name set. Treating as match. {} - {}", quay1.getName(), quay2.getName());
            return true;
        }

        if ((quay1HasName && !quay2HasName) || (!quay1HasName && quay2HasName)) {
            logger.debug("Only one of the quays have name set. Treating as match. {} - {}", quay1.getName(), quay2.getName());
            return true;
        }

        if (quay1.getName().getValue().equals(quay2.getName().getValue())) {
            logger.debug("Quay names matches. {} - {}", quay1.getName(), quay2.getName());
            return true;
        }

        logger.debug("Both quays does have names, but they do not match. {} - {}", quay1.getName(), quay2.getName());
        return false;
    }

    private boolean hasNameValue(MultilingualString multilingualString) {
        return multilingualString != null && !StringUtils.isBlank(multilingualString.getValue());
    }

    public boolean areClose(Quay quay1, Quay quay2) {
        return areClose(quay1, quay2, MERGE_DISTANCE_METERS);
    }

    public boolean areClose(Quay quay1, Quay quay2, double mergeDistanceInMeters) {
        if (!quay1.hasCoordinates() || !quay2.hasCoordinates()) {
            return false;
        }

        try {
            double distanceInMeters = JTS.orthodromicDistance(
                    quay1.getCentroid().getCoordinate(),
                    quay2.getCentroid().getCoordinate(),
                    DefaultGeographicCRS.WGS84);

            logger.debug("Distance in meters between quays is {} meters. {} - {}", distanceInMeters, quay1, quay2);

            return distanceInMeters < mergeDistanceInMeters;
        } catch (TransformException e) {
            logger.warn("Could not calculate distance between quays {} - {}", quay1, quay2, e);
            return false;
        }
    }

    public boolean haveSimilarOrAnyNullCompassBearing(Quay quay1, Quay quay2) {

        if (quay1.getCompassBearing() == null && quay2.getCompassBearing() == null) {
            return true;
        } else if ((quay1.getCompassBearing() == null && quay2.getCompassBearing() != null) || (quay1.getCompassBearing() != null && quay2.getCompassBearing() == null)) {
            return true;
        }

        return haveSimilarCompassBearing(quay1, quay2);
    }

    private boolean haveSimilarCompassBearing(Quay quay1, Quay quay2) {

        if (quay1.getCompassBearing() == null || quay2.getCompassBearing() == null) {
            return false;
        }
        int quayBearing1 = Math.round(quay1.getCompassBearing());
        int quayBearing2 = Math.round(quay2.getCompassBearing());

        int difference = Math.abs(getAngle(quayBearing1, quayBearing2));

        if (difference > maxCompassBearingDifference) {
            logger.debug("Quays have too much difference in compass bearing {}. {} {}", difference, quay1, quay2);
            return false;
        }

        logger.debug("Compass bearings for quays has less difference than the limit {}. {} {}", difference, quay1, quay2);
        return true;
    }

    private boolean haveMatchingPublicCodeOrOneIsMissing(Quay quay1, Quay quay2) {
        if ((quay1.getPublicCode() == null && quay2.getPublicCode() != null)
                || (quay1.getPublicCode() != null && quay1.getPublicCode() == null)) {
            return true;
        }

        return quay1.getPublicCode() != null && quay1.getPublicCode() != null && Objects.equals(quay1.getPublicCode(), quay2.getPublicCode());
    }

    private int getAngle(Integer bearing, Integer heading) {
        return ((((bearing - heading) % 360) + 540) % 360) - 180;

    }


}
