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

package org.rutebanken.tiamat.importer.handler;

import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.importer.ImportType;
import org.rutebanken.tiamat.importer.TariffZoneImporter;
import org.rutebanken.tiamat.model.Value;
import org.rutebanken.tiamat.netex.mapping.NetexMapper;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper;
import org.rutebanken.tiamat.repository.TariffZoneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class TariffZoneImportHandler {

    private static final Logger logger = LoggerFactory.getLogger(TariffZoneImportHandler.class);

    private final PublicationDeliveryHelper publicationDeliveryHelper;

    private final NetexMapper netexMapper;

    private final TariffZoneImporter tariffZoneImporter;

    @Autowired
    private TariffZoneRepository tariffZoneRepository;

    private ObjectFactory netexObectFactory = new ObjectFactory();

    public TariffZoneImportHandler(PublicationDeliveryHelper publicationDeliveryHelper, NetexMapper netexMapper, TariffZoneImporter tariffZoneImporter) {
        this.publicationDeliveryHelper = publicationDeliveryHelper;
        this.netexMapper = netexMapper;
        this.tariffZoneImporter = tariffZoneImporter;
    }


    public void handleTariffZones(SiteFrame netexSiteFrame, ImportParams importParams, AtomicInteger tariffZoneImportedCounter, SiteFrame responseSiteframe) {

        List<TariffZone> importedTariffZones = new ArrayList<>();

        List<StopPlace> tiamatStops = hasStopsTariffZones(netexSiteFrame);
        if (publicationDeliveryHelper.hasTariffZones(netexSiteFrame) && importParams.importType != ImportType.ID_MATCH) {
            List<org.rutebanken.tiamat.model.TariffZone> tiamatTariffZones = netexMapper.getFacade().mapAsList(netexSiteFrame.getTariffZones().getTariffZone_(), org.rutebanken.tiamat.model.TariffZone.class);
            logger.debug("Mapped {} tariff zones from netex to internal model", tiamatTariffZones.size());
            importedTariffZones.addAll(tariffZoneImporter.importTariffZones(tiamatTariffZones));
            logger.debug("Got {} imported tariffZones ", importedTariffZones.size());
        }
        if (!tiamatStops.isEmpty() && importParams.importType != ImportType.ID_MATCH) {
            List<org.rutebanken.tiamat.model.TariffZone> tiamatTariffZones = new ArrayList<>();
            Set<String> addedKeys = new HashSet<>();
            tiamatStops.forEach(stopPlace -> {
                stopPlace.getTariffZones().getTariffZoneRef_().forEach( tariffZoneRef -> {
                    String key = NetexIdMapper.FARE_ZONE;
                    String value = tariffZoneRef.getValue().getRef();
                    if (!addedKeys.contains(key + value) && tariffZoneRepository.findFirstByKeyValue(key, value) == null){
                        org.rutebanken.tiamat.model.TariffZone tariffZone = new org.rutebanken.tiamat.model.TariffZone();
                        tariffZone.getKeyValues().put(key, new Value(value));
                        tariffZone.setVersion(1);
                        tiamatTariffZones.add(tariffZone);
                        addedKeys.add(key + value);
                    }
                });
            });
            importedTariffZones.addAll(tariffZoneImporter.importTariffZones(tiamatTariffZones));
            logger.debug("Got {} created tariffZones ", importedTariffZones.size());
        }
        if (!importedTariffZones.isEmpty()) {
            List<JAXBElement<? extends Zone_VersionStructure>> newTariffZone = importedTariffZones.stream()
                    .map(tariffZone -> netexObectFactory.createTariffZone(tariffZone))
                    .collect(Collectors.toList());

            responseSiteframe.withTariffZones(netexObectFactory.createTariffZonesInFrame_RelStructure().withTariffZone_(newTariffZone));
        }
    }

    private List<StopPlace> hasStopsTariffZones(SiteFrame netexSiteFrame) {
        //We create TariffZone if there is zone id in StopPlace while there is no TariffZone in GTFS
        if (publicationDeliveryHelper.hasStops(netexSiteFrame)){
            List<StopPlace> netexStops = netexSiteFrame.getStopPlaces().getStopPlace_().stream()
                    .map(JAXBElement::getValue)
                    .map(sp -> (StopPlace)sp)
                    .collect(Collectors.toList());

            return netexStops.stream().filter(stopPlace -> stopPlace.getTariffZones() != null && stopPlace.getTariffZones().getTariffZoneRef_() != null
                    && !stopPlace.getTariffZones().getTariffZoneRef_().isEmpty())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
