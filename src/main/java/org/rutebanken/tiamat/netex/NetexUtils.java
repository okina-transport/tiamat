package org.rutebanken.tiamat.netex;

import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NetexUtils {

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

    public static <Parking> List<Parking> getMembers(Class<Parking> clazz, List<JAXBElement<? extends EntityStructure>> members) {
        List<Parking> foundMembers = new ArrayList<>();

        for (JAXBElement<? extends EntityStructure> member : members) {
            if (member.getValue().getClass().equals(clazz)) {
                foundMembers.add(clazz.cast(member.getValue()));
            }
        }

        return foundMembers;
    }
}
