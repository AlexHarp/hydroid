package au.gov.ga.hydroid.controller;

import au.gov.ga.hydroid.HydroidConfiguration;
import au.gov.ga.hydroid.dto.ServiceResponse;
import au.gov.ga.hydroid.model.DocumentType;
import au.gov.ga.hydroid.service.S3Client;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
@RequestMapping("/import")
public class CsvExtractorController {
   private Logger logger = LoggerFactory.getLogger(getClass());

   @Autowired
   private HydroidConfiguration configuration;

   @Autowired
   private S3Client s3Client;

   @RequestMapping(value="/bulk", method = {RequestMethod.POST})
   public @ResponseBody
   ResponseEntity<ServiceResponse> enhanceFile(@RequestParam("name") String name,
                                               @RequestParam("file") MultipartFile file) {
      if (!file.isEmpty()) {
         try {
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()));
            while ((line = br.readLine()) != null) {
               String url = line.split(",")[0];
               fetchUrlAndPushToS3(url);
            }
         } catch (Throwable e) {
            logger.error("Failed to get URL from CSV: " + name,e);
         }
      } else {
         return new ResponseEntity<>(
                 new ServiceResponse("You failed to upload " + name + " because the file was empty."), HttpStatus.OK);
      }

      return new ResponseEntity<>(new ServiceResponse("Your document has queue for enhancement successfully."),
              HttpStatus.OK);
   }

   private void fetchUrlAndPushToS3(String url) {
      try {
         URL obj = new URL(url);
         HttpURLConnection con = (HttpURLConnection) obj.openConnection();
         int responseCode = con.getResponseCode();

         if(responseCode < 300) {
            s3Client.storeFile(configuration.getS3Bucket(),
                    configuration.getS3EnhancerInput() + DocumentType.DOCUMENT.name().toLowerCase() + "s/" + obj.getHost() + obj.getPath(),
                    con.getInputStream(),
                    ContentType.TEXT_XML.getMimeType());
         }
      } catch (IOException e) {
         logger.error("Failed to fetch or store file in S3: " + url,e);
      }
   }
}