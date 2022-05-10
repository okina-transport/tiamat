package org.rutebanken.tiamat.netex;

import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.StopPlace;

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
}
