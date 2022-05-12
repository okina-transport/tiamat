package org.rutebanken.tiamat.netex.mapping.mapper;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MappingContext;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.netex.model.StopPlace;

import javax.xml.bind.JAXBElement;
import java.util.List;
import java.util.stream.Collectors;

public class SiteFrameCustomMapper extends CustomMapper<SiteFrame, org.rutebanken.tiamat.model.SiteFrame> {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    @Override
    public void mapAtoB(SiteFrame netexSiteFrame, org.rutebanken.tiamat.model.SiteFrame tiamatSiteFrame, MappingContext context) {
        super.mapAtoB(netexSiteFrame, tiamatSiteFrame, context);
        List<StopPlace> netexStopPlaces = netexSiteFrame.getStopPlaces().getStopPlace_().stream().map(sp -> (StopPlace) sp.getValue()).collect(Collectors.toList());
        tiamatSiteFrame.getStopPlaces().getStopPlace().addAll(mapperFacade.mapAsList(netexStopPlaces, org.rutebanken.tiamat.model.StopPlace.class));
    }

    @Override
    public void mapBtoA(org.rutebanken.tiamat.model.SiteFrame tiamatSiteFrame, SiteFrame netexSiteFrame, MappingContext context) {
        super.mapBtoA(tiamatSiteFrame, netexSiteFrame, context);
        if (tiamatSiteFrame.getStopPlaces() != null && tiamatSiteFrame.getStopPlaces().getStopPlace() != null ) {
            List<StopPlace> netexStopPlaces = mapperFacade.mapAsList(tiamatSiteFrame.getStopPlaces().getStopPlace(), StopPlace.class);

            netexSiteFrame.getStopPlaces().withStopPlace_(netexStopPlaces.stream()
                    .map(netexObjectFactory::createStopPlace)
                    .collect(Collectors.toList()));
        }
    }
}
