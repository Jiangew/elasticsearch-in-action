package me.jiangew.elasticsearch.client.high;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jiangew.elasticsearch.client.ClientBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Desc: High Level Client Update API Test
 * <p>
 * Author: Jiangew
 * Date: 15/09/2017
 */
public class UpdateAPI {
    private static final Log log = LogFactory.getLog(UpdateAPI.class);

    private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static RestHighLevelClient client;

    static {
        RestClientBuilder builder = ClientBuilder.builder();
        builder = ClientBuilder.setHttpClientConfigCallback(builder);

        client = new RestHighLevelClient(builder.build());
    }

    public UpdateRequest buildRequest() {
        return new UpdateRequest("twitter", "doc", "1");
    }

    public UpdateRequest setRequestDocWithString(UpdateRequest request) {
        String json = "{" +
                "\"updated\":\"2017-01-01\"," +
                "\"reason\":\"daily update\"" +
                "}";

        return request.doc(json, XContentType.JSON);
    }

    public UpdateRequest setRequestDocWithMap(UpdateRequest request) {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("updated", new Date());
        jsonMap.put("reason", "daily update");

        return request.doc(jsonMap);
    }

    public UpdateRequest setRequestDocWithJsonBuilder(UpdateRequest request) throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            builder.field("updated", new Date());
            builder.field("reason", "daily update");
        }
        builder.endObject();

        return request.doc(builder);
    }

    public UpdateRequest setRequestDocWithObjKv(UpdateRequest request) throws IOException {
        return new UpdateRequest("twitter", "doc", "1")
                .doc("updated", new Date(),
                     "reason", "daily update");
    }

    public UpdateRequest setRequestArguments(UpdateRequest request) {
        request.routing("routing");
        request.parent("parent");

        request.timeout(TimeValue.timeValueSeconds(1));
        request.timeout("1s");

        request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
        request.setRefreshPolicy("wait_for");

        request.retryOnConflict(3);

        request.version(2);

        // Disable the noop detection
        request.detectNoop(false);

        // Indicate that the script must run regardless of whether the document exists or not,
        // ie the script takes care of creating the document if it does not already exist.
        request.scriptedUpsert(true);

        // Indicate that the partial document must be used as the upsert document if it does not exist yet.
        request.docAsUpsert(true);

        // Sets the number of shard copies that must be active before proceeding with the update operation.
        request.waitForActiveShards(2);
        request.waitForActiveShards(ActiveShardCount.ALL);

        // Enable source retrieval, disabled by default
        request.fetchSource(true);

        // Configure source inclusion for specific fields
        String[] includes = new String[]{"updated", "r*"};
        String[] excludes = Strings.EMPTY_ARRAY;
        request.fetchSource(new FetchSourceContext(true, includes, excludes));

        // Configure source exclusion for specific fields
        String[] includesB = Strings.EMPTY_ARRAY;
        String[] excludesB = new String[]{"updated"};
        request.fetchSource(new FetchSourceContext(true, includesB, excludesB));

        return request;
    }

    public UpdateResponse execSync(UpdateRequest request) throws IOException {
        return client.update(request);
    }

    public void execAsync(UpdateRequest request) throws IOException {
        client.updateAsync(request, new ActionListener<UpdateResponse>() {
            @Override
            public void onResponse(UpdateResponse updateResponse) {

            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    public void handleResponse(UpdateResponse updateResponse) {
        String index = updateResponse.getIndex();
        String type = updateResponse.getType();
        String id = updateResponse.getId();
        long version = updateResponse.getVersion();

        if (updateResponse.getResult() == DocWriteResponse.Result.CREATED) {
            // created
        } else if (updateResponse.getResult() == DocWriteResponse.Result.UPDATED) {
            // updated
        } else if (updateResponse.getResult() == DocWriteResponse.Result.DELETED) {
            // delete
        } else if (updateResponse.getResult() == DocWriteResponse.Result.NOOP) {
            // noop: no operation
        }

        // When the source retrieval is enabled in the UpdateRequest through the fetchSource method,
        // the response contains the source of the updated document
        GetResult result = updateResponse.getGetResult();
        if (result.isExists()) {
            // Retrieve the source of the updated document as a String
            String sourceAsString = result.sourceAsString();
            // Retrieve the source of the updated document as a Map<String, Object>
            Map<String, Object> sourceAsMap = result.sourceAsMap();
            // Retrieve the source of the updated document as a byte[]
            byte[] sourceAsBytes = result.source();
        } else {
            // do something
        }

        ReplicationResponse.ShardInfo shardInfo = updateResponse.getShardInfo();
        if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
            // do ...
        }
        if (shardInfo.getFailed() > 0) {
            for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
                String reason = failure.reason();
            }
        }

    }

}
