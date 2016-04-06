package au.gov.ga.hydroid.service;

import java.util.Properties;

/**
 * Created by u24529 on 3/02/2016.
 */
public interface SolrClient {

   public void addDocument(String collectionName, Properties properties);
   public void deleteDocument(String collectionName, String id);
   public void deleteAll(String collectionName);

}
