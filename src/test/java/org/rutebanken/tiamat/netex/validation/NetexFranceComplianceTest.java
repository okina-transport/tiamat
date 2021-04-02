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

package org.rutebanken.tiamat.netex.validation;

import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.rutebanken.netex.model.AccessibilityAssessment;
import org.rutebanken.netex.model.AccessibilityLimitation;
import org.rutebanken.netex.model.Common_VersionFrameStructure;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.GeneralFrame;
import org.rutebanken.netex.model.General_VersionFrameStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.PostalAddress;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.TypeOfPlaceRefs_RelStructure;
import org.rutebanken.netex.model.VehicleModeEnumeration;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.exporter.StreamingPublicationDelivery;
import org.rutebanken.tiamat.exporter.async.ExportJobWorker;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.model.Value;
import org.rutebanken.tiamat.model.job.ExportJob;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.repository.ExportJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class NetexFranceComplianceTest extends TiamatIntegrationTest {

    @Autowired
    StreamingPublicationDelivery streamingPublicationDelivery;

    @Autowired
    ExportJobRepository exportJobRepository;

    @Autowired
    NetexXmlReferenceValidator netexXmlReferenceValidator;

    String tiamatExportDestination = "local";

    private String testPath ="src/test/data/netexFrance";
    private String fileNameWithoutExtention = "generatedNetexArrets";
    private String completeFilePath=testPath+"/test/"+fileNameWithoutExtention;

    @Test
    public void validateNetexFranceFormatReferences() throws Exception {
        deleteFileCreated(completeFilePath);
        initData();

        File testPathFile = new File(testPath);
        testPathFile.mkdirs();

        ExportJob exportJob = new ExportJob(JobStatus.PROCESSING);
        exportJob.setFileName(fileNameWithoutExtention);
        ExportParams exportParams = ExportParams.newExportParamsBuilder()
                                                .setStopPlaceSearch(
                                                        StopPlaceSearch
                                                                .newStopPlaceSearchBuilder()
                                                                .setVersionValidity(ExportParams.VersionValidity.ALL)
                                                                .build())
                                                .setProviderId(1L)
                                                .build();
        exportJob.setExportParams(exportParams);

        Provider provider = Collections.singletonList(providerRepository.getProvider(1L)).get(0);
        LocalDateTime localDateTime = LocalDateTime.now(ZoneOffset.UTC).withNano(0);


       ExportJobWorker exportJobWorker = new ExportJobWorker(exportJob, streamingPublicationDelivery, testPath, fileNameWithoutExtention, blobStoreService, exportJobRepository, netexXmlReferenceValidator, provider, localDateTime, tiamatExportDestination);
       exportJobWorker.run();

       if (exportJob.getStatus().equals(JobStatus.FAILED)){
           Assert.fail("Failure in Netex France generation");
       }

        unzipFile(completeFilePath,testPath+"/test");

        PublicationDeliveryStructure publicationDelivery = unmarshal(testPath + "/test/" + fileNameWithoutExtention + ".xml");
        checkGeneralInfo(publicationDelivery);
        checkDataObjects(publicationDelivery.getDataObjects());
        deleteFileCreated(completeFilePath+".xml");
        deleteFileCreated(completeFilePath);

    }

    private void checkDataObjects(PublicationDeliveryStructure.DataObjects dataObjects){
        Common_VersionFrameStructure firstFrame = dataObjects.getCompositeFrameOrCommonFrame().stream().findFirst().get().getValue();
        Assert.assertTrue(firstFrame.getId().startsWith("test:GeneralFrame:NETEX_ARRET_"));
        Assert.assertTrue(firstFrame.getId().endsWith(":LOC"));
        Assert.assertEquals("wrong version","any",firstFrame.getVersion());
        Assert.assertEquals("wrong typeOffFrame ref",firstFrame.getTypeOfFrameRef().getRef(),"FR:TypeOfFrame:NETEX_ARRET");
        Assert.assertEquals("wrong typeOffFrame value",firstFrame.getTypeOfFrameRef().getValue(),"version=\"1.1:FR-NETEX_ARRET-2.2\"");
        checkMembers(((GeneralFrame)firstFrame).getMembers());
    }

    private void checkMembers(General_VersionFrameStructure.Members members){

        for (JAXBElement<? extends EntityStructure> jaxbElement : members.getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity()) {
            checkQuayValues((org.rutebanken.netex.model.Quay)jaxbElement.getValue());
        }
    }


    private void checkQuayValues(org.rutebanken.netex.model.Quay quay){
        String quayNb = quay.getId().split(":")[2];

        Assert.assertEquals("wrong id","NSR:Quay:"+quayNb,quay.getId());
        Assert.assertEquals("wrong privateCode","quay"+quayNb,quay.getPrivateCode().getValue());

        Assert.assertEquals("wrong version","any",quay.getVersion());
        Assert.assertEquals("wrong srsName","EPSG:2154",quay.getCentroid().getLocation().getPos().getSrsName());
        Assert.assertEquals("wrong position","6884297.4",quay.getCentroid().getLocation().getPos().getValue().get(0).toString());
        Assert.assertEquals("wrong position","3004933.7",quay.getCentroid().getLocation().getPos().getValue().get(1).toString());


        checkPlaceType(quay.getPlaceTypes());

        PostalAddress postalAddress = quay.getPostalAddress();
        String postalAdressId = postalAddress.getId();

        Assert.assertEquals("wrong postalAdressId","test:PostalAddress:NSR:Quay:"+quayNb,postalAdressId);
        MultilingualString postalAdressName = postalAddress.getName();
        Assert.assertEquals("wrong lang","fr",postalAdressName.getLang());
        Assert.assertEquals("wrong address name","NSR:Quay:"+quayNb+"-address",postalAdressName.getValue());
        checkPlaceType(postalAddress.getPlaceTypes());
        Assert.assertEquals("wrong country ref","fr",postalAddress.getCountryRef().getValue());
        Assert.assertEquals("wrong postal regions","75000",postalAddress.getPostalRegion());

        AccessibilityAssessment accessAssessment = quay.getAccessibilityAssessment();

        Assert.assertEquals("wrong accessibility id","test:AccessibilityAssessment:NSR:Quay:"+quayNb,accessAssessment.getId());
        Assert.assertEquals("wrong accessibility assessment","unknown",accessAssessment.getMobilityImpairedAccess().value());
        Assert.assertEquals("wrong accessibility version","any",accessAssessment.getVersion());

        AccessibilityLimitation limitations = accessAssessment.getLimitations().getAccessibilityLimitation();
        Assert.assertEquals("wrong accessibility wheelchair","unknown",limitations.getWheelchairAccess().value());
        Assert.assertEquals("wrong accessibility audible","unknown",limitations.getAudibleSignalsAvailable().value());
        Assert.assertEquals("wrong accessibility visual","unknown",limitations.getVisualSignsAvailable().value());

        int stopPlaceNumber = Integer.valueOf(quayNb) + 1;
        Assert.assertEquals("wrong site ref","NSR:StopPlace:"+stopPlaceNumber,quay.getSiteRef().getRef());
        Assert.assertEquals("wrong transport mode", VehicleModeEnumeration.BUS,quay.getTransportMode());



    }

    private void checkPlaceType(TypeOfPlaceRefs_RelStructure placeTypes){
        Assert.assertEquals("wrong typeofplace","monomodalStopPlace",placeTypes.getTypeOfPlaceRef().get(0).getRef());
    }

    private void checkGeneralInfo( PublicationDeliveryStructure publicationDelivery ){
        Assert.assertTrue(publicationDelivery.getPublicationTimestamp() != null );
        Assert.assertEquals("wrong participant ref","test",publicationDelivery.getParticipantRef());
    }


    private void initData(){
        final int numberOfStopPlaces = StopPlaceSearch.DEFAULT_PAGE_SIZE;
        for (int i = 0; i < numberOfStopPlaces; i++) {
            StopPlace stopPlace = new StopPlace(new EmbeddableMultilingualString("stop place number " + i));
            stopPlace.setVersion(1L);
            stopPlace.setProvider("test");
            stopPlace.setStopPlaceType(StopTypeEnumeration.ONSTREET_BUS);


            Quay quay = new Quay();
            quay.setNetexId("NSR:Quay:" + i);
            quay.setName(new EmbeddableMultilingualString("Quay_" + i));
            quay.setPublicCode("quay" + i);
            quay.setCentroid(geometryFactory.createPoint(new Coordinate(48, 2)));
            quay.setZipCode("75000");

            stopPlace.getQuays().add(quay);

            stopPlaceRepository.save(stopPlace);

        }
        stopPlaceRepository.flush();
    }

    public static void unzipFile(String zipFileName, String targetFolder) {
        try {
            FileInputStream inputStream = new FileInputStream(new File(zipFileName));
            byte[] buffer = new byte[1024];
            ZipInputStream zis = new ZipInputStream(inputStream);
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                String fileName = zipEntry.getName();

                File newFile = new File(targetFolder + "/" + fileName);
                if (fileName.endsWith("/")) {
                    newFile.mkdirs();
                    continue;
                }

                File parent = newFile.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }


                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (IOException ioE) {
            throw new RuntimeException("Unzipping archive failed: " + ioE.getMessage(), ioE);
        }
    }

    private void deleteFileCreated(String filePath) {
        File file = new File(filePath);
        if(file.delete()){
            System.out.println("Fichier de test supprimé");
        }
    }

    public PublicationDeliveryStructure unmarshal(String path) throws JAXBException, XMLStreamException, IOException, SAXException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        JAXBElement<PublicationDeliveryStructure> netexFile = (JAXBElement<PublicationDeliveryStructure>) unmarshaller.unmarshal(new File(path));
        return netexFile.getValue();
    }



}