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
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TiamatGeneralFrameExporter {

    private static final Logger logger = LoggerFactory.getLogger(TiamatGeneralFrameExporter.class);

    @Autowired
    public TiamatGeneralFrameExporter(){
    }

    public org.rutebanken.tiamat.model.GeneralFrame createTiamatGeneralFrame(String siteName, LocalDateTime localDateTime) {
        // Frame <GeneralFrame>
        org.rutebanken.tiamat.model.GeneralFrame generalFrame = new org.rutebanken.tiamat.model.GeneralFrame();
        String localDateTimeString = localDateTime + "Z";
        localDateTimeString = localDateTimeString.replace("-", "");
        localDateTimeString = localDateTimeString.replace(":", "");
        generalFrame.setNetexId(siteName + ":GeneralFrame:NETEX_ARRET_" + localDateTimeString + ":LOC");
        generalFrame.setModification(ModificationEnumeration.REVISE);

        logger.info("Adding {} generalFrame", generalFrame);

        setFramesDefault(generalFrame);
        return generalFrame;
    }

    public void setFramesDefault(GeneralFrame generalFrame) {
        // Frame <TypeOfFrameRef>
        TypeOfFrameRefStructure typeOfFrameRefStructure = new TypeOfFrameRefStructure();
        typeOfFrameRefStructure.setRef("FR:TypeOfFrame:NETEX_ARRET");
        typeOfFrameRefStructure.setValue("version=\"1.1:FR-NETEX_ARRET-2.2\"");
        generalFrame.setTypeOfFrameRef(typeOfFrameRefStructure);

        logger.info("Adding {} typeOfFrameRefStructure in generalFrame", typeOfFrameRefStructure);

        // Frame <members>
        Members members = new Members();
        generalFrame.setMembers(members);

        logger.info("Adding {} members in generalFrame", members);
    }
}
