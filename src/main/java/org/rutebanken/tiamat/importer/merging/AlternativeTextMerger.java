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

import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.tiamat.model.AlternativeText;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AlternativeTextMerger {

    public boolean updateAlternativeTexts(List<AlternativeText> existing, List<AlternativeText> incoming) {
        if (CollectionUtils.isEmpty(existing) && CollectionUtils.isEmpty(incoming)) {
            return false;
        }

        if (alternativeTextsEqual(existing, incoming)) {
            return false;
        }

        existing.clear();
        existing.addAll(incoming);
        return true;
    }

    private boolean alternativeTextsEqual(List<AlternativeText> existing, List<AlternativeText> incoming) {
        if (existing.size() != incoming.size()) {
            return false;
        }
        List<String> existingKeys = existing.stream().map(this::alternativeTextKey).sorted().collect(Collectors.toList());
        List<String> incomingKeys = incoming.stream().map(this::alternativeTextKey).sorted().collect(Collectors.toList());
        return existingKeys.equals(incomingKeys);
    }

    private String alternativeTextKey(AlternativeText alternativeText) {
        return alternativeText.getAttributeName() + "|"
                + alternativeText.getUseForLanguage() + "|"
                + (alternativeText.getText() != null ? alternativeText.getText().getValue() : null) + "|"
                + (alternativeText.getText() != null ? alternativeText.getText().getLang() : null);
    }
}
