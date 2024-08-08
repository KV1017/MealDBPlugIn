package com.mycorp.apikey.templates;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.simplified.sdk.connectiontesting.SimpleTestableConnectedSystemTemplate;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.connectiontesting.TestConnectionResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@TemplateId(name="FoodAPIKeyConnectedSystemTemplate")
public class FoodAPIKeyConnectedSystemTemplate extends SimpleTestableConnectedSystemTemplate {

  static String FOOD_API_KEY = "apiKey";

  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration simpleConfiguration, ExecutionContext executionContext) {
    return simpleConfiguration.setProperties(
        // Sensitive values should use an encryptedTextProperty
        encryptedTextProperty(FOOD_API_KEY)
            .label("API Key")
            .instructionText("Don't have an API Key? See https://www.themealdb.com/api.php for instructions to generate a key to access the MealDB API.")
            .build()
    );
  }

  @Override
  // Validates if connection can be made to the MealDB API via GET Request
  protected TestConnectionResult testConnection(
      SimpleConfiguration simpleConfiguration, ExecutionContext executionContext) {
    String apiKey = simpleConfiguration.getValue(FOOD_API_KEY);
    try (FoodClient foodClient = new FoodClient()) {
      CloseableHttpResponse response = foodClient.execute(apiKey, "");

      //Read entity directly to json string
      HttpEntity entity = response.getEntity();
      String jsonResponse = EntityUtils.toString(entity);
      EntityUtils.consume(entity);
      Map<String, Object> responseMap = getResponseMap(jsonResponse);

      //Determine if MealDB API returned an error
      if (response.getStatusLine().getStatusCode() != 200) {
        return TestConnectionResult.error("There was an issue with your request. Please look into this issue.");
      }
      // On Success
      return TestConnectionResult.success();
    } catch(IOException | URISyntaxException e) {
      return TestConnectionResult.error("Something went wrong: " + e.getMessage());
    }
  }

  //Deserialize JSON to Java Map
  private Map<String,Object> getResponseMap(String jsonResponse) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String,Object> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<HashMap<String,Object>>() {});
    return responseMap;
  }
}
