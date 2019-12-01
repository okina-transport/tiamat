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

import com.google.api.client.repackaged.com.google.common.base.Strings;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.operation.TransformException;
import org.rutebanken.tiamat.importer.matching.OriginalIdMatcher;
import org.rutebanken.tiamat.model.MultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    public final double MERGE_DISTANCE_METERS_IGNORE_ID_MATCH = 3;

    @Value("${quayMerger.mergeDistanceMeters:8}")
    public final double MERGE_DISTANCE_METERS = 8;

    @Value("${quayMerger.mergeDistanceMetersExtended:30}")
    public final double MERGE_DISTANCE_METERS_EXTENDED = 30;

    @Value("${quayMerger.maxCompassBearingDifference:60}")
    private final int maxCompassBearingDifference = 60;

    private final OriginalIdMatcher originalIdMatcher;

    @Autowired
    public QuayMerger(OriginalIdMatcher originalIdMatcher) {
        this.originalIdMatcher = originalIdMatcher;
    }

    public boolean mergeQuays(StopPlace newStopPlace, StopPlace existingStopPlace, boolean addNewQuays) {
        return mergeQuays(newStopPlace, existingStopPlace, addNewQuays, true, true);
    }

    @Autowired
    private QuayRepository quayRepository;


    /**
     * Inspect quays from incoming AND matching stop place. If they do not exist from before, add them.
     */
    public boolean mergeQuays(StopPlace newStopPlace, StopPlace existingStopPlace, boolean addNewQuays, boolean lowDistanceCheckBeforeIdMatch, boolean stopPlaceAlone) {

        AtomicInteger updatedQuays = new AtomicInteger();
        AtomicInteger addedQuays = new AtomicInteger();

        logger.debug("About to compare quays for {}", existingStopPlace.getNetexId());

        if (newStopPlace.getQuays() == null) {
            newStopPlace.setQuays(new HashSet<>());
        }

        Set<Quay> result = mergeQuays(newStopPlace, newStopPlace.getQuays(), existingStopPlace.getQuays(), updatedQuays, addedQuays, addNewQuays, lowDistanceCheckBeforeIdMatch, stopPlaceAlone);

        existingStopPlace.setQuays(result);

        logger.debug("Created {} quays and updated {} quays for stop place {}", addedQuays.get(), updatedQuays.get(), existingStopPlace);
        return addedQuays.get() > 0 || updatedQuays.get() > 0;
    }

    public Set<Quay> mergeQuays(Set<Quay> newQuays, Set<Quay> existingQuays, AtomicInteger updatedQuaysCounter, AtomicInteger addedQuaysCounter, boolean addNewQuays) {
        return mergeQuays(null, newQuays, existingQuays, updatedQuaysCounter, addedQuaysCounter, addNewQuays, true, false);
    }

    public Set<Quay> mergeQuays(StopPlace newStopPlace, Set<Quay> newQuays, Set<Quay> existingQuays, AtomicInteger updatedQuaysCounter, AtomicInteger addedQuaysCounter, boolean addNewQuays) {
        return mergeQuays(newStopPlace, newQuays, existingQuays, updatedQuaysCounter, addedQuaysCounter, addNewQuays, true, false);
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
    public Set<Quay> mergeQuays(StopPlace newStopPlace, Set<Quay> newQuays, Set<Quay> existingQuays, AtomicInteger updatedQuaysCounter, AtomicInteger addedQuaysCounter, boolean addNewQuays, boolean lowDistanceCheckBeforeIdMatch, boolean stopPlaceAlone) {

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

            if (!matchingQuay.isPresent()) {
                if (incomingQuay != null && incomingQuay.getNetexId() != null) {
                    matchingQuay = result.stream()
                            .filter(quay -> incomingQuay.getNetexId().equals(quay.getNetexId()))
                            .findFirst();
                }
            }

            if (!matchingQuay.isPresent()) {
                matchingQuay = findMatchOnOriginalId(incomingQuay, result);
            }

            if (!matchingQuay.isPresent()) {
                matchingQuay = findMatch(incomingQuay, result, MERGE_DISTANCE_METERS);
            }

            if (matchingQuay.isPresent()) {
                updateIfChanged(matchingQuay.get(), incomingQuay, updatedQuaysCounter, stopPlaceAlone, newStopPlace);
            } else if (addNewQuays) {
                logger.info("Found no match for existing quay {}. Adding it!", incomingQuay);
                Quay quay = null;
                if (incomingQuay != null && incomingQuay.getNetexId() != null) {
                     quay = quayRepository.findFirstByNetexIdOrderByVersionDesc(incomingQuay.getNetexId());
                }
                if (quay == null){
                    result.add(incomingQuay);
                    incomingQuay.setCreated(Instant.now());
                    incomingQuay.setChanged(Instant.now());
                    addedQuaysCounter.incrementAndGet();
                }
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
            if (originalIdMatcher.matchesOnOriginalId(incomingQuay, alreadyAdded)) {
                return Optional.of(alreadyAdded);
            }
        }
        return Optional.empty();
    }

    private void updateIfChanged(Quay alreadyAdded, Quay incomingQuay, AtomicInteger updatedQuaysCounter, boolean stopPlaceAlone, StopPlace newStopPlace) {
        // The incoming quay could for some reason already have multiple imported IDs.
        boolean quayAlone = checkNumberProducers(alreadyAdded.getKeyValues(), incomingQuay.getKeyValues());
        boolean multipleIds = checkNumberId(alreadyAdded.getKeyValues(), incomingQuay.getKeyValues());
        boolean idUpdated = alreadyAdded.getOriginalIds().addAll(incomingQuay.getOriginalIds());
        boolean nameUpdated = updateName(alreadyAdded, incomingQuay, newStopPlace);
        boolean changedByMerge = mergeFields(incomingQuay, alreadyAdded);
        boolean centroidUpdated = updateCentroid(alreadyAdded, incomingQuay, stopPlaceAlone, quayAlone, multipleIds);
        boolean stopCodeUpdated = updateCodes(alreadyAdded, incomingQuay, stopPlaceAlone, quayAlone);

        if (idUpdated || nameUpdated || changedByMerge || centroidUpdated || stopCodeUpdated) {
            logger.debug("Quay changed. idUpdated: {}, nameUpdated: {}, merged fields? {}, centroidUpdated: {}, stopCodesUpdated: {}. Quay: {}", idUpdated, nameUpdated, changedByMerge, centroidUpdated, stopCodeUpdated, alreadyAdded);

            alreadyAdded.setChanged(Instant.now());
            updatedQuaysCounter.incrementAndGet();
        }
    }

    private boolean updateName(Quay alreadyAdded, Quay incomingQuay, StopPlace newStopPlace) {
        if(incomingQuay.getOriginalNames() != null && !incomingQuay.getOriginalNames().isEmpty()){
            return alreadyAdded.getOriginalNames().addAll(incomingQuay.getOriginalNames());
        }
        else if(incomingQuay.getName().getValue() != null){
            return alreadyAdded.getOriginalNames().add(incomingQuay.getName().getValue());
        }
        else{
            return alreadyAdded.getOriginalNames().add(newStopPlace.getName().getValue());
        }
    }

    private boolean updateCodes(Quay alreadyAdded, Quay incomingQuay, boolean stopPlaceAlone, boolean quayAlone) {
        if (!alreadyAdded.getOriginalStopCodes().contains(incomingQuay.getPublicCode())) {
            return alreadyAdded.getOriginalStopCodes().add(alreadyAdded.getPublicCode());
        }

        if (incomingQuay.getPublicCode() != null && !incomingQuay.getPublicCode().equals(alreadyAdded.getPublicCode())) {
            if (stopPlaceAlone && quayAlone) {
                alreadyAdded.setPublicCode(incomingQuay.getPublicCode());
            }
            return alreadyAdded.getOriginalStopCodes().add(incomingQuay.getPublicCode());
        }

        if (incomingQuay.getPrivateCode() != null && !incomingQuay.getPrivateCode().equals(alreadyAdded.getPrivateCode()) && stopPlaceAlone && quayAlone) {
            alreadyAdded.setPrivateCode(incomingQuay.getPrivateCode());
            return true;
        }

        return false;
    }

    private boolean updateCentroid(Quay alreadyAdded, Quay incomingQuay, boolean stopPlaceAlone, boolean quayAlone, boolean multipleIds) {
        if (alreadyAdded.getCentroid() != null && incomingQuay.getCentroid() != null && stopPlaceAlone && quayAlone && multipleIds) {
            if (!incomingQuay.getCentroid().equals(alreadyAdded.getCentroid())) {
                alreadyAdded.setCentroid(incomingQuay.getCentroid());
                return true;
            }
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


    public boolean checkNumberId(Map<String, org.rutebanken.tiamat.model.Value> existing, Map<String, org.rutebanken.tiamat.model.Value> incoming) {
        Set<String> idsList = new HashSet<>();

        idsList.addAll(existing.get(ORIGINAL_ID_KEY).getItems());
        idsList.addAll(incoming.get(ORIGINAL_ID_KEY).getItems());

        return idsList.size() == 1;
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
        return multilingualString != null && !Strings.isNullOrEmpty(multilingualString.getValue());
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
