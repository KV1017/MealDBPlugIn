package com.mycorp.apikey.templates;

import static com.mycorp.apikey.templates.FoodAPIKeyConnectedSystemTemplate.FOOD_API_KEY;


import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationError;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateRequestPolicy;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
// Must provide an integration id. This value need only be unique for this connected system
@TemplateId(name="FoodSearchIntegrationTemplate")
// Set template type to READ since this integration does not have side effects
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.READ)
public class FoodSearchIntegrationTemplate extends SimpleIntegrationTemplate {

  @Override
  protected SimpleConfiguration getConfiguration(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      PropertyPath propertyPath,
      ExecutionContext executionContext) {
    return integrationConfiguration.setProperties(
        // Create user input field for foodItem
        textProperty("foodItem")
            .label("Search Field")
            .instructionText("Please enter a name of a dish you'd like to learn about.")
            .description("This will return a description of the dish and a recipe.")
            .placeholder("Pizza")
            .isRequired(true)
            .isExpressionable(true)
            .build()
    );
  }



  @Override
  protected IntegrationResponse execute(
      SimpleConfiguration integrationConfiguration,
      SimpleConfiguration connectedSystemConfiguration,
      ExecutionContext executionContext) {
    // The apiKey is stored in the Connected System and all integrations for that Connected Systems will share credentials
    String apiKey = connectedSystemConfiguration.getValue(FOOD_API_KEY);
    String foodItem = integrationConfiguration.getValue("foodItem");

    IntegrationResponse.Builder integrationResponseBuilder;
    CloseableHttpResponse httpResponse = null;
    try (FoodClient foodClient = new FoodClient()){

      // The amount of time it takes to interact with the external
      // system will be displayed to the end user
      long startTime = System.currentTimeMillis();

      // Execute call to the Meals DB API
      httpResponse = foodClient.execute(apiKey, foodItem);
      long endTime = System.currentTimeMillis();
      long executionTime = endTime - startTime;
      String jsonResponse = EntityUtils.toString(httpResponse.getEntity());

      // Determine if call was successful
      if (httpResponse.getStatusLine().getStatusCode() == 200) {
        // Success Condition
        Map<String,Object> responseMap = getResponseMap(jsonResponse);
        integrationResponseBuilder = IntegrationResponse.forSuccess(responseMap);
      } else {
        // Builds response for error case
        IntegrationError error = httpResponseError(httpResponse);
        integrationResponseBuilder = IntegrationResponse.forError(error);
      }

      // Returns request and response information to troubleshoot potential issues with the integration
      Map<String,Object> requestDiagnostic = getRequestDiagnostic(apiKey, foodItem);
      Map<String,Object> responseDiagnostic = getResponseDiagnostic(jsonResponse);
      IntegrationDesignerDiagnostic integrationDesignerDiagnostic = IntegrationDesignerDiagnostic.builder()
          .addRequestDiagnostic(requestDiagnostic)
          .addResponseDiagnostic(responseDiagnostic)
          .addExecutionTimeDiagnostic(executionTime)
          .build();
      return integrationResponseBuilder.withDiagnostic(integrationDesignerDiagnostic).build();
    } catch (URISyntaxException | IOException e) {
      //Builds default response for unknown error case
      IntegrationError error = templateError();
      return IntegrationResponse.forError(error).build();
    } finally {
      //Closes Http Response
      HttpClientUtils.closeQuietly(httpResponse);
    }
  }

  private Map<String, Object> getRequestDiagnostic(String apiKey, String foodItem) {
    Map<String, Object> diagnostic = new HashMap<>();
    diagnostic.put("Url", FoodClient.BASE_URL);
    diagnostic.put("API Key", apiKey);
    diagnostic.put("Searched Food Item", foodItem);
    return diagnostic;
  }

  private Map<String, Object> getResponseDiagnostic(String jsonString) {
    Map<String, Object> diagnostic = new HashMap<>();
    diagnostic.put("Raw Response", jsonString);
    return diagnostic;
  }


  private IntegrationError httpResponseError(CloseableHttpResponse httpResponse) {
    return IntegrationError.builder()
        .title("Received an error Response")
        .message("Status Code: " + httpResponse.getStatusLine().getStatusCode())
        .build();
  }

  private IntegrationError templateError() {
    return IntegrationError.builder()
        .title("Something went wrong")
        .message("An error occurred in the IntegrationTemplate")
        .build();
  }

  private Map<String,Object> getResponseMap(String jsonResponse) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String,Object> mealsDBResponseMap = objectMapper.readValue(jsonResponse,
        new TypeReference<HashMap<String,Object>>() {
        });
    return mealsDBResponseMap;
  }
}
