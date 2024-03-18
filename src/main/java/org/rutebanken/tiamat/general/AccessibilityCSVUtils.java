package org.rutebanken.tiamat.general;

import org.rutebanken.tiamat.model.*;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityCSVUtils {

    /**
     * Tronque une chaîne de caractères si elle dépasse une longueur spécifiée.
     *
     * @param value La chaîne de caractères à évaluer.
     * @return La chaîne tronquée si elle dépasse 200 caractères, sinon la chaîne originale.
     */
    static String truncateIfNecessary(String value) {
        return value.length() > 200 ? value.substring(0, 200) : value;
    }

    /**
     * Identifie et retourne les éléments en double dans une liste.
     *
     * Cette méthode parcourt une liste de chaînes de caractères et identifie
     * les éléments qui apparaissent plus d'une fois.
     *
     * @param fullList La liste à vérifier pour la duplication.
     * @return Une liste contenant les éléments en double. La liste sera vide
     *         si aucun doublon n'est trouvé.
     */
    static List<String> foundDuplicates(List<String> fullList){
        List<String> alreadyReadList = new ArrayList<>();
        List<String> duplicateList = new ArrayList<>();

        fullList
                .forEach(id -> {
                    if (alreadyReadList.contains(id)){
                        duplicateList.add(id);
                    }else{
                        alreadyReadList.add(id);
                    }
                });

        return duplicateList;
    }

    /**
     * Convertit une chaîne de caractères représentant l'état d'accessibilité
     * en une énumération {@link LimitationStatusEnumeration}.
     *
     * La conversion se base sur des valeurs prédéfinies ("OUI", "NON", "PARTIEL").
     *
     * @param valueToConvert La chaîne de caractères représentant l'état d'accessibilité.
     * @return L'énumération {@link LimitationStatusEnumeration} correspondante.
     */
    static LimitationStatusEnumeration getValue(String valueToConvert) {
        return switch (valueToConvert.toUpperCase()) {
            case "OUI" -> LimitationStatusEnumeration.TRUE;
            case "NON" -> LimitationStatusEnumeration.FALSE;
            case "PARTIEL" -> LimitationStatusEnumeration.PARTIAL;
            default -> LimitationStatusEnumeration.UNKNOWN;
        };
    }
}
