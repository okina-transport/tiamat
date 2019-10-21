package org.rutebanken.tiamat.exporter;

import org.rutebanken.tiamat.model.GeneralFrame;
import org.rutebanken.tiamat.model.Members;
import org.rutebanken.tiamat.model.ModificationEnumeration;
import org.rutebanken.tiamat.model.Quays_RelStructure;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.TypeOfFrameRefStructure;
import org.rutebanken.tiamat.netex.id.NetexIdHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

@Service
public class TiamatGeneralFrameExporter {

    private final NetexIdHelper netexIdHelper;

    private static final Logger logger = LoggerFactory.getLogger(TiamatGeneralFrameExporter.class);


    @Autowired
    public TiamatGeneralFrameExporter(NetexIdHelper netexIdHelper) {
        this.netexIdHelper = netexIdHelper;
    }

    public org.rutebanken.tiamat.model.GeneralFrame createTiamatGeneralFrame() {
        // Frame <GeneralFrame>
        org.rutebanken.tiamat.model.GeneralFrame generalFrame = new org.rutebanken.tiamat.model.GeneralFrame();
        setFramesDefault(generalFrame);
        generalFrame.setNetexId("NETEX_ARRET_IDF_" + Instant.now() + ":LOC");
        generalFrame.setModification(ModificationEnumeration.REVISE);
        return generalFrame;
    }

    public void setFramesDefault(GeneralFrame generalFrame) {
        // Frame <TypeOfFrameRef>
        TypeOfFrameRefStructure typeOfFrameRefStructure = new TypeOfFrameRefStructure();
        typeOfFrameRefStructure.setRef("FR:TypeOfFrame:NETEX_ARRET");
        typeOfFrameRefStructure.setValue("version=\"1.04 :FR1-NETEX_ARRET_IDF-2.1\"");
        generalFrame.setTypeOfFrameRef(typeOfFrameRefStructure);

        // Frame <members>
        Members members = new Members();
        generalFrame.setMembers(members);
    }

//      TODO vérifier l'utilité de ce bout de code
//    public void addQuaysToTiamatGeneralFrame(org.rutebanken.tiamat.model.GeneralFrame generalFrame, List<StopPlace> iterableStopPlaces) {
//        Quays_RelStructure quays_relStructure = new Quays_RelStructure();
//
//        if (iterableStopPlaces != null) {
//            iterableStopPlaces.forEach(stopPlace ->
//                    stopPlace.getQuays().forEach(quay -> quays_relStructure.getQuayRefOrQuay().add(quay)));
//            logger.info("Adding {} quays", quays_relStructure.getQuayRefOrQuay().size());
//            generalFrame.getMembers().setQuays_relStructure(quays_relStructure);
//            if (generalFrame.getMembers().getQuays_relStructure().getQuayRefOrQuay().isEmpty()) {
//                generalFrame.getMembers().setQuays_relStructure(null);
//            }
//        }
//    }
}
