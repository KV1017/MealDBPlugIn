package com.mycorp.apikey.templates;

import static com.mycorp.apikey.templates.FoodAPIKeyConnectedSystemTemplate.FOOD_API_KEY;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;


import org.apache.http.client.methods.CloseableHttpResponse;

import org.apache.http.util.EntityUtils;

import com.appian.connectedsystems.simplified.sdk.SimpleClientApi;
import com.appian.connectedsystems.simplified.sdk.SimpleClientApiRequest;
import com.appian.connectedsystems.templateframework.sdk.ClientApiResponse;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;



/**
 * This is an example of a Client API that performs an operation when executed from a
 * Component Plug-in (CP). The Client API accepts a data structure which contains
 * the CP request payload as well as the data stored inside the Connected System object.
 * It uses both pieces to perform the operation.
 *
 * In this example, the Connected System stores a secret value, the Google API key, which
 * the Client API uses to submit a request to the Google Text Detection API. The Client API
 * then parses the response and returns a map of coordinates and the text found back to the CP.
 */

@TemplateId(name = "FoodSearchClientAPI")
public class FoodSearchClientAPI extends SimpleClientApi {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(
      DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  // Payload key in CP request
  private static final String FOOD_ITEM_KEY = "foodItem";

  @Override
  protected ClientApiResponse execute(
      SimpleClientApiRequest simpleClientApiRequest, ExecutionContext executionContext) {


    // The apiKey is stored in the Connected System and all integrations for that Connected Systems will share credentials
    String apiKey = simpleClientApiRequest.getConnectedSystemConfiguration().getValue(FOOD_API_KEY);
    String foodItem = (String)simpleClientApiRequest.getPayload().get(FOOD_ITEM_KEY);
    Map<String,Object> resultMap;
    CloseableHttpResponse httpResponse = null;



    try ( FoodClient client = new FoodClient()) {
      httpResponse = client.execute(apiKey, foodItem);
      String jsonResponse = EntityUtils.toString(httpResponse.getEntity());
      resultMap = getResponseMap(jsonResponse);
    }
    catch (URISyntaxException | IOException e) {
      return new ClientApiResponse(new HashMap<>());
    }

    return new ClientApiResponse(resultMap);
  }

  private Map<String,Object> getResponseMap(String jsonResponse) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String,Object> mealsDBResponseMap = objectMapper.readValue(jsonResponse,
        new TypeReference<HashMap<String,Object>>() {
        });
    return mealsDBResponseMap;
  }
}
