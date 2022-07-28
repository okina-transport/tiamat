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
import org.rutebanken.netex.model.*;
import org.rutebanken.tiamat.TiamatIntegrationTest;
import org.rutebanken.tiamat.domain.Provider;
import org.rutebanken.tiamat.exporter.ExportTypeEnumeration;
import org.rutebanken.tiamat.exporter.StreamingPublicationDelivery;
import org.rutebanken.tiamat.exporter.async.ExportJobWorker;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.exporter.params.StopPlaceSearch;
import org.rutebanken.tiamat.model.EmbeddableMultilingualString;
import org.rutebanken.tiamat.model.Quay;
import org.rutebanken.tiamat.model.StopPlace;
import org.rutebanken.tiamat.model.StopTypeEnumeration;
import org.rutebanken.tiamat.model.job.ExportJob;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.repository.ExportJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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
        exportJob.setId(1L);
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


       ExportJobWorker exportJobWorker = new ExportJobWorker(exportJob, streamingPublicationDelivery, testPath, fileNameWithoutExtention, blobStoreService, exportJobRepository, netexXmlReferenceValidator, provider, localDateTime, tiamatExportDestination, ExportTypeEnumeration.STOP_PLACE);
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
        Assert.assertTrue(firstFrame.getId().startsWith("MOBI-ITI:GeneralFrame:"));

        Assert.assertEquals("wrong version","1",firstFrame.getVersion());
        //Assert.assertEquals("wrong typeOffFrame ref",firstFrame.getTypeOfFrameRef().getRef(),"FR:TypeOfFrame:NETEX_ARRET");
        //Assert.assertEquals("wrong typeOffFrame value",firstFrame.getTypeOfFrameRef().getValue(),"version=\"1.1:FR-NETEX_ARRET-2.2\"");
        /*((SiteFrame)firstFrame).getStopPlaces().getStopPlace()
                                               .forEach(this::checkStopPlace);*/
        GeneralFrame generalFrame = (GeneralFrame) firstFrame;

        checkStopPlace(generalFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity());
    }

    private void checkStopPlace(List<JAXBElement<? extends EntityStructure>> listMembers){
        //create a list with all the QuayRef that we can find in the stop places
        List<List<Object>> listQuayRef = listMembers.stream()
                .filter(jaxbElement -> jaxbElement.getValue() instanceof org.rutebanken.netex.model.StopPlace)
                .map(jaxStopPlace -> {
                    org.rutebanken.netex.model.StopPlace stopPlace = (org.rutebanken.netex.model.StopPlace) jaxStopPlace.getValue();
                    return stopPlace.getQuays().getQuayRefOrQuay().stream().map(q -> (Object) q.getValue()).collect(Collectors.toList());
                })
                .collect(Collectors.toList());

        //Create a list with just the quays in the members
        List<org.rutebanken.netex.model.Quay> listQuays= listMembers.stream()
                .filter(jaxbElement -> jaxbElement.getValue() instanceof org.rutebanken.netex.model.Quay)
                .map(jaxQuay -> (org.rutebanken.netex.model.Quay)jaxQuay.getValue())
                .collect(Collectors.toList());


        //check that the quayRef that we have in the stop_places member, have their equivalent Quay in the members also
        listQuayRef.stream().forEach(quayRef ->{
            quayRef.stream().forEach(reference ->{
                QuayRefStructure quayRefStructure = (QuayRefStructure)reference;
                listQuays.stream().forEach(quay -> {
                    if(quay.getId().equals(quayRefStructure.getRef())){
                        checkQuayValues(quay);
                    }
                });
            });
        });
    }


    private void checkQuayValues(org.rutebanken.netex.model.Quay quay){
        String quayNb = quay.getId().split(":")[2];

        Assert.assertEquals("wrong id","NSR:Quay:"+quayNb,quay.getId());


        Assert.assertEquals("wrong version","0",quay.getVersion());

        Assert.assertEquals("wrong position","48.000000",quay.getCentroid().getLocation().getLongitude().toString());
        Assert.assertEquals("wrong position","2.000000",quay.getCentroid().getLocation().getLatitude().toString());


      //  checkPlaceType(quay.getPlaceTypes());

        PostalAddress postalAddress = quay.getPostalAddress();
        String postalAdressId = postalAddress.getId();

        Assert.assertEquals("wrong postalAdressId","NSR:PostalAddress:"+quayNb,postalAdressId);
        MultilingualString postalAdressName = postalAddress.getName();

        Assert.assertEquals("wrong address name","Quay_"+quayNb,postalAdressName.getValue());


        Assert.assertEquals("wrong postal regions","75000",postalAddress.getPostalRegion());



        int stopPlaceNumber = Integer.valueOf(quayNb) + 1;
        Assert.assertEquals("wrong site ref","NSR:StopPlace:"+stopPlaceNumber,quay.getSiteRef().getRef());
        Assert.assertEquals("wrong transport mode", AllVehicleModesOfTransportEnumeration.BUS,quay.getTransportMode());



    }

    private void checkPlaceType(TypeOfPlaceRefs_RelStructure placeTypes){
        Assert.assertEquals("wrong typeofplace","monomodalStopPlace",placeTypes.getTypeOfPlaceRef().get(0).getRef());
    }

    private void checkGeneralInfo( PublicationDeliveryStructure publicationDelivery ){
        Assert.assertTrue(publicationDelivery.getPublicationTimestamp() != null );
        Assert.assertEquals("wrong participant ref","MOBIITI",publicationDelivery.getParticipantRef());
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