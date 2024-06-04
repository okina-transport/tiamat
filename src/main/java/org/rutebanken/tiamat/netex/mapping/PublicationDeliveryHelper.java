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

package org.rutebanken.tiamat.netex.mapping;

import org.rutebanken.netex.model.*;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.rutebanken.tiamat.netex.mapping.mapper.NetexIdMapper.ORIGINAL_ID_KEY;

@Component
public class PublicationDeliveryHelper {

    public boolean hasTopographicPlaces(SiteFrame netexSiteFrame) {
        return netexSiteFrame.getTopographicPlaces() != null
                && netexSiteFrame.getTopographicPlaces().getTopographicPlace() != null
                && !netexSiteFrame.getTopographicPlaces().getTopographicPlace().isEmpty();
    }

    public List<TopographicPlace> extractTopographicPlaces(SiteFrame siteFrame) {
        if(siteFrame.getTopographicPlaces() != null && siteFrame.getTopographicPlaces().getTopographicPlace() != null) {
            return siteFrame.getTopographicPlaces().getTopographicPlace();
        } else {
            return new ArrayList<>();
        }
    }

    public boolean hasStops(SiteFrame siteFrame) {
        return siteFrame.getStopPlaces() != null && siteFrame.getStopPlaces().getStopPlace_() != null;
    }

    public boolean hasTariffZones(SiteFrame netexSiteFrame) {
        return netexSiteFrame.getTariffZones() != null && netexSiteFrame.getTariffZones().getTariffZone_() != null;
    }

    public boolean hasPathLinks(SiteFrame netexSiteFrame) {
        return netexSiteFrame.getPathLinks() != null && netexSiteFrame.getPathLinks().getPathLink() != null;
    }

    public boolean hasParkings(SiteFrame siteFrame) {
        return siteFrame.getParkings() != null && siteFrame.getParkings().getParking() != null;
    }

    public boolean hasPointOfInterests(SiteFrame siteFrame) {
        return siteFrame.getPointsOfInterest() != null && siteFrame.getPointsOfInterest().getPointOfInterest() != null;
    }

    public int numberOfStops(SiteFrame netexSiteFrame) {
        return hasStops(netexSiteFrame) ? netexSiteFrame.getStopPlaces().getStopPlace_().size() : 0;
    }

    public GeneralFrame findGeneralFrame(PublicationDeliveryStructure incomingPublicationDelivery){

        List<JAXBElement<? extends Common_VersionFrameStructure>> compositeFrameOrCommonFrame = incomingPublicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame();

        Optional<GeneralFrame> optionalGeneralFrame = compositeFrameOrCommonFrame.stream()
                .filter(element -> element.getValue() instanceof GeneralFrame)
                .map(element -> (GeneralFrame)element.getValue())
                .findFirst();

        if(optionalGeneralFrame.isPresent()){
            return optionalGeneralFrame.get();
        }

        return compositeFrameOrCommonFrame
                .stream()
                .filter(element -> element.getValue() instanceof CompositeFrame)
                .map(element -> (CompositeFrame) element.getValue())
                .map(compositeFrame -> compositeFrame.getFrames())
                .flatMap(frames -> frames.getCommonFrame().stream())
                .filter(jaxbElement -> jaxbElement.getValue() instanceof GeneralFrame)
                .map(jaxbElement -> (GeneralFrame) jaxbElement.getValue())
                .findAny().get();
    }

    public SiteFrame findSiteFrame(PublicationDeliveryStructure incomingPublicationDelivery) {

        List<JAXBElement<? extends Common_VersionFrameStructure>> compositeFrameOrCommonFrame = incomingPublicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame();

        Optional<SiteFrame> optionalSiteframe = compositeFrameOrCommonFrame
                .stream()
                .filter(element -> element.getValue() instanceof SiteFrame)
                .map(element -> (SiteFrame) element.getValue())
                .findFirst();

        if (optionalSiteframe.isPresent()) {
            return optionalSiteframe.get();
        }

        return compositeFrameOrCommonFrame
                .stream()
                .filter(element -> element.getValue() instanceof CompositeFrame)
                .map(element -> (CompositeFrame) element.getValue())
                .map(compositeFrame -> compositeFrame.getFrames())
                .flatMap(frames -> frames.getCommonFrame().stream())
                .filter(jaxbElement -> jaxbElement.getValue() instanceof SiteFrame)
                .map(jaxbElement -> (SiteFrame) jaxbElement.getValue())
                .findAny().get();
    }

    public Set<String> getImportedIds(DataManagedObjectStructure dataManagedObject) {

        return Stream.of(dataManagedObject)
                .filter(Objects::nonNull)
                .map(object -> object.getKeyList())
                .flatMap(keyList -> keyList.getKeyValue().stream())
                .filter(keyValueStructure -> keyValueStructure.getKey().equals(ORIGINAL_ID_KEY))
                .map(keyValue -> keyValue.getValue())
                .map(value -> value.split(","))
                .flatMap(values -> Stream.of(values))
                .collect(toSet());
    }

    public String getValueByKey(DataManagedObjectStructure dataManagedObject, String key) {

        return Stream.of(dataManagedObject)
                .filter(Objects::nonNull)
                .map(object -> object.getKeyList())
                .filter(Objects::nonNull)
                .flatMap(keyList -> keyList.getKeyValue().stream())
                .filter(keyValueStructure -> keyValueStructure.getKey().equals(key))
                .map(keyValue -> keyValue.getValue())
                .flatMap(values -> Stream.of(values))
                .findFirst().orElse(null);
    }

    public boolean hasGeneralFrame(GeneralFrame generalFrame) {
        return generalFrame!= null;
    }
}
