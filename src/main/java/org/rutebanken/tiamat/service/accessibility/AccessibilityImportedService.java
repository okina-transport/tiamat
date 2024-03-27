package org.rutebanken.tiamat.service.accessibility;

import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.repository.QuayRepository;
import org.rutebanken.tiamat.repository.StopPlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;


@Service
@Transactional
public class AccessibilityImportedService {

    private static final Logger logger = LoggerFactory.getLogger(AccessibilityImportedService.class);

    private final QuayRepository quayRepository;
    private final StopPlaceRepository stopPlaceRepository;

    @Autowired
    AccessibilityImportedService(QuayRepository quayRepository, StopPlaceRepository stopPlaceRepository){
        this.quayRepository = quayRepository;
        this.stopPlaceRepository = stopPlaceRepository;
    }

    /**
     * Récupère la valeur spécifique d'une limitation d'accessibilité pour un quai ou un arrêt commercial.
     * Cette méthode extrait l'évaluation d'accessibilité d'un {@link Quay} ou d'un {@link StopPlace},
     * puis utilise une fonction mapper pour obtenir la valeur de limitation spécifique.
     *
     * @param quay Le quai pour lequel obtenir la limitation d'accessibilité, peut être {@code null} si stopPlace est utilisé.
     * @param stopPlace L'arrêt commercial pour lequel obtenir la limitation d'accessibilité, peut être {@code null} si quay est utilisé.
     * @param mapper La fonction qui extrait la valeur spécifique de limitation d'accessibilité d'une {@link AccessibilityLimitation}.
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
     * @param quay Le quai à partir duquel obtenir les informations d'accessibilité, peut être {@code null}.
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
     * Met à jour les informations d'accessibilité pour une liste de quais.
     * Pour chaque quai à sauvegarder, cette méthode recherche d'abord les quais correspondants
     * dans la base de données par leur identifiant Netex. Ensuite, elle met à jour l'évaluation
     * d'accessibilité de chaque quai trouvé avec les nouvelles informations fournies, et enregistre
     * les modifications dans la base de données.
     *
     * @param quaysToSave La liste des quais dont les informations d'accessibilité doivent être mises à jour.
     * @return La liste des quais effectivement mis à jour et sauvegardés dans la base de données.
     */
    public List<Quay> updateAccessibilityQuays(List<Quay> quaysToSave){
        List<Quay> quayList = new ArrayList<>();
        for(Quay quayToSave: quaysToSave){
            try {
                List<Quay> quayInBDD = quayRepository.findByNetexId(quayToSave.getNetexId());

                for(Quay quay: quayInBDD) {
                    AccessibilityLimitation newAccessibilityLimitation = newAccessibilityLimitation(quayToSave, null);
                    AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
                    accessibilityAssessment.setMobilityImpairedAccess(quayToSave.getAccessibilityAssessment().getMobilityImpairedAccess());
                    accessibilityAssessment.setLimitations(List.of(newAccessibilityLimitation));
                    quay.setAccessibilityAssessment(accessibilityAssessment);

                    try {
                        quayRepository.save(quay);
                        quayList.add(quay);
                    } catch (Exception e) {
                        logger.warn("Cannot update quay with netexId : " + quay.getNetexId(), e);
                    }
                }
            } catch (Exception e) {
                logger.warn("Cannot find in BDD quay with netexId : " + quayToSave.getNetexId(), e);
            }
        }
        return quayList;
    }

