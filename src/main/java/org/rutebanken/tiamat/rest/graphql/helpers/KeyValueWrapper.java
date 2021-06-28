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

package org.rutebanken.tiamat.rest.graphql.helpers;

import org.rutebanken.tiamat.model.Value;

import java.util.Map;
import java.util.Set;

public class KeyValueWrapper {
    public String key;
    public Set<String> values;

    public KeyValueWrapper(String key, Value value) {
        this.key = key;
        if (value != null) {
            this.values = value.getItems();
        }
    }

    /**
     * Extrait le code de reference d'apres la Map de cle/valeur de la topo
     * @param rKeyV : La Map
     * @param rDefaultIsNull : Valeur a affecter si null
     * @return le code de reference ou null si pas trouve
     */
    public static String extractCodeFromKeyValues(Map<String, Value> rKeyV, String rDefaultIsNull) {
        String code = extractCodeFromKeyValues(rKeyV);
        return (code == null) ? rDefaultIsNull : code;
    }

    /**
     * Extrait le code de reference d'apres la Map de cle/valeur de la topo
     * @param rKeyV : La Map
     * @return le code de reference ou null si pas trouve
     */
    public static String extractCodeFromKeyValues(Map<String, Value> rKeyV) {
        //Tente d'extraire l'identifiant du point d'arret (le parent des quais)
        if (rKeyV != null) {
            Value lReference = rKeyV.get("imported-id");
            if (lReference != null) {
                String lDataRef = lReference.getItems().stream().findFirst().orElse(null);
                //Si on a effectivement trouve l'element
                if (lDataRef != null) {
                    return lDataRef.substring(lDataRef.lastIndexOf(':')+1);
                }
            } else { return null; }
        }
        return null;
    }

}