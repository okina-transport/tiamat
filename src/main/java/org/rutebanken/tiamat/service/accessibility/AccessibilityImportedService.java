package org.rutebanken.tiamat.service.accessibility;

import org.rutebanken.tiamat.model.*;

import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.rutebanken.tiamat.versioning.VersionCreator;
import org.rutebanken.tiamat.versioning.save.StopPlaceVersionedSaverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Transactional
public class AccessibilityImportedService {

    private static final Logger logger = LoggerFactory.getLogger(AccessibilityImportedService.class);

    private final StopPlaceRepository stopPlaceRepository;
    private final VersionCreator versionCreator;
    private final StopPlaceVersionedSaverService stopPlaceVersionedSaverService;

    @Autowired
    AccessibilityImportedService(StopPlaceRepository stopPlaceRepository, VersionCreator versionCreator, StopPlaceVersionedSaverService stopPlaceVersionedSaverService) {
        this.stopPlaceRepository = stopPlaceRepository;
        this.versionCreator = versionCreator;
        this.stopPlaceVersionedSaverService = stopPlaceVersionedSaverService;
    }

    /**
     * Récupère la valeur spécifique d'une limitation d'accessibilité pour un quai ou un arrêt commercial.
     * Cette méthode extrait l'évaluation d'accessibilité d'un {@link Quay} ou d'un {@link StopPlace},
     * puis utilise une fonction mapper pour obtenir la valeur de limitation spécifique.
     *
     * @param quay      Le quai pour lequel obtenir la limitation d'accessibilité, peut être {@code null} si stopPlace est utilisé.
     * @param stopPlace L'arrêt commercial pour lequel obtenir la limitation d'accessibilité, peut être {@code null} si quay est utilisé.
     * @param mapper    La fonction qui extrait la valeur spécifique de limitation d'accessibilité d'une {@link AccessibilityLimitation}.
     * @return La valeur de la limitation d'accessibilité correspondante, ou UNKNOWN si non trouvée.
     */
    private static LimitationStatusEnumeration getAccessibilityLimitation(Quay quay, StopPlace stopPlace, Function<AccessibilityLimitation, LimitationStatusEnumeration> mapper) {
        AccessibilityAssessment assessment = (quay != null) ? quay.getAccessibilityAssessment() : stopPlace.getAccessibilityAssessment();
        return assessment.getLimitations().stream()
                .findFirst()
                .map(mapper)
                .orElse(LimitationStatusEnumeration.UNKNOWN);
    }

    /**
     * Crée une nouvelle {@link AccessibilityLimitation} pour un quai ou un arrêt commercial,
     * en compilant les informations d'accessibilité depuis les sources fournies.
     * Utilise une série de getters et setters pour appliquer les valeurs d'accessibilité
     * depuis le quai ou l'arrêt commercial au nouvel objet de limitation d'accessibilité.
     *
     * @param quay      Le quai à partir duquel obtenir les informations d'accessibilité, peut être {@code null}.
     * @param stopPlace L'arrêt commercial à partir duquel obtenir les informations d'accessibilité, peut être {@code null}.
     * @return Une nouvelle instance de {@link AccessibilityLimitation} peuplée avec les informations d'accessibilité appropriées.
     */
    private AccessibilityLimitation newAccessibilityLimitation(Quay quay, StopPlace stopPlace) {
        AccessibilityLimitation newAccessibilityLimitation = new AccessibilityLimitation();

        // Définition des getters pour chaque type d'accessibilité
        List<Function<AccessibilityLimitation, LimitationStatusEnumeration>> getters = Arrays.asList(
                AccessibilityLimitation::getWheelchairAccess,
                AccessibilityLimitation::getAudibleSignalsAvailable,
                AccessibilityLimitation::getEscalatorFreeAccess,
                AccessibilityLimitation::getLiftFreeAccess,
                AccessibilityLimitation::getStepFreeAccess,
                AccessibilityLimitation::getVisualSignsAvailable
        );

        // Définition des setters pour chaque type d'accessibilité
        List<BiConsumer<AccessibilityLimitation, LimitationStatusEnumeration>> setters = Arrays.asList(
                AccessibilityLimitation::setWheelchairAccess,
                AccessibilityLimitation::setAudibleSignalsAvailable,
                AccessibilityLimitation::setEscalatorFreeAccess,
                AccessibilityLimitation::setLiftFreeAccess,
                AccessibilityLimitation::setStepFreeAccess,
                AccessibilityLimitation::setVisualSignsAvailable
        );

        for (int i = 0; i < getters.size(); i++) {
            LimitationStatusEnumeration value = getAccessibilityLimitation(quay, stopPlace, getters.get(i));
            setters.get(i).accept(newAccessibilityLimitation, value);
        }

        return newAccessibilityLimitation;
    }


