package me.jiangew.elasticsearch.client.low;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.jiangew.elasticsearch.client.ClientBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Desc: Low Level Client Test
 * <p>
 * Author: Jiangew
 * Date: 14/09/2017
 */
public class RestClientAPI {
    private static final Log log = LogFactory.getLog(RestClientAPI.class);

    private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    //    private Map<String, String> emptyParams = Collections.emptyMap();
    private Map<String, String> singletonParams = Collections.singletonMap("pretty", "true");

    // Define what needs to happen when the request is successfully performed
    // Define what needs to happen when the request fails, meaning whenever there’s a connection error or a response with error status code is returned.
    private ResponseListener responseListener = new ResponseListener() {
        @Override
        public void onSuccess(Response response) {
            try {
                handleResponse(response);
            } catch (Exception e) {
                log.error("asynchronous response listener onSuccess", e);
            }
        }

        @Override
        public void onFailure(Exception e) {
            log.error("asynchronous response listener onFailure", e);
        }
    };

    // Controls how the response body gets streamed from a non-blocking HTTP connection on the client side.
    // When not provided, the default implementation is used which buffers the whole response body in heap memory, up to 100 MB.
    private HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory consumerFactory = new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(30 * 1024 * 1024);

    private static RestClientBuilder builder;

    static {
        builder = ClientBuilder.builder();
        builder = ClientBuilder.setHttpClientConfigCallback(builder);
    }

    private void handleResponse(Response response) throws Exception {
        final int statusCode = response.getStatusLine().getStatusCode();
        final String responseBody = EntityUtils.toString(response.getEntity());

        assertEquals(200, statusCode, "response code");
//        assertTrue(statusCode == 200, "statusCode: " + statusCode + ",responseBody: " + responseBody);
        log.info(String.format("response body: %s", responseBody));
    }

    @Test
    void handleRequestAsync() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("user", "JamesiWork");
        jsonObject.addProperty("postDate", "2013-01-30");
        jsonObject.addProperty("message", "trying out Elasticsearch");
        HttpEntity entity = new NStringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON);

        RestClient restClient = builder.build();

        final CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            restClient.performRequestAsync(
                    "PUT",
                    "/twitter/doc" + 10 + i,
                    Collections.<String, String>emptyMap(),
                    //let's assume that the documents are stored in an HttpEntity array
                    entity,
                    new ResponseListener() {
                        @Override
                        public void onSuccess(Response response) {
                            // Process the returned response
                            latch.countDown();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            // Handle the returned exception,
                            // due to communication error or a response with status code that indicates an error
                            latch.countDown();
                        }
                    }
            );
        }
        latch.await();
    }

    @Test
    @DisplayName("Get index document by id")
    void performRequestWithParams() throws Exception {
        RestClient restClient = builder.build();
        Response response = restClient.performRequest("GET", "/twitter/doc/1", singletonParams);
        handleResponse(response);

        restClient.close();
    }

    @Test
    @DisplayName("Get index document by id async")
    void performRequestWithParamsAsync() throws Exception {
        RestClient restClient = builder.build();
        restClient.performRequestAsync("GET", "/twitter/doc/1", singletonParams, responseListener);
    }

    @Test
    void performRequestWithHeaders() throws Exception {
        RestClient restClient = builder.build();
        Response response = restClient.performRequest("GET", "/twitter/doc/1", singletonParams, new BasicHeader("Content-Type", "application/json"));
        handleResponse(response);

        restClient.close();
    }

    @Test
    void performRequestWithHeadersAsync() throws Exception {
        RestClient restClient = builder.build();
        restClient.performRequestAsync("GET", "/twitter/doc/1", singletonParams, responseListener, new BasicHeader("Content-Type", "application/json"));
    }

    @Test
    void performRequestWithEntity() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("user", "JamesiWork");
        jsonObject.addProperty("postDate", "2013-02-01");
        jsonObject.addProperty("message", "ELK: Elasticsearch Logstash Kibana");
        HttpEntity entity = new NStringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON);

        builder = ClientBuilder.setDefaultHeaders(builder);
        RestClient restClient = builder.build();
        Response response = restClient.performRequest("PUT", "/twitter/doc/6", singletonParams, entity);
        handleResponse(response);

        restClient.close();
    }

    @Test
    void performRequestWithEntityAsync() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("user", "JamesiWork");
        jsonObject.addProperty("postDate", "2013-02-01");
        jsonObject.addProperty("message", "ELK: Elasticsearch Logstash Kibana");
        HttpEntity entity = new NStringEntity(gson.toJson(jsonObject), ContentType.APPLICATION_JSON);

        builder = ClientBuilder.setDefaultHeaders(builder);
        RestClient restClient = builder.build();
        restClient.performRequestAsync("PUT", "/twitter/doc/6", singletonParams, entity, responseListener);
    }

    @Test
    void performRequestWithConsumerFact() throws Exception {
        RestClient restClient = builder.build();
        // Send a request by providing optional request body,
        // and the optional factory that is used to create an org.apache.http.nio.protocol.HttpAsyncResponseConsumer callback instance per request attempt.
        Response response = restClient.performRequest("GET", "/twitter/_search", singletonParams, null, consumerFactory);
        handleResponse(response);

        restClient.close();
    }

    @Test
    void performRequestWithConsumerFactAsync() throws Exception {
        RestClient restClient = builder.build();
        restClient.performRequestAsync("GET", "/twitter/_search", singletonParams, null, consumerFactory, responseListener);
    }

    @Test
    void performRequestWithEntityConsumerFact() throws Exception {
        String all = "{\n" +
                "    \"query\" : {\n" +
                "        \"match_all\" : {}\n" +
                "    }\n" +
                "}";

        String match = "{\n" +
                "    \"query\" : {\n" +
                "        \"match\" : { \"user\": \"JamesiWork\" }\n" +
                "    }\n" +
                "}";

        String range = "{\n" +
                "    \"query\" : {\n" +
                "        \"range\" : {\n" +
                "            \"post_date\" : { \"from\" : \"2017-01-01T00:00:00\", \"to\" : \"2017-09-14T20:00:00\" }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        HttpEntity entity = new NStringEntity(range, ContentType.APPLICATION_JSON);

        builder = ClientBuilder.setDefaultHeaders(builder);
        RestClient restClient = builder.build();
        // Send a request by providing optional request body,
        // and the optional factory that is used to create an org.apache.http.nio.protocol.HttpAsyncResponseConsumer callback instance per request attempt.
        Response response = restClient.performRequest("GET", "/twitter/_search", singletonParams, entity, consumerFactory);
        handleResponse(response);

        restClient.close();
    }

}
