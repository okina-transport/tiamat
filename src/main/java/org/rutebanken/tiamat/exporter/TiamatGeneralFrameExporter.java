package org.rutebanken.tiamat.exporter;

import org.rutebanken.tiamat.model.GeneralFrame;
import org.rutebanken.tiamat.model.Members;
import org.rutebanken.tiamat.model.ModificationEnumeration;
import org.rutebanken.tiamat.model.TypeOfFrameRefStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TiamatGeneralFrameExporter {

    private static final Logger logger = LoggerFactory.getLogger(TiamatGeneralFrameExporter.class);

    @Autowired
    public TiamatGeneralFrameExporter(){
    }

    public org.rutebanken.tiamat.model.GeneralFrame createTiamatGeneralFrame(String siteName, LocalDateTime localDateTime, TypeEnumeration exportType) {
        // Frame <GeneralFrame>
        org.rutebanken.tiamat.model.GeneralFrame generalFrame = new org.rutebanken.tiamat.model.GeneralFrame();
        String localDateTimeString = localDateTime + "Z";
        localDateTimeString = localDateTimeString.replace("-", "");
        localDateTimeString = localDateTimeString.replace(":", "");
        generalFrame.setModification(ModificationEnumeration.REVISE);
        generalFrame.setVersion(1L);


        if (TypeEnumeration.PARKING.equals(exportType)) {
            generalFrame.setNetexId(siteName + ":GeneralFrame:NETEX_PARKING_" + localDateTimeString + ":LOC");
            setFramesParking(generalFrame);
        } else {
            generalFrame.setNetexId(siteName + ":GeneralFrame:NETEX_ARRET_" + localDateTimeString + ":LOC");
            setFramesDefault(generalFrame);
        }
        logger.info("Adding {} generalFrame", generalFrame);
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

    public void setFramesParking(GeneralFrame generalFrame) {
        // Frame <TypeOfFrameRef>
        TypeOfFrameRefStructure typeOfFrameRefStructure = new TypeOfFrameRefStructure();
        typeOfFrameRefStructure.setRef("FR:TypeOfFrame:NETEX_PARKING");
        typeOfFrameRefStructure.setValue("version=\"1.1:FR-NETEX_PARKING-2.2\"");
        generalFrame.setTypeOfFrameRef(typeOfFrameRefStructure);

        logger.info("Adding {} typeOfFrameRefStructure in generalFrame", typeOfFrameRefStructure);

        // Frame <members>
        Members members = new Members();
        generalFrame.setMembers(members);

        logger.info("Adding {} members in generalFrame", members);
    }
}
