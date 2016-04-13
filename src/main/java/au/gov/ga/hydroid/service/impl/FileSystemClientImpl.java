package au.gov.ga.hydroid.service.impl;

import au.gov.ga.hydroid.service.DataObjectSummary;
import au.gov.ga.hydroid.service.S3Client;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service("s3FileSystem")
public class FileSystemClientImpl implements S3Client {

   private static final Logger logger = LoggerFactory.getLogger(FileSystemClientImpl.class);

   private Path basePath;

   public FileSystemClientImpl() {
      String customPath = System.getProperties().getProperty("s3.use.file.system.path");
      if(customPath == null || customPath.isEmpty()) {
         customPath = System.getProperty("java.io.tmpdir");
      }
      this.basePath = FileSystems.getDefault().getPath(customPath).toAbsolutePath();
   }

   @Override
   public String getAccountOwner() {
      return null;
   }

   private File doGetFile(String bucketName, String key) {
      Path p = FileSystems.getDefault().getPath(basePath.toString(), bucketName, key);
      return new File(p.toString());
   }

   private void ensureDirectoriesExist(String bucketName, String key) {
      File file = doGetFile(bucketName, key);
      Path parentDir = file.toPath().getParent();
      if (!Files.exists(parentDir)) {
         try {
            Files.createDirectories(parentDir);
         } catch (IOException e) {
            logger.debug("getFile - IOException: ", e);
         }
      }
   }

   @Override
   public InputStream getFile(String bucketName, String key) {
      InputStream result = null;
      Path fileToGet = doGetFile(bucketName,key).toPath().toAbsolutePath();
      logger.debug("Trying to get file :" + fileToGet.toString());
      logger.debug("File exists : " + Files.exists(fileToGet));
      logger.debug("Trying to get file :" + doGetFile(bucketName,key).toPath().toAbsolutePath().toString());
      try {
         if(Files.exists(fileToGet)) {
            result = FileUtils.openInputStream(fileToGet.toFile());
         }
      } catch (IOException e) {
         logger.debug("openInputStream - IOException: ", e);
      }
      return result;
   }

   @Override
   public byte[] getFileAsByteArray(String bucketName, String key) {
      byte[] result = null;
      InputStream is = null;
      Path fileToGet = doGetFile(bucketName,key).toPath().toAbsolutePath();
      logger.debug("Trying to get file :" + fileToGet.toString());
      logger.debug("File exists : " + Files.exists(fileToGet));
      try {
         if(Files.exists(fileToGet)) {
            is = FileUtils.openInputStream(fileToGet.toFile());
            result = IOUtils.toByteArray(is);
         }
         } catch (IOException e) {
            logger.debug("getFileAsByteArray - IOException: ", e);
         }
         finally {
         try {
            if(is != null) {
               is.close();
            }
         } catch (IOException e) {
            logger.debug("Failed to close input stream.",e);
         }
      }
      return result;
   }

   @Override
   public void storeFile(String bucketName, String key, String content, String contentType) {
      try {
         ensureDirectoriesExist(bucketName, key);
         Files.write(doGetFile(bucketName, key).toPath(), Collections.singletonList(content), Charset.forName("UTF-8"));
      } catch (IOException e) {
         logger.debug("storeFile - IOException: ", e);
      }
   }

   @Override
   public void storeFile(String bucketName, String key, InputStream content, String contentType) {
      try {
         ensureDirectoriesExist(bucketName, key);
         Files.write(doGetFile(bucketName, key).toPath(), IOUtils.toByteArray(content));
      } catch (IOException e) {
         logger.debug("storeFile - IOException: ", e);
      }
   }

   @Override
   public void deleteFile(String bucketName, String key) {
      try {
         Path file = doGetFile(bucketName, key).toPath();
         if (Files.exists(file)) {
            Files.delete(file);
         }
      } catch (IOException e) {
         logger.debug("deleteFile - IOException: ", e);
      }
   }

   @Override
   public List<DataObjectSummary> listObjects(String bucketName, String key) {
      List<DataObjectSummary> result = new ArrayList<>();
      File fileRoot = doGetFile(bucketName, key);
      logger.debug("Listing files in:" + fileRoot.getAbsolutePath());
      if (fileRoot.listFiles() == null) {
         return result;
      }
      for (File file : fileRoot.listFiles()) {
         String addKey = file.getPath().replace(this.basePath.toAbsolutePath().toString() + File.separator, "").replaceFirst(bucketName, "").replaceAll("\\\\", "/");
         result.add(new DataObjectSummaryImpl(bucketName, addKey));
      }
      return result;
   }

   @Override
   public void copyObject(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey) {
      try {
         ensureDirectoriesExist(destinationBucketName, destinationKey);
         Files.copy(doGetFile(sourceBucketName, sourceKey).toPath(), doGetFile(destinationBucketName, destinationKey).toPath());
      } catch (IOException e) {
         logger.debug("copyObject - IOException: ", e);
      }
   }

}
