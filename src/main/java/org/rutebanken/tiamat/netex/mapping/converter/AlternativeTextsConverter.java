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

package org.rutebanken.tiamat.netex.mapping.converter;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.netex.model.AlternativeTexts_RelStructure;
import org.rutebanken.tiamat.model.AlternativeText;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AlternativeTextsConverter extends BidirectionalConverter<List<AlternativeText>, AlternativeTexts_RelStructure> {

    @Override
    public AlternativeTexts_RelStructure convertTo(List<AlternativeText> alternativeTexts, Type<AlternativeTexts_RelStructure> type, MappingContext mappingContext) {

        if (CollectionUtils.isNotEmpty(alternativeTexts)) {

            List<org.rutebanken.netex.model.AlternativeText> netexAlternativeTexts = new ArrayList<>();

            for (org.rutebanken.tiamat.model.AlternativeText alternativeText : alternativeTexts) {
                if (alternativeText != null
                        && alternativeText.getText() != null
                        && StringUtils.isNotBlank(alternativeText.getText().getValue())) {
                    //Only include non-empty alternative texts
                    org.rutebanken.netex.model.AlternativeText netexAlternativeText = new org.rutebanken.netex.model.AlternativeText();
                    mapperFacade.map(alternativeText, netexAlternativeText);
                    netexAlternativeText.setId(alternativeText.getNetexId());
                    netexAlternativeTexts.add(netexAlternativeText);
                }
            }

            if (CollectionUtils.isNotEmpty(netexAlternativeTexts)) {
                AlternativeTexts_RelStructure alternativeTextsRelStructure = new AlternativeTexts_RelStructure();
                alternativeTextsRelStructure.getAlternativeText().addAll(netexAlternativeTexts);
                return alternativeTextsRelStructure;
            }
        }
        return null;
    }

    @Override
    public List<AlternativeText> convertFrom(AlternativeTexts_RelStructure alternativeTextsRelStructure, Type<List<AlternativeText>> type, MappingContext mappingContext) {
        return List.of();
    }
}
