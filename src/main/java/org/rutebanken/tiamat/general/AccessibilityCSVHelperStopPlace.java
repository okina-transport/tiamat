package org.rutebanken.tiamat.general;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.rest.dto.DtoAccessibilityStopPlace;
import org.rutebanken.tiamat.service.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AccessibilityCSVHelperStopPlace {

    /**
     * Analyse un fichier CSV pour créer une liste d'objets {@link DtoAccessibilityStopPlace}.
     * Cette méthode lit chaque enregistrement du fichier CSV, crée un objet DTO correspondant,
     * valide les champs obligatoires, puis ajoute le DTO à la liste de retour.
     *
     * @param csvFile Le flux d'entrée représentant le fichier CSV à analyser.
     * @return Une liste d'objets {@code DtoAccessibilityStopPlace} correspondant aux enregistrements du fichier CSV.
     * @throws IOException Si une erreur d'entrée/sortie se produit lors de la lecture du fichier.
     */
    public static List<DtoAccessibilityStopPlace> parseDocument(InputStream csvFile) throws IOException {
        List<DtoAccessibilityStopPlace> dtoAccessibilityQuaisList = new ArrayList<>();
        Iterable<CSVRecord> records = CSVHelper.getRecords(csvFile);

        for (CSVRecord csvRecord : records) {
            DtoAccessibilityStopPlace dtoAccessibilityQuay = createDtoFromCsvRecord(csvRecord);
            validateRequiredFields(dtoAccessibilityQuay);
            dtoAccessibilityQuaisList.add(dtoAccessibilityQuay);

        }

        return dtoAccessibilityQuaisList;
    }

    /**
     * Crée un objet {@link DtoAccessibilityStopPlace} à partir d'un enregistrement CSV.
     * Cette méthode extrait les données de chaque champ de l'enregistrement CSV et
     * applique une éventuelle troncature sur certains champs spécifiques.
     *
     * @param csvRecord L'enregistrement CSV à partir duquel l'objet DTO est construit.
     * @return Un objet {@code DtoAccessibilityStopPlace} peuplé avec les données extraites de l'enregistrement CSV.
     */
    private static DtoAccessibilityStopPlace createDtoFromCsvRecord(CSVRecord csvRecord) {
        return new DtoAccessibilityStopPlace(
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
     * Vérifie que les champs obligatoires d'un {@link DtoAccessibilityStopPlace} sont bien renseignés.
     * Lève une {@link IllegalArgumentException} si l'un des champs obligatoires (ID ou nom) est vide.
     *
     * @param dtoAccessibilityQuay Le DTO à valider.
     * @throws IllegalArgumentException Si l'ID ou le nom du quai est vide.
     */
    private static void validateRequiredFields(DtoAccessibilityStopPlace dtoAccessibilityQuay) {
        Preconditions.checkArgument(StringUtils.isNotBlank(dtoAccessibilityQuay.getId()), "ID is required");
        Preconditions.checkArgument(StringUtils.isNotBlank(dtoAccessibilityQuay.getName()), "Name is required for ID " + dtoAccessibilityQuay.getId());
    }

    /**
     * Vérifie l'absence de doublons dans une liste de {@link DtoAccessibilityStopPlace} en fonction
     * de leur ID et nom. Lève une {@link IllegalArgumentException} s'il existe des doublons.
     *
     * @param dtoAccessibilityQuaisList La liste des DTO à vérifier.
     * @throws IllegalArgumentException Si des doublons sont trouvés.
     */
    public static void checkDuplicatedQuays(List<DtoAccessibilityStopPlace> dtoAccessibilityQuaisList) throws IllegalArgumentException {
        List<String> compositeKey = dtoAccessibilityQuaisList.stream().map(accessibilityQuai -> accessibilityQuai.getId() + accessibilityQuai.getName()).collect(Collectors.toList());
        List<String> duplicates = AccessibilityCSVUtils.foundDuplicates(compositeKey);

        if (duplicates.size() > 0){
            String duplicatesMsg = duplicates.stream()
                    .collect(Collectors.joining(","));

            throw new IllegalArgumentException("There are duplicated quai in your CSV File 'With the same ID & Name'. Duplicates:" + duplicatesMsg);
        }

    }

    /**
     * Convertit une liste de {@link DtoAccessibilityStopPlace} en une liste d'entités {@link StopPlace}.
     * Cette conversion inclut la création d'une évaluation d'accessibilité ({@link AccessibilityAssessment})
     * et la définition des limitations d'accessibilité pour chaque arrêt.
     *
     * @param dtoAccessibilityQuays La liste des DTO des arrêts à convertir.
     * @return Une liste d'entités {@code StopPlace} peuplée avec les données fournies.
     */
    public static List<StopPlace> mapFromDtoToEntity(List<DtoAccessibilityStopPlace> dtoAccessibilityQuays) {
        return dtoAccessibilityQuays.stream().map(dtoAccessibilityStopPlace -> {
            StopPlace stopPlace = new StopPlace();
            stopPlace.setNetexId(dtoAccessibilityStopPlace.getId());

            AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
            AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
            ArrayList<AccessibilityLimitation> accessibilityLimitations = new ArrayList<>();
            accessibilityLimitations.add(accessibilityLimitation);
            accessibilityLimitation.setWheelchairAccess(AccessibilityCSVUtils.getValue(dtoAccessibilityStopPlace.getWheelchairAccess()));
            accessibilityLimitation.setAudibleSignalsAvailable(AccessibilityCSVUtils.getValue(dtoAccessibilityStopPlace.getAudibleSignalsAvailable()));
            accessibilityLimitation.setStepFreeAccess(AccessibilityCSVUtils.getValue(dtoAccessibilityStopPlace.getStepFreeAccess()));
            accessibilityLimitation.setEscalatorFreeAccess(AccessibilityCSVUtils.getValue(dtoAccessibilityStopPlace.getEscalatorFreeAccess()));
            accessibilityLimitation.setLiftFreeAccess(AccessibilityCSVUtils.getValue(dtoAccessibilityStopPlace.getLiftFreeAccess()));
            accessibilityLimitation.setVisualSignsAvailable(AccessibilityCSVUtils.getValue(dtoAccessibilityStopPlace.getVisualSignsAvailable()));

            accessibilityAssessment.setLimitations(accessibilityLimitations);
            stopPlace.setAccessibilityAssessment(accessibilityAssessment);
            return stopPlace;
        }).collect(Collectors.toList());
    }
}
