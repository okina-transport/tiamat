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

package org.rutebanken.tiamat.exporter;

import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.TariffZone;
import org.rutebanken.netex.model.TariffZonesInFrame_RelStructure;
import org.rutebanken.netex.model.Zone_VersionStructure;
import org.rutebanken.tiamat.model.TariffZoneRef;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.repository.reference.ReferenceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Transactional
public class TariffZonesFromStopsExporter {

    private static final Logger logger = LoggerFactory.getLogger(TariffZonesFromStopsExporter.class);

    @Autowired
    private NetexMapper netexMapper;

    @Autowired
    private ReferenceResolver referenceResolver;

    private QName qname = new QName("http://www.netex.org.uk/netex", "TariffZone_");

    /**
     * Resolve and add relevant tariffzones to the response site frame.
     * If a tariffzone already exists in the response, it's not added twice.
     *
     * @param importedNetexStopPlaces stop places that were imported, that could reference to tariff zones
     * @param responseSiteFrame the site fra to append or add tariff zones to
     */
    public void resolveTariffZones(Collection<StopPlace> importedNetexStopPlaces, SiteFrame responseSiteFrame) {

        Map<String, TariffZone> tariffZoneMap = new HashMap<>();

        if(responseSiteFrame.getTariffZones() != null && responseSiteFrame.getTariffZones().getTariffZone_() != null) {


            List<org.rutebanken.netex.model.TariffZone> tarifZones = responseSiteFrame.getTariffZones().getTariffZone_()
                                                                                    .stream()
                                                                                    .map(jaxbElement -> (org.rutebanken.netex.model.TariffZone) jaxbElement.getValue())
                                                                                    .collect(Collectors.toList());

            tarifZones
                    .forEach(tariffZone -> tariffZoneMap.put(key(tariffZone.getId(), tariffZone.getVersion()), tariffZone));
    }

        importedNetexStopPlaces.stream()
                .filter(stopPlace -> stopPlace.getTariffZones() != null)
                .flatMap(stopPlace -> stopPlace.getTariffZones().getTariffZoneRef_().stream())
                .filter(tariffZoneRef -> !tariffZoneMap.containsKey(key(tariffZoneRef.getValue().getRef(), tariffZoneRef.getValue().getVersion())))
                .map(tariffZoneRef -> netexMapper.getFacade().map(tariffZoneRef.getValue(), TariffZoneRef.class))
                .peek(mappedTariffZoneRef -> logger.debug("Resolving ref: {}", mappedTariffZoneRef))
                .map(mappedTariffZoneRef -> {
                    org.rutebanken.tiamat.model.TariffZone tiamatTariffZone = referenceResolver.resolve(mappedTariffZoneRef);
                    if(tiamatTariffZone == null) {
                        logger.warn("Resolved tariff zone to null from reference: {}", mappedTariffZoneRef);
                    }
                    return tiamatTariffZone;
                })
                .filter(Objects::nonNull)
                .peek(tiamatTariffZone -> logger.debug("Resolved tariffZone: {}", tiamatTariffZone))
                .map(tiamatTariffZone -> netexMapper.getFacade().map(tiamatTariffZone, TariffZone.class))
                .forEach(tariffZone -> tariffZoneMap.put(key(tariffZone.getId(), tariffZone.getVersion()), tariffZone));

        if(tariffZoneMap.values().isEmpty()) {
            logger.info("No relevant tariff zones to return");
            responseSiteFrame.withTariffZones(null);
        } else {
            logger.info("Adding {} tariff zones", tariffZoneMap.values().size());
            List<JAXBElement<? extends Zone_VersionStructure>> jaxbTariffZone = tariffZoneMap.values().stream()
                                                                            .map(tariffZone -> new JAXBElement<TariffZone>(qname, TariffZone.class, tariffZone))
                                                                            .collect(Collectors.toList());

            responseSiteFrame.withTariffZones(new TariffZonesInFrame_RelStructure().withTariffZone_(jaxbTariffZone));
        }
    }

    private String key(String id, String version) {
        return id + "-" + version;
    }

}
