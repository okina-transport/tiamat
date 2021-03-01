package org.rutebanken.tiamat.importer.matching;

import org.rutebanken.tiamat.importer.merging.MergingStopPlaceImporter;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.service.stopplace.StopPlaceQuayMover;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Classe pour déplacer les quais d'un arrêt à un autre
 */
@Component
@Transactional
public class QuayMover {

    @Autowired
    private StopPlaceQuayMover stopPlaceQuayMover;

    @Autowired
    protected VersionCreator versionCreator;

    @Autowired
    private MergingStopPlaceImporter mergingStopPlaceImporter;

    private org.rutebanken.tiamat.model.StopPlace oldIncomingStopPlace;

    /**
     * On boucle sur les arrêts trouvés avec les identifiants des quais du point d'arrêt arrivant
     * @param foundStopPlaces
     * @param incomingStopPlace
     * @throws ExecutionException
     */
    public void doMove(List<StopPlace> foundStopPlaces, org.rutebanken.tiamat.model.StopPlace incomingStopPlace) throws ExecutionException {
        oldIncomingStopPlace = new org.rutebanken.tiamat.model.StopPlace();

        // Dans les arrêts trouvés on vérifie si le quay a le même original id que le quay trouvé
        for (org.rutebanken.tiamat.model.StopPlace foundStopPlace : foundStopPlaces) {
            for (Quay foundQuay : foundStopPlace.getQuays()) {
                for (Quay incomingQuay : incomingStopPlace.getQuays()) {
                    doMoveQuayInFoundOrCreatedStopplace(foundStopPlaces, incomingStopPlace, foundStopPlace, foundQuay, incomingQuay);
                }
            }
        }
    }

    /**
     * On déplace les quais déjà présents en base qui ont le même id que les quais du point d'arrêt arrivant dans un arrêt commercial différent de celui arrivant mais qui est soit déjà existant en base soit à créer
     * @param foundStopPlaces
     * @param incomingStopPlace
     * @param foundStopPlace
     * @param foundQuay
     * @param incomingQuay
     * @throws ExecutionException
     */
    private void doMoveQuayInFoundOrCreatedStopplace(List<StopPlace> foundStopPlaces, StopPlace incomingStopPlace, StopPlace foundStopPlace, Quay foundQuay, Quay incomingQuay) throws ExecutionException {
        for (String originalIdFoundQuay : foundQuay.getOriginalIds()) {
            if (!incomingQuay.getOriginalIds().contains(originalIdFoundQuay)) continue;

            for (String originalIdFoundStopPlace : foundStopPlace.getOriginalIds()) {
                if (!incomingStopPlace.getOriginalIds().contains(originalIdFoundStopPlace)) {
                    if(!checkAndMoveIfFoundExistingStopplaceWithIdIncoming(foundStopPlaces, incomingStopPlace, foundStopPlace, foundQuay)){
                        createStopplaceIfNeeded(incomingStopPlace, foundQuay);
                    }
                }
            }
        }
    }

    /**
     * On vérifie que l'original id de l'arrêt commercial arrivant n'est pas présent dans la liste des arrêts commerciaux trouvés
     * Si oui on y déplace les quais des autres arrêts trouvés qui correspondent à ceux de l'arrêt arrivant
     * @param foundStopPlaces
     * @param incomingStopPlace
     * @param foundStopPlace
     * @param foundQuay
     */
    private boolean checkAndMoveIfFoundExistingStopplaceWithIdIncoming(List<StopPlace> foundStopPlaces, StopPlace incomingStopPlace, StopPlace foundStopPlace, Quay foundQuay) {
        for (StopPlace checkExistingIdInFoundStopPlace : foundStopPlaces) {
            for (String originalIdIncomingStopPlace : incomingStopPlace.getOriginalIds()) {
                if (checkExistingIdInFoundStopPlace.getOriginalIds().contains(originalIdIncomingStopPlace) && !checkExistingIdInFoundStopPlace.getNetexId().equals(foundStopPlace.getNetexId())) {
                    stopPlaceQuayMover.moveQuays(Collections.singletonList(foundQuay.getNetexId()), checkExistingIdInFoundStopPlace.getNetexId(), "Move the quay to existing stop place", "Move the quay to existing stop place");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Si non on crée un nouvel arrêt commercial et on y ajoute les quais qui correspondent à ceux de l'arrêt arrivant
     * @param incomingStopPlace
     * @param foundQuay
     * @throws ExecutionException
     */
    private void createStopplaceIfNeeded(StopPlace incomingStopPlace, Quay foundQuay) throws ExecutionException {
        if (oldIncomingStopPlace.getNetexId() == null) {
            StopPlace copyIncomingStopPlace = versionCreator.createCopy(incomingStopPlace, StopPlace.class);
            copyIncomingStopPlace.getQuays().clear();
            oldIncomingStopPlace = mergingStopPlaceImporter.handleCompletelyNewStopPlace(copyIncomingStopPlace);
        }
        stopPlaceQuayMover.moveQuays(Collections.singletonList(foundQuay.getNetexId()), oldIncomingStopPlace.getNetexId(), "Move the quay to new stop place", "Move the quay to new stop place");
    }
}
