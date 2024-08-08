package com.mycorp.apikey.templates;

import static org.apache.http.impl.client.HttpClients.createDefault;
import static org.apache.http.util.TextUtils.isEmpty;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

public class FoodClient implements AutoCloseable {

  private final CloseableHttpClient client;
  // Full HTTP Request: https://www.themealdb.com/api/json/v1/1/random.php
  public static final String BASE_URL = "https://www.themealdb.com/api/json/v1/";

  FoodClient() {
    client = createDefault();
  }

  public CloseableHttpResponse execute(String apiKey, String foodItem)
      throws IOException, URISyntaxException {
    HttpGet getRequest = new HttpGet();
    try {
      URI uri = constructRequest(apiKey, foodItem);
      getRequest.setURI(uri);
      return client.execute(getRequest);
    } finally {
      // Testing to see if this works: getRequest.releaseConnection();
    }
  }

  private URI constructRequest(String apiKey, String foodItem)
      throws URISyntaxException {

    // if foodItem is provided, then make a call to the /search.php endpoint. Otherwise,
    // make a call to the /random.php endpoint.

    if (!isEmpty(foodItem)) {
      return new URIBuilder(BASE_URL + apiKey + "/search.php")
          .addParameter("s",foodItem)
          .build();
    } else {
      return new URIBuilder(BASE_URL + apiKey + "/random.php").build();
    }
  }

  @Override
  public void close() {
    HttpClientUtils.closeQuietly(client);
  }
}