    /**
     * Met à jour les informations d'accessibilité pour une liste d'arrêts commerciaux.
     * Pour chaque arrêt commercial à sauvegarder, cette méthode recherche d'abord les arrêts commerciaux correspondants
     * dans la base de données par leur identifiant Netex. Ensuite, elle met à jour l'évaluation
     * d'accessibilité de chaque arrêt commercial trouvé avec les nouvelles informations fournies, et enregistre
     * les modifications dans la base de données.
     *
     * @param stopPlaces La liste des arrêts commerciaux dont les informations d'accessibilité doivent être mises à jour.
     * @return La liste des arrêts commerciaux effectivement mis à jour et sauvegardés dans la base de données.
     */
    public List<StopPlace> updateAccessibilityStopPlaces(List<StopPlace> stopPlaces){
        List<StopPlace> stopPlaceList = new ArrayList<>();
        for(StopPlace stopPlaceToSave: stopPlaces){
            try {
                List<StopPlace> inBDD = stopPlaceRepository.findByNetexId(stopPlaceToSave.getNetexId());

                for(StopPlace stopPlace: inBDD) {
                    AccessibilityLimitation newAccessibilityLimitation = newAccessibilityLimitation(null, stopPlaceToSave);
                    AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
                    accessibilityAssessment.setMobilityImpairedAccess(stopPlaceToSave.getAccessibilityAssessment().getMobilityImpairedAccess());
                    accessibilityAssessment.setLimitations(List.of(newAccessibilityLimitation));
                    stopPlace.setAccessibilityAssessment(accessibilityAssessment);

                    try {
                        stopPlaceRepository.save(stopPlace);
                        stopPlaceList.add(stopPlace);
                    } catch (Exception e) {
                        logger.warn("Cannot update stop place with netexId : " + stopPlace.getNetexId(), e);
                    }
                }
            } catch (Exception e) {
                logger.warn("Cannot find in BDD stop place with netexId : " + stopPlaceToSave.getNetexId(), e);
            }
        }
        return stopPlaceList;
    }

    /**
     * Identifie les arrêts commerciaux et les quais associés dans la base de données, puis met à jour
     * leurs évaluations d'accessibilité en fonction des données fournies. Cette méthode utilise
     * les relations existantes entre les arrêts commerciaux et les quais pour appliquer les mises à jour
     * nécessaires sur les évaluations d'accessibilité de chaque lieu d'arrêt.
     *
     * @param quays La liste des quais avec des informations d'accessibilité mises à jour.
     */
    public void findAndUpdateAccessibilityStopPlacesToQuays(List<Quay> quays) {
        Map<StopPlace, List<Quay>> stopPlacesToQuays = stopPlaceRepository.findStopPlacesToQuays(quays);
        updateAccessibilityStopPlacesAccessibilityQuay(stopPlacesToQuays);
    }

    /**
     * Identifie les quais et les arrêts commerciaux associés dans la base de données, puis met à jour
     * leurs évaluations d'accessibilité en fonction des données fournies. Cette méthode utilise
     * les relations existantes entre les quais et les arrêts commerciaux pour appliquer les mises à jour
     * nécessaires sur les évaluations d'accessibilité de chaque quai.
     *
     * @param stopPlaces La liste des arrêts commerciaux avec des informations d'accessibilité mises à jour.
     */
    public void findAndUpdateAccessibilityQuaysToStopPlaces(List<StopPlace> stopPlaces) {
        Map<Quay, List<StopPlace>> quaysToStopPlaces = stopPlaceRepository.findQuaysToStopPlaces(stopPlaces);
        updateAccessibilityStopPlacesAccessibilityStopPlace(quaysToStopPlaces);
    }