    /**
     * Met à jour les informations d'accessibilité pour une liste d'arrêts commerciaux.
     * Pour chaque arrêt commercial à sauvegarder, cette méthode recherche d'abord les arrêts commerciaux correspondants
     * dans la base de données par leur identifiant Netex. Ensuite, elle met à jour l'évaluation
     * d'accessibilité de chaque arrêt commercial trouvé ainsi que de ses quais avec les nouvelles informations fournies, et enregistre
     * les modifications dans la base de données.
     *
     * @param stopPlaces La liste des arrêts commerciaux dont les informations d'accessibilité doivent être mises à jour.
     * @return La liste des arrêts commerciaux effectivement mis à jour et sauvegardés dans la base de données.
     */
    public void updateAccessibilityStopPlacesAndQuays(List<StopPlace> stopPlaces) {
        for (StopPlace stopPlaceToSave : stopPlaces) {
            try {
                StopPlace existingStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(stopPlaceToSave.getNetexId());
                StopPlace newVersionStopPlace = versionCreator.createCopy(existingStopPlace, StopPlace.class);

                AccessibilityLimitation newAccessibilityLimitation = newAccessibilityLimitation(null, stopPlaceToSave);
                AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
                accessibilityAssessment.setMobilityImpairedAccess(stopPlaceToSave.getAccessibilityAssessment().getMobilityImpairedAccess());
                accessibilityAssessment.setLimitations(List.of(newAccessibilityLimitation));
                newVersionStopPlace.setAccessibilityAssessment(accessibilityAssessment);
                newVersionStopPlace.getQuays().forEach(quay -> {
                    AccessibilityLimitation accessibilityLimitationQuay = new AccessibilityLimitation();
                    BeanUtils.copyProperties(accessibilityAssessment.getLimitations().get(0), accessibilityLimitationQuay);
                    AccessibilityAssessment accessibilityAssessmentQuay = new AccessibilityAssessment();
                    BeanUtils.copyProperties(accessibilityAssessment, accessibilityAssessmentQuay);
                    accessibilityAssessmentQuay.setLimitations(List.of(accessibilityLimitationQuay));
                    quay.setAccessibilityAssessment(accessibilityAssessmentQuay);
                });

                try {
                    saveNewVersionStopPlace(existingStopPlace, newVersionStopPlace, true);
                } catch (Exception e) {
                    logger.warn("Cannot update stop place with netexId : {}", existingStopPlace.getNetexId(), e);
                }
            } catch (Exception e) {
                logger.warn("Cannot find in BDD stop place with netexId : {}", stopPlaceToSave.getNetexId(), e);
            }
        }
    }

    private void saveNewVersionStopPlace(StopPlace existingStopPlace, StopPlace newVersionStopPlace, boolean optimizeAccessibilityAssessments) {
        if (existingStopPlace.getParentSiteRef() != null && !existingStopPlace.isParentStopPlace()) {
            StopPlace existingParentStopPlace = stopPlaceRepository.findFirstByNetexIdOrderByVersionDesc(existingStopPlace.getParentSiteRef().getRef());
            StopPlace copyParentStopPlace = versionCreator.createCopy(existingParentStopPlace, StopPlace.class);
            copyParentStopPlace.getChildren().removeIf(stopPlace -> stopPlace.getNetexId().equals(newVersionStopPlace.getNetexId()));
            copyParentStopPlace.getChildren().add(newVersionStopPlace);
            copyParentStopPlace = stopPlaceVersionedSaverService.saveNewVersion(existingParentStopPlace, copyParentStopPlace, optimizeAccessibilityAssessments);
            copyParentStopPlace.getChildren().stream().filter(stopPlace -> stopPlace.getNetexId().equals(newVersionStopPlace.getNetexId())).findFirst().get();
        } else {
            stopPlaceVersionedSaverService.saveNewVersion(existingStopPlace, newVersionStopPlace, optimizeAccessibilityAssessments);
        }
    }

