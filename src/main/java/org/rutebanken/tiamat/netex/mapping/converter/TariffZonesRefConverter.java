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
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.TariffZoneRefs_RelStructure;
import org.rutebanken.netex.model.TariffZone_VersionStructure;
import org.rutebanken.tiamat.model.TariffZone;
import org.rutebanken.tiamat.model.TariffZoneRef;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Component
public class TariffZonesRefConverter extends BidirectionalConverter<Set<TariffZoneRef>, TariffZoneRefs_RelStructure> {
    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    @Override
    public TariffZoneRefs_RelStructure convertTo(Set<TariffZoneRef> tariffZones, Type<TariffZoneRefs_RelStructure> type, MappingContext mappingContext) {

        if(tariffZones == null || tariffZones.isEmpty()) {
            return null;
        }

        return new TariffZoneRefs_RelStructure()
                .withTariffZoneRef_(tariffZones.stream()
                    .map(tariffZoneRef -> netexObjectFactory.createTariffZoneRef(mapperFacade.map(tariffZoneRef, org.rutebanken.netex.model.TariffZoneRef.class)))
                        .collect(toList()));
    }

    @Override
    public Set<TariffZoneRef> convertFrom(TariffZoneRefs_RelStructure tariffZoneRefs_relStructure, Type<Set<TariffZoneRef>> type, MappingContext mappingContext) {
        if(tariffZoneRefs_relStructure == null
                || tariffZoneRefs_relStructure.getTariffZoneRef_() == null
                || tariffZoneRefs_relStructure.getTariffZoneRef_().isEmpty()) {
            return null;
        }

        return tariffZoneRefs_relStructure
                .getTariffZoneRef_()
                .stream()
                .map(tariffZoneRef -> mapperFacade.map(tariffZoneRef.getValue(), TariffZoneRef.class))
                .collect(toSet());

    }
}