    /**
     * Compare deux valeurs d'évaluation d'accessibilité et détermine le résultat de la comparaison
     * en fonction des règles métiers définies. Cette méthode est utilisée pour résoudre les conflits
     * lors de la fusion des données d'accessibilité provenant de sources multiples.
     *
     * @param value1 La première valeur d'évaluation à comparer.
     * @param value2 La deuxième valeur d'évaluation à comparer.
     * @return Le résultat de la comparaison en tant que {@link LimitationStatusEnumeration}.
     */
    private LimitationStatusEnumeration compareAccessibility(LimitationStatusEnumeration value1, LimitationStatusEnumeration value2) {
        if (LimitationStatusEnumeration.TRUE.equals(value1)) {
            if (LimitationStatusEnumeration.TRUE.equals(value2)) return LimitationStatusEnumeration.TRUE;
            else if (LimitationStatusEnumeration.FALSE.equals(value2)) return LimitationStatusEnumeration.PARTIAL;
            else if (LimitationStatusEnumeration.PARTIAL.equals(value2)) return LimitationStatusEnumeration.PARTIAL;
            else if (LimitationStatusEnumeration.UNKNOWN.equals(value2)) return LimitationStatusEnumeration.PARTIAL;
        }

        else if (LimitationStatusEnumeration.FALSE.equals(value1)) {
            if (LimitationStatusEnumeration.TRUE.equals(value2)) return LimitationStatusEnumeration.PARTIAL;
            else if (LimitationStatusEnumeration.FALSE.equals(value2)) return LimitationStatusEnumeration.FALSE;
            else if (LimitationStatusEnumeration.PARTIAL.equals(value2)) return LimitationStatusEnumeration.PARTIAL;
            else if (LimitationStatusEnumeration.UNKNOWN.equals(value2)) return LimitationStatusEnumeration.UNKNOWN;
        }

        else if (LimitationStatusEnumeration.PARTIAL.equals(value1)) {
            if (LimitationStatusEnumeration.TRUE.equals(value2)) return LimitationStatusEnumeration.PARTIAL;
            else if (LimitationStatusEnumeration.FALSE.equals(value2)) return LimitationStatusEnumeration.PARTIAL;
            else if (LimitationStatusEnumeration.PARTIAL.equals(value2)) return LimitationStatusEnumeration.PARTIAL;
            else if (LimitationStatusEnumeration.UNKNOWN.equals(value2)) return LimitationStatusEnumeration.UNKNOWN;

        }

        else if (LimitationStatusEnumeration.UNKNOWN.equals(value1)) {
            if (LimitationStatusEnumeration.TRUE.equals(value2)) return LimitationStatusEnumeration.PARTIAL;
            else if (LimitationStatusEnumeration.FALSE.equals(value2)) return LimitationStatusEnumeration.UNKNOWN;
            else if (LimitationStatusEnumeration.PARTIAL.equals(value2)) return LimitationStatusEnumeration.PARTIAL;
            else if (LimitationStatusEnumeration.UNKNOWN.equals(value2)) return LimitationStatusEnumeration.UNKNOWN;
        }

        return LimitationStatusEnumeration.UNKNOWN;
    }

    /**
     * Met à jour les informations d'accessibilité pour une liste de lieux d'arrêt en fonction
     * des associations fournies entre les arrêts commerciaux et les quais. Cette méthode applique
     * les mises à jour d'accessibilité sur chaque arrêt commercial en consolidant les données
     * d'accessibilité des quais associés.
     *
     * @param stopPlaceWithQuays Une carte associant chaque arrêt commercial à la liste de ses quais.
     */
    public void updateAccessibilityStopPlacesAccessibilityQuay(Map<StopPlace, List<Quay>> stopPlaceWithQuays) {
        stopPlaceWithQuays.forEach((stopPlace, quays) -> {
            AccessibilityAssessment stopPlaceAccessibility = new AccessibilityAssessment();
            // Pour simplifier, nous utilisons le premier quai comme référence initiale
            AccessibilityLimitation stopPlaceLimitation = newAccessibilityLimitation(quays.get(0), null);

            if (quays.size() > 1) {
                // Comparer les limitations des quais suivants et mise à jour des valeurs
                for (int i = 1; i < quays.size(); i++) {
                    AccessibilityLimitation currentQuayLimitation = newAccessibilityLimitation(quays.get(i), null);
                    setLimitationValues(stopPlaceLimitation, currentQuayLimitation);
                }
            }
            else {
                stopPlaceLimitation.setWheelchairAccess(stopPlaceLimitation.getWheelchairAccess());
                stopPlaceLimitation.setAudibleSignalsAvailable(stopPlaceLimitation.getAudibleSignalsAvailable());
                stopPlaceLimitation.setEscalatorFreeAccess(stopPlaceLimitation.getEscalatorFreeAccess());
                stopPlaceLimitation.setLiftFreeAccess(stopPlaceLimitation.getLiftFreeAccess());
                stopPlaceLimitation.setStepFreeAccess(stopPlaceLimitation.getStepFreeAccess());
                stopPlaceLimitation.setVisualSignsAvailable(stopPlaceLimitation.getVisualSignsAvailable());
            }

            stopPlaceAccessibility.setLimitations(List.of(stopPlaceLimitation));
            stopPlace.setAccessibilityAssessment(stopPlaceAccessibility);

            try {
                stopPlaceRepository.save(stopPlace);
            }
            catch (Exception e) {
                logger.warn("Cannot update stop place with netexId : " + stopPlace.getNetexId(), e);
            }
        });
    }