    /**
     * Met à jour les évaluations d'accessibilité des quais et des arrêts commerciaux associés.
     * - Récupère le StopPlace existant pour chaque quai
     * - Crée une nouvelle version du StopPlace avec les mises à jour
     * - Met à jour l'accessibilité du quai et fusionne les valeurs d'accessibilité pour le StopPlace
     * - Sauvegarde la nouvelle version du StopPlace
     *
     * @param quays Liste des quais contenant des informations mises à jour sur l'accessibilité.
     */
    public void updateAccessibilityQuaysAndStopPlaces(List<Quay> quays) {
        for (Quay quay : quays) {
            StopPlace existingStopPlace = stopPlaceRepository.findFirstStopPlaceByNetexQuayOrderByVersionDesc(quay.getNetexId());
            StopPlace newVersionStopPlace = versionCreator.createCopy(existingStopPlace, StopPlace.class);

            // Création d'une nouvelle évaluation d'accessibilité pour le quai
            AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
            accessibilityAssessment.setMobilityImpairedAccess(quay.getAccessibilityAssessment().getMobilityImpairedAccess());
            accessibilityAssessment.setLimitations(List.of(newAccessibilityLimitation(quay, null)));

            // Mise à jour du quai dans le StopPlace
            newVersionStopPlace.getQuays().stream()
                    .filter(newQuay -> newQuay.getNetexId().equals(quay.getNetexId()))
                    .forEach(newQuay -> newQuay.setAccessibilityAssessment(accessibilityAssessment));

            // Fusion des accessibilités des quais pour calculer celle du StopPlace
            aggregateQuayAccessibilitiesToStopPlace(newVersionStopPlace);

            // Sauvegarde du StopPlace avec gestion des erreurs
            try {
                saveNewVersionStopPlace(existingStopPlace, newVersionStopPlace, false);
            } catch (Exception e) {
                logger.warn("Cannot update stop place with netexId : {}", existingStopPlace.getNetexId(), e);
            }
        }
    }

    /**
     * Agrège les valeurs d'accessibilité des quais pour en déduire celle du StopPlace.
     * - Applique une règle de fusion pour déterminer la valeur finale
     * - Gère proprement les valeurs nulles
     *
     * @param quayValues Liste des valeurs d'accessibilité des quais
     * @return La valeur agrégée selon la règle de fusion
     */
    private LimitationStatusEnumeration aggregateQuayAccessibilities(List<LimitationStatusEnumeration> quayValues) {
        if (quayValues == null || quayValues.isEmpty()) {
            return LimitationStatusEnumeration.UNKNOWN; // Par défaut si aucun quai n'est renseigné
        }

        LimitationStatusEnumeration result = quayValues.get(0);

        for (int i = 1; i < quayValues.size(); i++) {
            result = compareTwoAccessibilities(result, quayValues.get(i));
        }

        return result;
    }

    /**
     * Applique la règle de fusion entre deux valeurs d'accessibilité en respectant le tableau défini.
     * - TRUE est prioritaire
     * - PARTIAL est intermédiaire
     * - UNKNOWN et FALSE nécessitent une gestion spécifique
     *
     * @param value1 Première valeur à comparer
     * @param value2 Deuxième valeur à comparer
     * @return La valeur fusionnée selon la logique définie
     */
    private LimitationStatusEnumeration compareTwoAccessibilities(LimitationStatusEnumeration value1, LimitationStatusEnumeration value2) {
        if (value1 == null) return value2;
        if (value2 == null) return value1;

        if (value1 == LimitationStatusEnumeration.TRUE && value2 == LimitationStatusEnumeration.TRUE)
            return LimitationStatusEnumeration.TRUE;
        if (value1 == LimitationStatusEnumeration.TRUE || value2 == LimitationStatusEnumeration.TRUE)
            return LimitationStatusEnumeration.PARTIAL;

        if (value1 == LimitationStatusEnumeration.FALSE && value2 == LimitationStatusEnumeration.FALSE)
            return LimitationStatusEnumeration.FALSE;

        // Si l'un est FALSE et l'autre UNKNOWN, on retourne UNKNOWN
        if ((value1 == LimitationStatusEnumeration.FALSE && value2 == LimitationStatusEnumeration.UNKNOWN) ||
                (value1 == LimitationStatusEnumeration.UNKNOWN && value2 == LimitationStatusEnumeration.FALSE)) {
            return LimitationStatusEnumeration.UNKNOWN;
        }

        // PARTIAL est prioritaire sur FALSE
        if (value1 == LimitationStatusEnumeration.PARTIAL || value2 == LimitationStatusEnumeration.PARTIAL)
            return LimitationStatusEnumeration.PARTIAL;

        // Si l'un est UNKNOWN et qu'on n'a pas de FALSE, on retourne UNKNOWN
        if (value1 == LimitationStatusEnumeration.UNKNOWN || value2 == LimitationStatusEnumeration.UNKNOWN)
            return LimitationStatusEnumeration.UNKNOWN;

        return LimitationStatusEnumeration.UNKNOWN;
    }

