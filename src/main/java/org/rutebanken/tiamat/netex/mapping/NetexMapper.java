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

import ma.glasnost.orika.*;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.rutebanken.netex.model.*;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.tiamat.netex.mapping.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class NetexMapper {
    private static final Logger logger = LoggerFactory.getLogger(NetexMapper.class);

    private final MapperFacade facade;

    @Autowired
    public NetexMapper(List<Converter> converters, KeyListToKeyValuesMapMapper keyListToKeyValuesMapMapper,
                       DataManagedObjectStructureMapper dataManagedObjectStructureMapper,
                       PublicationDeliveryHelper publicationDeliveryHelper,
                       AccessibilityAssessmentMapper accessibilityAssessmentMapper,
                       PointOfInterestMapper pointOfInterestMapper) {

        logger.info("Setting up netexMapper with DI");

        MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();

        logger.info("Creating netex mapperFacade with {} converters ", converters.size());

        if(logger.isDebugEnabled()) {
            logger.debug("Converters: {}", converters);
        }

        converters.forEach(converter -> mapperFactory.getConverterFactory().registerConverter(converter));

        // Issues with registering multiple mappers
        mapperFactory.registerMapper(keyListToKeyValuesMapMapper);

        mapperFactory.classMap(SiteFrame.class, org.rutebanken.tiamat.model.SiteFrame.class)
                .customize(new SiteFrameCustomMapper())
                .byDefault()
                .register();


        mapperFactory.classMap(GeneralFrame.class, org.rutebanken.tiamat.model.GeneralFrame.class)
                .byDefault()
                .register();

        mapperFactory.classMap(TopographicPlace.class, org.rutebanken.tiamat.model.TopographicPlace.class)
                .fieldBToA("name", "descriptor.name")
                .customize(new TopographicPlaceMapper())
                .byDefault()
                .register();

        mapperFactory.classMap(GroupOfStopPlaces.class, org.rutebanken.tiamat.model.GroupOfStopPlaces.class)
                .byDefault()
                .register();

        mapperFactory.classMap(StopPlace.class, org.rutebanken.tiamat.model.StopPlace.class)
                .fieldBToA("topographicPlace", "topographicPlaceRef")
                .fieldAToB("topographicPlaceRef.ref", "topographicPlace.netexId")
                .fieldAToB("topographicPlaceRef.version", "topographicPlace.version")
                // TODO: Excluding some fields while waiting for NRP-1354
                .exclude("localServices")
                .exclude("roadAddress")
                .customize(new StopPlaceMapper(publicationDeliveryHelper))
                .byDefault()
                .register();

        mapperFactory.classMap(Quay.class, org.rutebanken.tiamat.model.Quay.class)
                .exclude("localServices")
                .exclude("roadAddress")
                .exclude("otherTransportModes")
                .customize(new QuayMapper())
                .byDefault()
                .register();

        mapperFactory.classMap(TariffZone.class, org.rutebanken.tiamat.model.TariffZone.class)
                .byDefault()
                .register();

        mapperFactory.classMap(PointOfInterest.class, org.rutebanken.tiamat.model.PointOfInterest.class)
                .fieldBToA("netexId", "id")
                .customize(pointOfInterestMapper)
                .byDefault()
                .register();

        mapperFactory.classMap(PointOfInterestClassification.class, org.rutebanken.tiamat.model.PointOfInterestClassification.class)
                .fieldBToA("netexId", "id")
                .customize(new PointOfInterestClassificationMapper())
                .register();

        mapperFactory.classMap(Parking.class, org.rutebanken.tiamat.model.Parking.class)
                .exclude("paymentMethods")
                .exclude("cardsAccepted")
                .exclude("currenciesAccepted")
                .exclude("accessModes")
                .fieldBToA("netexId", "id")
                .customize(new ParkingMapper())
                .byDefault()
                .register();

        mapperFactory.classMap(ParkingArea.class, org.rutebanken.tiamat.model.ParkingArea.class)
                .customize(new ParkingAreaMapper())
                .byDefault()
                .register();

        mapperFactory.classMap(PathLinkEndStructure.class, org.rutebanken.tiamat.model.PathLinkEnd.class)
                .byDefault()
                .register();

        mapperFactory.classMap(PathLink.class, org.rutebanken.tiamat.model.PathLink.class)
                .byDefault()
                .register();

        mapperFactory.classMap(InstalledEquipment_VersionStructure.class, org.rutebanken.tiamat.model.InstalledEquipment_VersionStructure.class)
                .fieldBToA("netexId", "id")
                .customize(new InstalledEquipmentMapper())
                .byDefault()
                .register();

        mapperFactory.classMap(WaitingRoomEquipment.class, org.rutebanken.tiamat.model.WaitingRoomEquipment.class)
                .customize(new WaitingRoomEquipmentMapper())
                .byDefault()
                .register();

        mapperFactory.classMap(SanitaryEquipment.class, org.rutebanken.tiamat.model.SanitaryEquipment.class)
                .customize(new SanitaryEquipmentMapper())
                .byDefault()
                .register();

        mapperFactory.classMap(TicketingEquipment.class, org.rutebanken.tiamat.model.TicketingEquipment.class)
                .customize(new TicketingEquipmentMapper())
                .byDefault()
                .register();

        mapperFactory.classMap(ShelterEquipment.class, org.rutebanken.tiamat.model.ShelterEquipment.class)
                .customize(new ShelterEquipmentMapper())
                .byDefault()
                .register();

        mapperFactory.classMap(CycleStorageEquipment.class, org.rutebanken.tiamat.model.CycleStorageEquipment.class)
                .byDefault()
                .register();

        mapperFactory.classMap(GeneralSign.class, org.rutebanken.tiamat.model.GeneralSign.class)
                .customize(new GeneralSignMapper())
                .byDefault()
                .register();

        mapperFactory.classMap(PlaceEquipments_RelStructure.class, org.rutebanken.tiamat.model.PlaceEquipment.class)
                .fieldBToA("netexId", "id")
                .customize(new PlaceEquipmentMapper())
                .byDefault()
                .register();

        mapperFactory.classMap(AccessibilityAssessment.class, org.rutebanken.tiamat.model.AccessibilityAssessment.class)
                .customize(accessibilityAssessmentMapper)
                .exclude("id")
                .byDefault()
                .register();

        mapperFactory.classMap(DataManagedObjectStructure.class, org.rutebanken.tiamat.model.DataManagedObjectStructure.class)
                .fieldBToA("keyValues", "keyList")
                .field("validBetween[0]", "validBetween")
                .customize(dataManagedObjectStructureMapper)
                .exclude("id")
                .exclude("keyList")
                .exclude("keyValues")
                .exclude("version")
                .byDefault()
                .register();

        facade = mapperFactory.getMapperFacade();
    }

    public TopographicPlace mapToNetexModel(org.rutebanken.tiamat.model.TopographicPlace topographicPlace) {
        return facade.map(topographicPlace, TopographicPlace.class);
    }

    public SiteFrame mapToNetexModel(org.rutebanken.tiamat.model.SiteFrame tiamatSiteFrame) {
        SiteFrame siteFrame = facade.map(tiamatSiteFrame, SiteFrame.class);
        return siteFrame;
    }

    public GeneralFrame mapToNetexModel(org.rutebanken.tiamat.model.GeneralFrame tiamatGeneralFrame) {
        GeneralFrame generalFrame = facade.map(tiamatGeneralFrame, GeneralFrame.class);
        return generalFrame;
    }

    public StopPlace mapToNetexModel(org.rutebanken.tiamat.model.StopPlace tiamatStopPlace) {
        StopPlace netexStopPlace = facade.map(tiamatStopPlace, StopPlace.class);
        netexStopPlace.setTransportMode(AllVehicleModesOfTransportEnumeration.BUS);
        netexStopPlace.setWeighting(InterchangeWeightingEnumeration.INTERCHANGE_ALLOWED);

        initTypeOfPlace(netexStopPlace);
        if (netexStopPlace.getQuays() != null) {
            netexStopPlace.getQuays().getQuayRefOrQuay().forEach(quay -> initQuayProperties(netexStopPlace, (Quay) quay.getValue()));
        }

        ValidBetween validBetween = new ValidBetween();
        validBetween.setFromDate(LocalDateTime.now());
        netexStopPlace.getValidBetween().clear();
        netexStopPlace.getValidBetween().add(validBetween);
        return netexStopPlace;
    }

    private void initTypeOfPlace(StopPlace netexStopPlace){

        List<AllVehicleModesOfTransportEnumeration> tranportModeList = new ArrayList<>();

        if (netexStopPlace.getQuays() != null){
            tranportModeList    = netexStopPlace.getQuays().getQuayRefOrQuay().stream()
                    .map(obj -> ((Quay) obj.getValue()).getTransportMode())
                    .filter(transportMode -> transportMode != null)
                    .distinct()
                    .collect(Collectors.toList());
        }


        TypeOfPlaceRefs_RelStructure placeRefs = new TypeOfPlaceRefs_RelStructure();
        TypeOfPlaceRefStructure typeOfPlace = new TypeOfPlaceRefStructure();
        if (tranportModeList.size() > 1){
            typeOfPlace.withRef("multimodalStopPlace");
        }else{
            typeOfPlace.withRef("monomodalStopPlace");
        }
        placeRefs.withTypeOfPlaceRef(typeOfPlace);
        netexStopPlace.setPlaceTypes(placeRefs);
    }

    private void initQuayProperties(StopPlace stopPlace, Quay quay){
        MultilingualString multilingualString = new MultilingualString();
        Optional<String> importedNameOpt = getImportedName(quay);
        if (!importedNameOpt.isPresent()){
            logger.error("Unable to find importedName for quay:" + quay.getId());
        }
        multilingualString.setValue(importedNameOpt.get());
        multilingualString.setLang("fr");
        quay.setName(multilingualString);


        TypeOfPlaceRefs_RelStructure placeRefs = new TypeOfPlaceRefs_RelStructure();
        TypeOfPlaceRefStructure typeOfPlace = new TypeOfPlaceRefStructure();
        typeOfPlace.withRef("monomodalStopPlace");
        placeRefs.withTypeOfPlaceRef(typeOfPlace);
        quay.setPlaceTypes(placeRefs);
        quay.setTransportMode(AllVehicleModesOfTransportEnumeration.BUS);
        SiteRefStructure siteRef = new SiteRefStructure();
        siteRef.withRef(stopPlace.getId());
        quay.setSiteRef(siteRef);
        ValidBetween validBetween = new ValidBetween();
        validBetween.setFromDate(LocalDateTime.now());
        quay.getValidBetween().clear();
        quay.getValidBetween().add(validBetween);
        CountryRef cr = new CountryRef();
        cr.setValue("fr");

        if (quay.getPostalAddress() != null) {
            quay.getPostalAddress().setCountryRef(cr);
            quay.getPostalAddress().setPlaceTypes(placeRefs);
            MultilingualString multilingualStringAddressShortName = new MultilingualString();
            multilingualStringAddressShortName.setValue(quay.getId()+"-address");
            multilingualStringAddressShortName.setLang("fr");
            quay.getPostalAddress().setShortName(multilingualStringAddressShortName);
            quay.getPostalAddress().setName(multilingualStringAddressShortName);
        }
    }

    public static Optional<String> getImportedName(Zone_VersionStructure stopPlace){

        KeyListStructure keyList = stopPlace.getKeyList();
        if (keyList != null && keyList.getKeyValue() != null) {
            List<KeyValueStructure> keyValue = keyList.getKeyValue();
            for (KeyValueStructure structure : keyValue) {
                if (structure != null && "imported-name".equals(structure.getKey())) {

                    String rawIds = structure.getValue();
                    List<String> idList = Arrays.stream(rawIds.split(","))
                            .distinct()
                            .collect(Collectors.toList());

                    return Optional.of(idList.get(0));
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<String> getImportedId(Zone_VersionStructure stopPlace){

        KeyListStructure keyList = stopPlace.getKeyList();
        if (keyList != null && keyList.getKeyValue() != null) {
            List<KeyValueStructure> keyValue = keyList.getKeyValue();
            for (KeyValueStructure structure : keyValue) {
                if (structure != null && "imported-id".equals(structure.getKey())) {

                    return Optional.of(structure.getValue());
                }
            }
        }
        return Optional.empty();
    }


    public Parking mapToNetexModel(org.rutebanken.tiamat.model.Parking tiamatParking) {
        return facade.map(tiamatParking, Parking.class);
    }

    public PointOfInterest mapToNetexModel(org.rutebanken.tiamat.model.PointOfInterest tiamatPointOfInterest) {
        return facade.map(tiamatPointOfInterest, PointOfInterest.class);
    }

    public PointOfInterestClassification mapToNetexModel(org.rutebanken.tiamat.model.PointOfInterestClassification tiamatPointOfInterestClassification) {
        return facade.map(tiamatPointOfInterestClassification, PointOfInterestClassification.class);
    }

    public org.rutebanken.tiamat.model.TopographicPlace mapToTiamatModel(TopographicPlace topographicPlace) {
        return facade.map(topographicPlace, org.rutebanken.tiamat.model.TopographicPlace.class);
    }

    public org.rutebanken.tiamat.model.TariffZone mapToTiamatModel(TariffZone tariffZone) {
        return facade.map(tariffZone, org.rutebanken.tiamat.model.TariffZone.class);
    }

    public List<org.rutebanken.tiamat.model.StopPlace> mapStopsToTiamatModel(List<StopPlace> stopPlaces) {
        return facade.mapAsList(stopPlaces, org.rutebanken.tiamat.model.StopPlace.class);
    }

    public List<org.rutebanken.tiamat.model.Parking> mapParkingsToTiamatModel(List<Parking> parking) {
        return facade.mapAsList(parking, org.rutebanken.tiamat.model.Parking.class);
    }

    public List<org.rutebanken.tiamat.model.PointOfInterest> mapPointsOfInterestToTiamatModel(List<PointOfInterest> pointsOfInterest) {
        return facade.mapAsList(pointsOfInterest, org.rutebanken.tiamat.model.PointOfInterest.class);
    }

    public List<org.rutebanken.tiamat.model.PointOfInterestClassification> mapPointsOfInterestClassificationsToTiamatModel(List<PointOfInterestClassification> pointOfInterestClassifications) {
        return facade.mapAsList(pointOfInterestClassifications, org.rutebanken.tiamat.model.PointOfInterestClassification.class);
    }

    public List<org.rutebanken.tiamat.model.PathLink> mapPathLinksToTiamatModel(List<PathLink> pathLinks) {
        return facade.mapAsList(pathLinks, org.rutebanken.tiamat.model.PathLink.class);
    }

    public org.rutebanken.tiamat.model.SiteFrame mapToTiamatModel(SiteFrame netexSiteFrame) {
        org.rutebanken.tiamat.model.SiteFrame tiamatSiteFrame = facade.map(netexSiteFrame, org.rutebanken.tiamat.model.SiteFrame.class);
        return tiamatSiteFrame;
    }

    public org.rutebanken.tiamat.model.GeneralFrame mapToTiamatModel(GeneralFrame netexGeneralFrame) {
        org.rutebanken.tiamat.model.GeneralFrame tiamatGeneralFrame = facade.map(netexGeneralFrame, org.rutebanken.tiamat.model.GeneralFrame.class);
        return tiamatGeneralFrame;
    }

    public org.rutebanken.tiamat.model.StopPlace mapToTiamatModel(StopPlace netexStopPlace) {
        return facade.map(netexStopPlace, org.rutebanken.tiamat.model.StopPlace.class);
    }

    public org.rutebanken.tiamat.model.Quay mapToTiamatModel(Quay netexQuay) {
        return facade.map(netexQuay, org.rutebanken.tiamat.model.Quay.class);
    }


    public org.rutebanken.tiamat.model.Parking mapToTiamatModel(Parking netexParking) {
        return facade.map(netexParking, org.rutebanken.tiamat.model.Parking.class);
    }

    public org.rutebanken.tiamat.model.PointOfInterest mapToTiamatModel(PointOfInterest netexPointOfInterest) {
        return facade.map(netexPointOfInterest, org.rutebanken.tiamat.model.PointOfInterest.class);
    }

    public org.rutebanken.tiamat.model.PointOfInterestClassification mapToTiamatModel(PointOfInterestClassification netexPointOfInterestClassification) {
        return facade.map(netexPointOfInterestClassification, org.rutebanken.tiamat.model.PointOfInterestClassification.class);
    }

    public Quay mapToNetexModel(org.rutebanken.tiamat.model.Quay tiamatQuay) {
        return facade.map(tiamatQuay, Quay.class);
    }

    public PathLink mapToNetexModel(org.rutebanken.tiamat.model.PathLink pathLink) {
        return facade.map(pathLink, PathLink.class);
    }

    public MapperFacade getFacade() {
        return facade;
    }
}
