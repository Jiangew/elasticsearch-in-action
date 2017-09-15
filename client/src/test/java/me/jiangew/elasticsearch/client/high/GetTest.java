package me.jiangew.elasticsearch.client.high;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jiangew.elasticsearch.client.ClientBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;

import java.io.IOException;
import java.util.Map;

/**
 * Desc: High Level Client Get API Test
 * <p>
 * Author: Jiangew
 * Date: 15/09/2017
 */
public class GetTest {
    private static final Log log = LogFactory.getLog(GetTest.class);

    private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static RestHighLevelClient client;

    static {
        RestClientBuilder builder = ClientBuilder.builder();
        builder = ClientBuilder.setHttpClientConfigCallback(builder);

        client = new RestHighLevelClient(builder.build());
    }

    public GetRequest buildReuqest() {
        return new GetRequest("twitter", "doc", "1");
    }

    public GetRequest setRequestArguments(GetRequest request) throws IOException {
        // Disable source retrieval, enabled by default
        request.fetchSourceContext(new FetchSourceContext(false));

        // Configure source inclusion for specific fields
        String[] includesA = new String[]{"message", "*Date"};
        String[] excludesA = Strings.EMPTY_ARRAY;
        request.fetchSourceContext(new FetchSourceContext(true, includesA, excludesA));

        // Configure source exclusion for specific fields
        String[] includesB = Strings.EMPTY_ARRAY;
        String[] excludesB = new String[]{"message"};
        request.fetchSourceContext(new FetchSourceContext(true, includesB, excludesB));

        // Configure retrieval for specific stored fields (requires fields to be stored separately in the mappings)
        // Retrieve the message stored field (requires the field to be stored separately in the mappings)
        request.storedFields("message");
        GetResponse getResponse = client.get(request);
        String message = (String) getResponse.getField("message").getValue();

        request.routing("routing");
        request.parent("parent");
        request.preference("preference");
        request.realtime(false);
        request.refresh(true);
        request.version(2);
        request.versionType(VersionType.EXTERNAL);

        return request;
    }

    public GetResponse execSync(GetRequest request) throws IOException {
        return client.get(request);
    }

    public void execAsync(GetRequest request) throws IOException {
        client.getAsync(request, new ActionListener<GetResponse>() {
            @Override
            public void onResponse(GetResponse getFields) {

            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    public void handleResponse(GetResponse getResponse) throws IOException {
        String index = getResponse.getIndex();
        String type = getResponse.getType();
        String id = getResponse.getId();

        if (getResponse.isExists()) {
            long version = getResponse.getVersion();
            String sourceAsString = getResponse.getSourceAsString();
            Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();
            byte[] sourceAsBytes = getResponse.getSourceAsBytes();
        } else {
            // Handle the scenario where the document was not found.
            // Note that although the returned response has 404 status code,
            // a valid GetResponse is returned rather than an exception thrown.
        }
    }

    public void handleIndexNotExist() throws IOException {
        GetRequest request = new GetRequest("does_not_exist", "doc", "1");
        try {
            GetResponse getResponse = client.get(request);
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.NOT_FOUND) {
                // Handle the exception thrown because the index does not exist
            }
        }
    }

    public void handleVersionConflict() throws IOException {
        try {
            GetRequest request = new GetRequest("twitter", "doc", "1").version(2);
            GetResponse getResponse = client.get(request);
        } catch (ElasticsearchException exception) {
            if (exception.status() == RestStatus.CONFLICT) {
                // In case a specific document version has been requested,
                // and the existing document has a different version number, a version conflict is raised
            }
        }
    }

}