    /**
     * Fusionne les évaluations d'accessibilité de tous les quais pour mettre à jour le StopPlace.
     * - Vérifie et initialise `AccessibilityAssessment` et `Limitations` pour chaque quai et le StopPlace
     * - Agrège les valeurs de chaque critère d'accessibilité à partir des quais
     * - Met à jour le StopPlace avec les nouvelles valeurs agrégées
     *
     * @param newVersionStopPlace StopPlace mis à jour avec les nouvelles valeurs agrégées
     */
    protected void aggregateQuayAccessibilitiesToStopPlace(StopPlace newVersionStopPlace) {

        // Initialiser chaque quai si nécessaire
        newVersionStopPlace.getQuays().forEach(this::initializeQuayAccessibility);

        // Initialiser l'assessment du StopPlace si nécessaire
        AccessibilityAssessment stopPlaceAssessment = newVersionStopPlace.getAccessibilityAssessment();
        if (stopPlaceAssessment == null) {
            stopPlaceAssessment = new AccessibilityAssessment();
            newVersionStopPlace.setAccessibilityAssessment(stopPlaceAssessment);
        }

        // Fusion de `MobilityImpairedAccess`
        stopPlaceAssessment.setMobilityImpairedAccess(aggregateQuayAccessibilities(
                newVersionStopPlace.getQuays().stream()
                        .map(quay -> quay.getAccessibilityAssessment().getMobilityImpairedAccess())
                        .collect(Collectors.toList())
        ));

        // Vérifier et initialiser les limitations si nécessaire
        if (stopPlaceAssessment.getLimitations() == null || stopPlaceAssessment.getLimitations().isEmpty()) {
            stopPlaceAssessment.setLimitations(Collections.singletonList(new AccessibilityLimitation()));
        }

        AccessibilityLimitation stopPlaceLimitation = stopPlaceAssessment.getLimitations().get(0);

        // Définition des champs à agréger pour chaque critère d'accessibilité
        Map<Consumer<LimitationStatusEnumeration>, Function<Quay, LimitationStatusEnumeration>> fieldsToAggregate = Map.of(
                stopPlaceLimitation::setWheelchairAccess, quay -> quay.getAccessibilityAssessment().getLimitations().get(0).getWheelchairAccess(),
                stopPlaceLimitation::setAudibleSignalsAvailable, quay -> quay.getAccessibilityAssessment().getLimitations().get(0).getAudibleSignalsAvailable(),
                stopPlaceLimitation::setEscalatorFreeAccess, quay -> quay.getAccessibilityAssessment().getLimitations().get(0).getEscalatorFreeAccess(),
                stopPlaceLimitation::setLiftFreeAccess, quay -> quay.getAccessibilityAssessment().getLimitations().get(0).getLiftFreeAccess(),
                stopPlaceLimitation::setStepFreeAccess, quay -> quay.getAccessibilityAssessment().getLimitations().get(0).getStepFreeAccess(),
                stopPlaceLimitation::setVisualSignsAvailable, quay -> quay.getAccessibilityAssessment().getLimitations().get(0).getVisualSignsAvailable()
        );

        // Appliquer l'agrégation pour chaque limitation
        fieldsToAggregate.forEach((setter, getter) ->
                setter.accept(aggregateQuayAccessibilities(
                        newVersionStopPlace.getQuays().stream()
                                .map(getter)
                                .collect(Collectors.toList())
                ))
        );
    }

    /**
     * Initialise l'évaluation d'accessibilité (`AccessibilityAssessment`) et les limitations (`Limitations`)
     * pour un quai donné si ces valeurs sont `null`.
     *
     * @param quay Le quai dont l'accessibilité doit être initialisée
     */
    protected void initializeQuayAccessibility(Quay quay) {
        if (quay == null) return;

        // Vérifier et initialiser l'assessment
        if (quay.getAccessibilityAssessment() == null) {
            quay.setAccessibilityAssessment(new AccessibilityAssessment());
        }

        AccessibilityAssessment assessment = quay.getAccessibilityAssessment();

        // Vérifier et initialiser les limitations
        if (assessment.getLimitations() == null || assessment.getLimitations().isEmpty()) {
            assessment.setLimitations(Collections.singletonList(new AccessibilityLimitation()));
        }
    }
}
