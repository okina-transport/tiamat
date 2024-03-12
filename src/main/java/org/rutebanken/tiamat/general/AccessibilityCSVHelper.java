package org.rutebanken.tiamat.general;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.rest.dto.DtoAccessibility;
import org.rutebanken.tiamat.service.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AccessibilityCSVHelper {

    /**
     * Analyse un fichier CSV pour créer une liste d'objets {@link DtoAccessibility}.
     * Cette méthode lit chaque enregistrement du fichier CSV, crée un objet DTO correspondant,
     * valide les champs obligatoires, puis ajoute le DTO à la liste de retour.
     *
     * @param csvFile Le flux d'entrée représentant le fichier CSV à analyser.
     * @return Une liste d'objets {@code DtoAccessibilityStopPlace} correspondant aux enregistrements du fichier CSV.
     * @throws IOException Si une erreur d'entrée/sortie se produit lors de la lecture du fichier.
     */
    public static List<DtoAccessibility> parseDocument(InputStream csvFile) throws IOException {
        List<DtoAccessibility> dtoAccessibilityList = new ArrayList<>();
        Iterable<CSVRecord> records = CSVHelper.getRecords(csvFile);

        for (CSVRecord csvRecord : records) {
            DtoAccessibility dtoAccessibility = createDtoFromCsvRecord(csvRecord);
            validateRequiredFields(dtoAccessibility);
            dtoAccessibilityList.add(dtoAccessibility);

        }

        return dtoAccessibilityList;
    }

    /**
     * Crée un objet {@link DtoAccessibility} à partir d'un enregistrement CSV.
     * Cette méthode extrait les données de chaque champ de l'enregistrement CSV et
     * applique une éventuelle troncature sur certains champs spécifiques.
     *
     * @param csvRecord L'enregistrement CSV à partir duquel l'objet DTO est construit.
     * @return Un objet {@code DtoAccessibilityStopPlace} peuplé avec les données extraites de l'enregistrement CSV.
     */
    private static DtoAccessibility createDtoFromCsvRecord(CSVRecord csvRecord) {
        return new DtoAccessibility(
                csvRecord.get(0),
                csvRecord.get(1),
                csvRecord.get(2),
                csvRecord.get(3),
                csvRecord.get(4),
                AccessibilityCSVUtils.truncateIfNecessary(csvRecord.get(5)),
                csvRecord.get(6),
                AccessibilityCSVUtils.truncateIfNecessary(csvRecord.get(7)),
                csvRecord.get(8),
                csvRecord.get(9),
                csvRecord.get(10),
                csvRecord.get(11),
                csvRecord.get(12),
                csvRecord.get(13),
                csvRecord.get(14),
                csvRecord.get(15),
                csvRecord.get(16)
        );
    }

    /**
     * Vérifie que les champs obligatoires d'un {@link DtoAccessibility} sont bien renseignés.
     * Lève une {@link IllegalArgumentException} si l'un des champs obligatoires (ID ou nom) est vide.
     *
     * @param dtoAccessibility Le DTO à valider.
     * @throws IllegalArgumentException Si l'ID ou le nom du quai est vide.
     */
    private static void validateRequiredFields(DtoAccessibility dtoAccessibility) {
        Preconditions.checkArgument(StringUtils.isNotBlank(dtoAccessibility.getId()), "ID is required");
        Preconditions.checkArgument(StringUtils.isNotBlank(dtoAccessibility.getName()), "Name is required for ID " + dtoAccessibility.getId());
    }

    /**
     * Vérifie l'absence de doublons dans une liste de {@link DtoAccessibility} en fonction
     * de leur ID et nom. Lève une {@link IllegalArgumentException} s'il existe des doublons.
     *
     * @param dtoAccessibilityList La liste des DTO à vérifier.
     * @throws IllegalArgumentException Si des doublons sont trouvés.
     */
    public static void checkDuplicatedQuays(List<DtoAccessibility> dtoAccessibilityList) throws IllegalArgumentException {
        List<String> compositeKey = dtoAccessibilityList.stream().map(accessibility -> accessibility.getId() + accessibility.getName()).collect(Collectors.toList());
        List<String> duplicates = AccessibilityCSVUtils.foundDuplicates(compositeKey);

        if (duplicates.size() > 0){
            String duplicatesMsg = duplicates.stream()
                    .collect(Collectors.joining(","));

            throw new IllegalArgumentException("There are duplicated quai in your CSV File 'With the same ID & Name'. Duplicates:" + duplicatesMsg);
        }

    }

    /**
     * Convertit une liste de {@link DtoAccessibility} en une liste d'entités {@link StopPlace}.
     * Cette conversion inclut la création d'une évaluation d'accessibilité ({@link AccessibilityAssessment})
     * et la définition des limitations d'accessibilité pour chaque arrêt.
     *
     * @param dtoAccessibilities La liste des DTO des arrêts à convertir.
     * @return Une liste d'entités {@code StopPlace} peuplée avec les données fournies.
     */
    public static List<StopPlace> mapFromDtoToStopPlaceEntity(List<DtoAccessibility> dtoAccessibilities) {
        return dtoAccessibilities.stream().map(dtoAccessibility -> {
            StopPlace stopPlace = new StopPlace();
            stopPlace.setNetexId(dtoAccessibility.getId());

            AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
            AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
            ArrayList<AccessibilityLimitation> accessibilityLimitations = new ArrayList<>();
            accessibilityLimitations.add(accessibilityLimitation);
            accessibilityLimitation.setWheelchairAccess(AccessibilityCSVUtils.getValue(dtoAccessibility.getWheelchairAccess()));
            accessibilityLimitation.setAudibleSignalsAvailable(AccessibilityCSVUtils.getValue(dtoAccessibility.getAudibleSignalsAvailable()));
            accessibilityLimitation.setStepFreeAccess(AccessibilityCSVUtils.getValue(dtoAccessibility.getStepFreeAccess()));
            accessibilityLimitation.setEscalatorFreeAccess(AccessibilityCSVUtils.getValue(dtoAccessibility.getEscalatorFreeAccess()));
            accessibilityLimitation.setLiftFreeAccess(AccessibilityCSVUtils.getValue(dtoAccessibility.getLiftFreeAccess()));
            accessibilityLimitation.setVisualSignsAvailable(AccessibilityCSVUtils.getValue(dtoAccessibility.getVisualSignsAvailable()));

            accessibilityAssessment.setLimitations(accessibilityLimitations);
            stopPlace.setAccessibilityAssessment(accessibilityAssessment);
            return stopPlace;
        }).collect(Collectors.toList());
    }

    /**
     * Convertit une liste de {@link DtoAccessibility} en une liste d'entités {@link Quay}.
     * Cette conversion inclut la création d'une évaluation d'accessibilité ({@link AccessibilityAssessment})
     * et des limitations d'accessibilité ({@link AccessibilityLimitation}) pour chaque quai,
     * en se basant sur les données fournies dans le DTO.
     *
     * @param dtoAccessibilities la liste des DTO des quais à convertir.
     * @return une liste d'entités {@link Quay} peuplée avec les données fournies.
     */
    public static List<Quay> mapFromDtoToQuayEntity(List<DtoAccessibility> dtoAccessibilities) {
        return dtoAccessibilities.stream().map(dtoQuay -> {
            Quay quay = new Quay();
            quay.setNetexId(dtoQuay.getId());

            AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
            AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
            ArrayList<AccessibilityLimitation> accessibilityLimitations = new ArrayList<>();
            accessibilityLimitations.add(accessibilityLimitation);
            accessibilityLimitation.setWheelchairAccess(AccessibilityCSVUtils.getValue(dtoQuay.getWheelchairAccess()));
            accessibilityLimitation.setAudibleSignalsAvailable(AccessibilityCSVUtils.getValue(dtoQuay.getAudibleSignalsAvailable()));
            accessibilityLimitation.setStepFreeAccess(AccessibilityCSVUtils.getValue(dtoQuay.getStepFreeAccess()));
            accessibilityLimitation.setEscalatorFreeAccess(AccessibilityCSVUtils.getValue(dtoQuay.getEscalatorFreeAccess()));
            accessibilityLimitation.setLiftFreeAccess(AccessibilityCSVUtils.getValue(dtoQuay.getLiftFreeAccess()));
            accessibilityLimitation.setVisualSignsAvailable(AccessibilityCSVUtils.getValue(dtoQuay.getVisualSignsAvailable()));

            accessibilityAssessment.setLimitations(accessibilityLimitations);
            quay.setAccessibilityAssessment(accessibilityAssessment);
            return quay;
        }).collect(Collectors.toList());
    }
}
