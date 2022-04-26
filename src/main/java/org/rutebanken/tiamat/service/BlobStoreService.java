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

package org.rutebanken.tiamat.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.okina.helper.aws.BlobStoreHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BlobStoreService {

    private static final Logger logger = LoggerFactory.getLogger(BlobStoreService.class);

    private final String bucketName;
    private final String blobPath;
    private final AmazonS3 client;


    public BlobStoreService(@Value("${blobstore.aws.access.key}") String accessKey,
                            @Value("${blobstore.aws.access.secret}") String accessSecret,
                            @Value("${blobstore.aws.container.name}") String bucketName,
                            @Value("${blobstore.aws.blob.path}") String blobPath) {
        this.bucketName = bucketName;
        this.blobPath = blobPath;
        this.client = getClient(accessKey, accessSecret);
    }

    public void upload(String fileName, File file) {
        String blobIdName = createBlobIdName(blobPath, fileName);
        try {
            logger.info("Uploading {} to path {} in bucket {}", fileName, blobPath, bucketName);
            BlobStoreHelper.uploadBlob(client, bucketName, blobIdName, file);
        } catch (Exception e) {
            throw new RuntimeException("Error uploading file " + fileName + ", blobIdName " + blobIdName + " to bucket " + bucketName, e);
        }
    }

    private AmazonS3 getClient(String accessKey, String accessSecret) {
        try {
            logger.info(String.format("Getting client for key %s", accessKey));
            return BlobStoreHelper.getClient(accessKey, accessSecret);
        } catch (RuntimeException e) {
            throw new RuntimeException(String.format("Error getting client for key %s", accessKey));
        }
    }

    public InputStream download(String fileName) {
        String blobIdName = createBlobIdName(blobPath, fileName);
        return BlobStoreHelper.getBlob(client, bucketName, blobIdName);
    }

    public File downloadFromAbsolutePath(String absolutePath) {
        InputStream inputStream = BlobStoreHelper.getBlob(client, bucketName, absolutePath);

        Optional<String> fileName = getFileName(inputStream);


        File file = new File(System.getProperty("java.io.tmpdir") + "/" + (fileName.isPresent() ? fileName.get():UUID.randomUUID()));

        try {
            Files.deleteIfExists(file.toPath());
            Files.copy(inputStream, Paths.get(file.getAbsolutePath()), new CopyOption[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    private Optional<String> getFileName(InputStream inputStream){
        if (!(inputStream instanceof S3ObjectInputStream))
            return Optional.empty();

        String path = ((S3ObjectInputStream) inputStream).getHttpRequest().getURI().getPath();
        return Optional.of(getFileNameFromFilePath(path));
    }

    private String getFileNameFromFilePath(String filePath){
        String[] splittedPath = filePath.split("/");
        return splittedPath[splittedPath.length-1];
    }

    public String createBlobIdName(String blobPath, String fileName) {
        return blobPath + '/' + fileName;
    }

    public List<String> listStopPlacesInBlob(String siteId, int maxNbResults){
        List<S3ObjectSummary> stopPlaceFileList = BlobStoreHelper.listAllBlobsRecursively(this.client, this.bucketName, siteId+"/exports");
        Stream<String> fileListStream = stopPlaceFileList.stream()
                                                         .sorted(Comparator.comparing(S3ObjectSummary::getLastModified).reversed())
                                                         .map(S3ObjectSummary::getKey)
                                                         .filter(key -> key.contains("ARRET_"));

        return maxNbResults == 0 ? fileListStream.collect(Collectors.toList()) : fileListStream.limit(maxNbResults).collect(Collectors.toList());
    }

    public List<String> listPointsOfInterestInBlob(String siteId, int maxNbResults){
        List<S3ObjectSummary> poiFileList = BlobStoreHelper.listAllBlobsRecursively(this.client, this.bucketName, siteId+"/exports");
        Stream<String> fileListStream = poiFileList.stream()
                .sorted(Comparator.comparing(S3ObjectSummary::getLastModified).reversed())
                .map(S3ObjectSummary::getKey)
                .filter(key -> key.contains("POI_"));

        return maxNbResults == 0 ? fileListStream.collect(Collectors.toList()) : fileListStream.limit(maxNbResults).collect(Collectors.toList());
    }
}