    /**
     * Met à jour les informations d'accessibilité pour une liste de quais en fonction
     * des associations fournies entre les quais et les arrêts commerciaux. Cette méthode applique
     * les mises à jour d'accessibilité sur chaque quai en consolidant les données
     * d'accessibilité des arrêts commerciaux associés.
     *
     * @param quayListMap Une carte associant chaque quai à la liste de ses arrêts commerciaux.
     */
    public void updateAccessibilityStopPlacesAccessibilityStopPlace(Map<Quay, List<StopPlace>> quayListMap) {
        quayListMap.forEach((quay, stopPlaces) -> {
            AccessibilityAssessment stopPlaceAccessibility = new AccessibilityAssessment();
            // Pour simplifier, nous utilisons le premier quai comme référence initiale
            AccessibilityLimitation stopPlaceLimitation = newAccessibilityLimitation(null, stopPlaces.get(0));

            if (stopPlaces.size() > 1) {
                // Comparer les limitations des quais suivants et mise à jour des valeurs
                for (int i = 1; i < stopPlaces.size(); i++) {
                    AccessibilityLimitation currentQuayLimitation = newAccessibilityLimitation(null, stopPlaces.get(i));
                    setLimitationValues(stopPlaceLimitation, currentQuayLimitation);
                }
            }
            else {
                stopPlaceLimitation.setWheelchairAccess(stopPlaceLimitation.getWheelchairAccess());
                stopPlaceLimitation.setAudibleSignalsAvailable(stopPlaceLimitation.getAudibleSignalsAvailable());
                stopPlaceLimitation.setEscalatorFreeAccess(stopPlaceLimitation.getEscalatorFreeAccess());
                stopPlaceLimitation.setLiftFreeAccess(stopPlaceLimitation.getLiftFreeAccess());
                stopPlaceLimitation.setStepFreeAccess(stopPlaceLimitation.getStepFreeAccess());
                stopPlaceLimitation.setVisualSignsAvailable(stopPlaceLimitation.getVisualSignsAvailable());
            }

            stopPlaceAccessibility.setLimitations(List.of(stopPlaceLimitation));
            quay.setAccessibilityAssessment(stopPlaceAccessibility);

            try {
                quayRepository.save(quay);
            }
            catch (Exception e) {
                logger.warn("Cannot update quay with netexId : " + quay.getNetexId(), e);
            }
        });
    }

    /**
     * Ajuste les valeurs d'accessibilité d'une évaluation en fonction d'une autre. Cette méthode
     * est utilisée pour fusionner les informations d'accessibilité en provenance de sources multiples,
     * en appliquant un ensemble de règles définies pour chaque type d'accessibilité.
     *
     * @param stopPlaceLimitation Les limitations d'accessibilité actuelles de l'arrêt commercial.
     * @param currentQuayLimitation Les limitations d'accessibilité à appliquer.
     */
    private void setLimitationValues(AccessibilityLimitation stopPlaceLimitation, AccessibilityLimitation currentQuayLimitation) {
        stopPlaceLimitation.setWheelchairAccess(compareAccessibility(stopPlaceLimitation.getWheelchairAccess(), currentQuayLimitation.getWheelchairAccess()));
        stopPlaceLimitation.setAudibleSignalsAvailable(compareAccessibility(stopPlaceLimitation.getAudibleSignalsAvailable(), currentQuayLimitation.getAudibleSignalsAvailable()));
        stopPlaceLimitation.setEscalatorFreeAccess(compareAccessibility(stopPlaceLimitation.getEscalatorFreeAccess(), currentQuayLimitation.getEscalatorFreeAccess()));
        stopPlaceLimitation.setLiftFreeAccess(compareAccessibility(stopPlaceLimitation.getLiftFreeAccess(), currentQuayLimitation.getLiftFreeAccess()));
        stopPlaceLimitation.setStepFreeAccess(compareAccessibility(stopPlaceLimitation.getStepFreeAccess(), currentQuayLimitation.getStepFreeAccess()));
        stopPlaceLimitation.setVisualSignsAvailable(compareAccessibility(stopPlaceLimitation.getVisualSignsAvailable(), currentQuayLimitation.getVisualSignsAvailable()));
    }
}
