package org.rutebanken.tiamat.netex;

import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.importer.NetexImporter;
import org.rutebanken.tiamat.netex.mapping.PublicationDeliveryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.rutebanken.tiamat.importer.NetexImporter.IMPORT_CORRELATION_ID;
import static org.rutebanken.tiamat.netex.mapping.NetexMappingContextThreadLocal.updateMappingGeneralFrameContext;

public class NetexUtils {

    private static final Logger logger = LoggerFactory.getLogger(NetexUtils.class);

    public static List<Quay> getQuaysFromStopPlace(StopPlace stopPlace){
        List<Quay> quays = new ArrayList<>();
        List<Object> rawQuayList = stopPlace.getQuays().getQuayRefOrQuay().stream().map(JAXBElement::getValue).collect(Collectors.toList());
        if (!rawQuayList.isEmpty()){
            quays = rawQuayList.stream()
                                .map(quayObj -> (Quay) quayObj)
                                .collect(Collectors.toList());

        }
        return quays;
    }


    public static void fillTypeOfKey(DataManagedObjectStructure object){
        if (object.getKeyList() == null || object.getKeyList().getKeyValue() == null || object.getKeyList().getKeyValue().isEmpty()){
            return;
        }

        for (KeyValueStructure keyValueStructure : object.getKeyList().getKeyValue()) {
            if ("imported-id".equals(keyValueStructure.getKey())){
                keyValueStructure.setTypeOfKey("ALTERNATE_IDENTIFIER");
            }
        }
    }

    public static <Parking> List<Parking> getMembers(Class<Parking> clazz, List<JAXBElement<? extends EntityStructure>> members) {
        List<Parking> foundMembers = new ArrayList<>();

        for (JAXBElement<? extends EntityStructure> member : members) {
            if (member.getValue().getClass().equals(clazz)) {
                foundMembers.add(clazz.cast(member.getValue()));
            }
        }

        return foundMembers;
    }

    public static List<JAXBElement<? extends EntityStructure>> getMembersFromPublicationDelivery(PublicationDeliveryStructure publicationDeliveryStructure) {

        PublicationDeliveryHelper publicationDeliveryHelper = new PublicationDeliveryHelper();

        GeneralFrame netexGeneralFrame = publicationDeliveryHelper.findGeneralFrame(publicationDeliveryStructure);
        String requestId = netexGeneralFrame.getId();
        updateMappingGeneralFrameContext(netexGeneralFrame);

        GeneralFrame responseGeneralFrame = new GeneralFrame();
        MDC.put(IMPORT_CORRELATION_ID, requestId);
        logger.info("Publication delivery contains site frame created at {}", netexGeneralFrame.getCreated());
        responseGeneralFrame.withId(requestId + "-response").withVersion("1");

        if (publicationDeliveryHelper.hasGeneralFrame(netexGeneralFrame)) {
            return netexGeneralFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity();
        }
        return null;
    }
}
