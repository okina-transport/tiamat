package org.rutebanken.tiamat.importer.matching;

import org.rutebanken.tiamat.importer.StopPlaceSharingPolicy;
import org.rutebanken.tiamat.importer.merging.MergingStopPlaceImporter;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.service.stopplace.StopPlaceQuayMover;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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

    private org.rutebanken.tiamat.model.StopPlace targetStopPlace;

    @Value("${stopPlace.sharing.policy}")
    protected StopPlaceSharingPolicy sharingPolicy;


    /**
     * On boucle sur les arrêts trouvés avec les identifiants des quais du point d'arrêt arrivant
     * @param foundStopPlaces
     * @param incomingStopPlace
     * @throws ExecutionException
     */
    public void doMove(List<StopPlace> foundStopPlaces, org.rutebanken.tiamat.model.StopPlace incomingStopPlace) throws ExecutionException {

        //Initialisation du StopPlace cible
        targetStopPlace = new org.rutebanken.tiamat.model.StopPlace();

        //Si le StopPlace existe déjà on le récupère
        boolean incomingStopPlaceAlreadyExists = false;
        for (StopPlace stopPlace : foundStopPlaces) {
            if (stopPlace.getOriginalIds().stream().anyMatch(originalId -> incomingStopPlace.getOriginalIds().contains(originalId))) {
                incomingStopPlaceAlreadyExists = true;
                targetStopPlace = stopPlace;
                break;
            }
        }

        //Sinon on le crée
        if (!incomingStopPlaceAlreadyExists) {
            StopPlace copyIncomingStopPlace = versionCreator.createCopy(incomingStopPlace, StopPlace.class);
            copyIncomingStopPlace.getQuays().clear();
            targetStopPlace = mergingStopPlaceImporter.handleCompletelyNewStopPlace(copyIncomingStopPlace);
        }

        //On construit une liste qui contient l'ensemble des originalIds des quays de l'incoming StopPlace
        List<String> incomingQuaysOriginalIds = new ArrayList<>();
        for (Quay quay : incomingStopPlace.getQuays()) {
            incomingQuaysOriginalIds.addAll(quay.getOriginalIds());
        }

        //On finit par déplacer les quais
        Map<Quay, StopPlace> quaysToMoveMap = new HashMap<>();
        for (StopPlace stopPlace : foundStopPlaces) {
            //On écarte tout de suite le stopplace cible
            if (stopPlace.getNetexId().equals(targetStopPlace.getNetexId())) {
                continue;
            }
            //on déplace tous les quais concernés en comparant avec la liste préalablement construite
            for (Quay quay : stopPlace.getQuays()) {
                if (quay.getOriginalIds().stream().anyMatch(originalId -> incomingQuaysOriginalIds.contains(originalId))) {
                    if (incomingStopPlaceAlreadyExists) {
                        stopPlaceQuayMover.moveQuays(Collections.singletonList(quay.getNetexId()), targetStopPlace.getNetexId(), "Move the quay to existing stop place", "Move the quay to existing stop place");
                    } else {
                        stopPlaceQuayMover.moveQuays(Collections.singletonList(quay.getNetexId()), targetStopPlace.getNetexId(), "Move the quay to new stop place", "Move the quay to new stop place");
                    }
                }
            }
        }
    }
}
