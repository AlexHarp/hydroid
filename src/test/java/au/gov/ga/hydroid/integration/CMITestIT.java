package au.gov.ga.hydroid.integration;

import au.gov.ga.hydroid.HydroidApplication;
import au.gov.ga.hydroid.dto.CmiNodeSummary;
import com.google.gson.Gson;
import org.apache.jena.ext.com.google.common.reflect.TypeToken;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by u24529 on 21/04/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(HydroidApplication.class)
@IntegrationTest
public class CMITestIT {

   @Test
   public void testGetCMINodesSummary() {

      String cmiSummaryEndpoint = "http://52.64.197.68/cmi_summary";
//      String cmiSummaryEndpoint = "src/test/resources/testfiles/cmi_summary_test.json";

      try {
         Gson cmiGson = new Gson();
         List<CmiNodeSummary> cmiNodes = new ArrayList<>();
         cmiNodes = cmiGson.fromJson(org.apache.commons.io.IOUtils.toString(new URL(cmiSummaryEndpoint), StandardCharsets.UTF_8), new TypeToken<List<CmiNodeSummary>>(){}.getType());
//            cmiNodes = cmiGson.fromJson(new FileReader(cmiSummaryEndpoint), new TypeToken<List<CmiNodeSummary>>(){}.getType());
         cmiNodes.forEach(System.out::println);


//          String cmiNodeEndpoint = "src/test/resources/testfiles/cmi_test.json";
//          InputStream jsonInStream = new FileInputStream(cmiNodeEndpoint);
//          String nodeJson = new String(Files.readAllBytes(Paths.get(cmiNodeEndpoint)));
      }
      catch (IOException ioe) {
         Assert.fail(ioe.getMessage());
      }

   }


}
