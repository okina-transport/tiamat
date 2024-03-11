package org.rutebanken.tiamat.general;

import org.apache.commons.csv.CSVRecord;
import org.rutebanken.tiamat.model.*;
import org.rutebanken.tiamat.rest.dto.DtoAccessibilityQuay;
import org.rutebanken.tiamat.service.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AccessibilityCSVHelperQuay {

    /**
     * Analyse un fichier CSV pour créer une liste d'objets {@link DtoAccessibilityQuay}.
     * Chaque enregistrement du fichier CSV est converti en un objet {@code DtoAccessibilityQuay}.
     * Les champs obligatoires de chaque DTO sont validés avant d'ajouter l'objet à la liste.
     *
     * @param csvFile le flux d'entrée représentant le fichier CSV à analyser.
     *                Ne doit pas être {@code null}.
     * @return une liste de {@link DtoAccessibilityQuay} représentant les données d'accessibilité des quais.
     * @throws IOException si une erreur d'entrée/sortie se produit lors de la lecture du fichier CSV.
     *                     Cela peut inclure des erreurs de format de fichier ou des problèmes d'accès au fichier.
     */
    public static List<DtoAccessibilityQuay> parseDocument(InputStream csvFile) throws IOException {
        List<DtoAccessibilityQuay> dtoAccessibilityQuaisList = new ArrayList<>();
        Iterable<CSVRecord> records = CSVHelper.getRecords(csvFile);

        for (CSVRecord csvRecord : records) {
            DtoAccessibilityQuay dtoAccessibilityQuay = createDtoFromCsvRecord(csvRecord);
            validateRequiredFields(dtoAccessibilityQuay);
            dtoAccessibilityQuaisList.add(dtoAccessibilityQuay);
        }

        return dtoAccessibilityQuaisList;
    }

    /**
     * Crée un objet {@link DtoAccessibilityQuay} à partir d'un enregistrement CSV.
     * Cette méthode extrait les données de chaque champ de l'enregistrement CSV
     * pour construire un nouvel objet {@code DtoAccessibilityQuay}.
     * Les champs susceptibles de dépasser une certaine longueur sont tronqués
     * pour respecter les contraintes de taille lors du traitement ultérieur.
     *
     * @param csvRecord l'enregistrement CSV à partir duquel l'objet DTO est construit.
     *                  Ne doit pas être {@code null}.
     * @return un nouvel objet {@link DtoAccessibilityQuay} peuplé avec les données de l'enregistrement CSV.
     *         Les champs longs sont tronqués selon la nécessité.
     */
    private static DtoAccessibilityQuay createDtoFromCsvRecord(CSVRecord csvRecord) {
        return new DtoAccessibilityQuay(
                csvRecord.get(0),
                csvRecord.get(1),
                csvRecord.get(2),
                csvRecord.get(3),
                AccessibilityCSVUtils.truncateIfNecessary(csvRecord.get(4)),
                csvRecord.get(5),
                AccessibilityCSVUtils.truncateIfNecessary(csvRecord.get(6)),
                csvRecord.get(7),
                csvRecord.get(8),
                csvRecord.get(9),
                csvRecord.get(10),
                csvRecord.get(11),
                csvRecord.get(12),
                csvRecord.get(13),
                csvRecord.get(14),
                csvRecord.get(15)
        );
    }

    /**
     * Vérifie que les champs obligatoires d'un {@link DtoAccessibilityQuay} sont bien renseignés.
     * Les champs obligatoires sont l'identifiant (ID) et le nom du quai. Cette méthode lance une
     * {@link IllegalArgumentException} si l'un de ces champs est vide ou ne contient que des espaces blancs.
     *
     * @param dtoAccessibilityQuay le DTO du quai à valider. Ne doit pas être {@code null}.
     * @throws IllegalArgumentException si l'ID ou le nom est vide.
     */
    private static void validateRequiredFields(DtoAccessibilityQuay dtoAccessibilityQuay) {
        Preconditions.checkArgument(StringUtils.isNotBlank(dtoAccessibilityQuay.getId()), "ID is required");
        Preconditions.checkArgument(StringUtils.isNotBlank(dtoAccessibilityQuay.getName()), "Name is required for ID " + dtoAccessibilityQuay.getId());
    }

    /**
     * Vérifie l'unicité des quais dans une liste de {@link DtoAccessibilityQuay} en se basant sur
     * une clé composite formée de l'ID et du nom. Lance une {@link IllegalArgumentException} si des doublons
     * sont trouvés.
     *
     * @param dtoAccessibilityQuaisList la liste des DTO des quais à vérifier.
     * @throws IllegalArgumentException si des doublons sont trouvés dans la liste.
     */
    public static void checkDuplicatedQuays(List<DtoAccessibilityQuay> dtoAccessibilityQuaisList) throws IllegalArgumentException {
        List<String> compositeKey = dtoAccessibilityQuaisList.stream().map(accessibilityQuai -> accessibilityQuai.getId() + accessibilityQuai.getName()).collect(Collectors.toList());
        List<String> duplicates = AccessibilityCSVUtils.foundDuplicates(compositeKey);

        if (duplicates.size() > 0){
            String duplicatesMsg = duplicates.stream()
                    .collect(Collectors.joining(","));

            throw new IllegalArgumentException("There are duplicated quai in your CSV File 'With the same ID & Name'. Duplicates:" + duplicatesMsg);
        }

    }

    /**
     * Convertit une liste de {@link DtoAccessibilityQuay} en une liste d'entités {@link Quay}.
     * Cette conversion inclut la création d'une évaluation d'accessibilité ({@link AccessibilityAssessment})
     * et des limitations d'accessibilité ({@link AccessibilityLimitation}) pour chaque quai,
     * en se basant sur les données fournies dans le DTO.
     *
     * @param dtoAccessibilityQuays la liste des DTO des quais à convertir.
     * @return une liste d'entités {@link Quay} peuplée avec les données fournies.
     */
    public static List<Quay> mapFromDtoToEntity(List<DtoAccessibilityQuay> dtoAccessibilityQuays) {
        return dtoAccessibilityQuays.stream().map(dtoQuay -> {
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
