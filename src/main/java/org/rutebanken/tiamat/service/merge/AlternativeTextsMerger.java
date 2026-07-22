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

package org.rutebanken.tiamat.service.merge;

import org.apache.commons.collections4.CollectionUtils;
import org.rutebanken.tiamat.model.AlternativeText;
import org.rutebanken.tiamat.service.ObjectMerger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlternativeTextsMerger {

    public void mergeAlternativeTexts(List<AlternativeText> fromAlternativeTexts, List<AlternativeText> toAlternativeTexts) {
        if (CollectionUtils.isNotEmpty(fromAlternativeTexts)) {
            fromAlternativeTexts.forEach(altText -> {
                AlternativeText mergedAltText = new AlternativeText();
                ObjectMerger.copyPropertiesNotNull(altText, mergedAltText);
                mergedAltText.setVersion(mergedAltText.getVersion() + 2);
                toAlternativeTexts.add(mergedAltText);
            });
        }
    }
}
