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

    private static final Logger logger = LoggerFactory.getLogger(TiamatGeneralFrameExporter.class);

    @Autowired
    public TiamatGeneralFrameExporter(){
    }

    public org.rutebanken.tiamat.model.GeneralFrame createTiamatGeneralFrame(String siteName) {
        // Frame <GeneralFrame>
        org.rutebanken.tiamat.model.GeneralFrame generalFrame = new org.rutebanken.tiamat.model.GeneralFrame();
        generalFrame.setNetexId(siteName + ":GeneralFrame:NETEX_ARRET_IDF_" + Instant.now() + ":LOC");
        generalFrame.setModification(ModificationEnumeration.REVISE);

        logger.info("Adding {} generalFrame", generalFrame);

        setFramesDefault(generalFrame);
        return generalFrame;
    }

    public void setFramesDefault(GeneralFrame generalFrame) {
        // Frame <TypeOfFrameRef>
        TypeOfFrameRefStructure typeOfFrameRefStructure = new TypeOfFrameRefStructure();
        typeOfFrameRefStructure.setRef("FR:TypeOfFrame:NETEX_ARRET");
        typeOfFrameRefStructure.setValue("version=\"1.04 :FR1-NETEX_ARRET_IDF-2.1\"");
        generalFrame.setTypeOfFrameRef(typeOfFrameRefStructure);

        logger.info("Adding {} typeOfFrameRefStructure in generalFrame", typeOfFrameRefStructure);

        // Frame <members>
        Members members = new Members();
        generalFrame.setMembers(members);

        logger.info("Adding {} members in generalFrame", members);
    }
}
