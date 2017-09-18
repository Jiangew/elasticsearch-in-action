package me.jiangew.elasticsearch.client.high;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jiangew.elasticsearch.client.ClientBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;

/**
 * Desc: High Level Client Delete API Test
 * <p>
 * Author: Jiangew
 * Date: 15/09/2017
 */
public class DeleteAPI {
    private static final Log log = LogFactory.getLog(DeleteAPI.class);

    private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static RestHighLevelClient client;

    static {
        RestClientBuilder builder = ClientBuilder.builder();
        builder = ClientBuilder.setHttpClientConfigCallback(builder);

        client = new RestHighLevelClient(builder.build());
    }

    public DeleteRequest buildRequest() {
        return new DeleteRequest("twitter", "doc", "1");
    }

    public DeleteRequest setRequestArguments(DeleteRequest request) {
        request.routing("routing");
        request.parent("parent");

        request.timeout(TimeValue.timeValueMinutes(2));
        request.timeout("2m");

        request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
        request.setRefreshPolicy("wait_for");

        request.version(2);
        request.versionType(VersionType.EXTERNAL);

        return request;
    }

    public DeleteResponse execSync(DeleteRequest request) throws IOException {
        return client.delete(request);
    }

    public void execAsync(DeleteRequest request) throws IOException {
        client.deleteAsync(request, new ActionListener<DeleteResponse>() {
            @Override
            public void onResponse(DeleteResponse deleteResponse) {

            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    public void handleResponse(DeleteResponse deleteResponse) throws IOException {
        String index = deleteResponse.getIndex();
        String type = deleteResponse.getType();
        String id = deleteResponse.getId();
        long version = deleteResponse.getVersion();

        ReplicationResponse.ShardInfo shardInfo = deleteResponse.getShardInfo();
        if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
            // Handle the situation where number of successful shards is less than total shards
        }
        if (shardInfo.getFailed() > 0) {
            for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
                // Handle the potential failures
                String reason = failure.reason();
            }
        }
    }

    public void handleDocNotExsit() throws IOException {
        DeleteRequest request = new DeleteRequest("twitter", "doc", "does_not_exist");
        DeleteResponse deleteResponse = client.delete(request);
        if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
            // do something
        }
    }

    public void handleVersionConflict() throws IOException {
        try {
            DeleteRequest request = new DeleteRequest("twitter", "doc", "1").version(2);
            DeleteResponse deleteResponse = client.delete(request);
        } catch (ElasticsearchException exception) {
            if (exception.status() == RestStatus.CONFLICT) {
                // If there is a version conflict, an ElasticsearchException will be thrown
            }
        }
    }

}
