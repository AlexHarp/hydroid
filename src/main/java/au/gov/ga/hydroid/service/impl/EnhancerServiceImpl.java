package au.gov.ga.hydroid.service.impl;

import au.gov.ga.hydroid.HydroidConfiguration;
import au.gov.ga.hydroid.model.Document;
import au.gov.ga.hydroid.model.DocumentType;
import au.gov.ga.hydroid.service.*;
import au.gov.ga.hydroid.utils.StanbolMediaTypes;
import org.apache.commons.lang.ArrayUtils;
import org.apache.http.entity.ContentType;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by u24529 on 3/02/2016.
 */
@Service
public class EnhancerServiceImpl implements EnhancerService {

   private static final String[] VALID_PREDICATES = {"title", "subject", "created", "extracted-from", "entity-reference", "entity-label"};
   private static final String GA_PUBLIC_VOCABS = "GAPublicVocabsSandbox";
   private static final String DOCUMENT_CREATOR = "Hydroid Enhancer App";

   @Autowired
   private HydroidConfiguration configuration;

   @Autowired
   private StanbolClient stanbolClient;

   @Autowired
   private SolrClient solrClient;

   @Autowired
   private S3Client s3Client;

   @Autowired
   private DocumentService documentService;

   private Properties generateSolrDocument(List<Statement> rdfDocument, String content, String docType) {
      String predicate = null;
      boolean isValidEntityRef = true;
      List<String> concepts = new ArrayList<>();
      List<String> labels = new ArrayList<>();
      Properties properties = new Properties();
      for (Statement statement : rdfDocument) {
         predicate = statement.getPredicate().getLocalName().toLowerCase();
         if (ArrayUtils.indexOf(VALID_PREDICATES, predicate) >= 0) {
            if (predicate.equalsIgnoreCase("extracted-from") && properties.getProperty("about") == null) {
               properties.put("about", statement.getObject().stringValue());
            } else if (predicate.equalsIgnoreCase("entity-reference")) {
               isValidEntityRef = (statement.getObject().stringValue().indexOf(GA_PUBLIC_VOCABS) >= 0);
               // add new concept if not there yet
               if (isValidEntityRef && !concepts.contains(statement.getObject().stringValue())) {
                  concepts.add(statement.getObject().stringValue());
               }
            } else if (predicate.equalsIgnoreCase("entity-label")) {
               // add new label if not there yet
               if (isValidEntityRef && !labels.contains(statement.getObject().stringValue())) {
                  labels.add(statement.getObject().stringValue());
               }
            } else {
               properties.put(predicate, statement.getObject().stringValue());
            }
         }
      }

      // If docType is provided we add it to the rdf document
      if (docType != null && !docType.isEmpty()) {
         ValueFactory factory = ValueFactoryImpl.getInstance();
         URI resource = factory.createURI(properties.getProperty("about"));
         URI type = factory.createURI("https://www.w3.org/TR/rdf-schema/#ch_type");
         Literal value = factory.createLiteral(docType);
         Statement statement = factory.createStatement(resource, type, value);
         rdfDocument.add(statement);
      }

      String solrContent = content;
      if (solrContent.length() > 500) {
         solrContent = solrContent.substring(0, 500) + "...";
      }
      properties.put("content", solrContent);
      properties.put("label", labels);
      properties.put("concept", concepts);
      properties.put("docType", docType);
      properties.put("creator", DOCUMENT_CREATOR);

      return properties;
   }

   @Override
   public void enhance(String title, String content, String docType) throws Exception {

      String urn = null;
      Properties properties = null;

      try {

         // Send content to Stanbol for enhancement
         String enhancedText = stanbolClient.enhance(configuration.getStanbolChain(), content, StanbolMediaTypes.RDFXML);

         // Parse enhancedText into an rdf document
         List<Statement> rdfDocument = stanbolClient.parseRDF(enhancedText);
         if (rdfDocument != null) {
            // Generate dictionary with properties we are interested in
            properties = generateSolrDocument(rdfDocument, content, docType);
            if (title != null && !title.isEmpty()) {
               properties.setProperty("title", title);
            }

            // Add enhanced document to Solr
            urn = properties.getProperty("about");
            solrClient.addDocument(configuration.getSolrCollection(), properties);

            // Store full enhanced doc (rdf) at S3
            s3Client.storeFile(configuration.getS3Bucket(), configuration.getS3RDFFolder() + urn, enhancedText,
                  ContentType.APPLICATION_XML.getMimeType());

            // Store full document in DB
            Document document = new Document();
            document.setUrn(urn);
            document.setTitle(title);
            document.setType(DocumentType.valueOf(docType));
            document.setContent(content.getBytes());
            documentService.create(document);
         }

      } catch (Exception e) {
         // if there was any error in the process we remove the documents stored under the URN in process
         if (urn != null) {
            rollbackEnhancement(urn);
         }
         throw e;
      }
   }

   @Override
   public void reindexDocument(String urn, boolean enhance) throws Exception {

      String enhancedText = null;
      Properties properties = null;
      Document document = documentService.findByUrn(urn);

      if (document == null) {
         throw new RuntimeException("No document was found under " + urn);
      }

      // Post to Stanbol for enhancement again
      if (enhance) {
         enhancedText = stanbolClient.enhance(configuration.getStanbolChain(), new String(document.getContent()),
               StanbolMediaTypes.RDFXML);

      // Use cached (already enhanced) version of the document from s3
      } else {
         enhancedText = new String(s3Client.getFile(configuration.getS3Bucket(), configuration.getS3RDFFolder() + urn));
      }

      // Parse enhancedText into an rdf document
      List<Statement> rdfDocument = stanbolClient.parseRDF(enhancedText);
      if (rdfDocument != null) {
         // Generate dictionary with properties we are interested in
         properties = generateSolrDocument(rdfDocument, new String(document.getContent()), document.getType().name());
         if (document.getTitle() != null && !document.getTitle().isEmpty()) {
            properties.setProperty("title", document.getTitle());
         }

         // Reindex enhanced document in Solr
         urn = properties.getProperty("about");
         solrClient.addDocument(configuration.getSolrCollection(), properties);
      }
   }

   private void rollbackEnhancement(String urn) throws Exception {
      // Delete document from database
      documentService.deleteByUrn(urn);

      // Delete document from S3
      s3Client.deleteFile(configuration.getS3Bucket(), configuration.getS3RDFFolder() + urn);

      // Delete document from Solr
      solrClient.deleteDocument(configuration.getSolrCollection(), urn);
   }

}
