package me.jiangew.elasticsearch.client.high;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jiangew.elasticsearch.client.ClientBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Desc: High Level Client Bulk API Test
 * <p>
 * Author: Jiangew
 * Date: 15/09/2017
 */
public class BulkAPI {
    private static final Log logger = LogFactory.getLog(BulkAPI.class);

    private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static RestHighLevelClient client;

    static {
        RestClientBuilder builder = ClientBuilder.builder();
        builder = ClientBuilder.setHttpClientConfigCallback(builder);

        client = new RestHighLevelClient(builder.build());
    }

    public BulkRequest buildRequest() {
        BulkRequest request = new BulkRequest();

        request.add(new IndexRequest("twitter", "doc", "21")
                            .source(XContentType.JSON,
                                    "user", "JamesiWork",
                                    "postDate", new Date(),
                                    "message", "Elasticsearch Logstash Kibana 2017.09.15")
        );

        request.add(new UpdateRequest("twitter", "doc", "21")
                            .doc(XContentType.JSON, "user", "JamesiWork")
        );

        request.add(new DeleteRequest("twitter", "doc", "21"));

        request.timeout(TimeValue.timeValueMinutes(2));
        request.timeout("2m");

        request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
        request.setRefreshPolicy("wait_for");

        // Sets the number of shard copies that must be active before proceeding with the index/update/delete operations.
        request.waitForActiveShards(2);
        request.waitForActiveShards(ActiveShardCount.ALL);

        return request;
    }

    public BulkResponse execSync(BulkRequest request) throws IOException {
        return client.bulk(request);
    }

    public void execAsync(BulkRequest request) throws IOException {
        client.bulkAsync(request, new ActionListener<BulkResponse>() {
            @Override
            public void onResponse(BulkResponse bulkItemResponses) {

            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    public void handleResponse(BulkResponse bulkResponse) throws IOException {
        if (bulkResponse.hasFailures()) {
            // do ...
        }

        for (BulkItemResponse bulkItemResponse : bulkResponse) {
            if (bulkItemResponse.isFailed()) {
                BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
            }

            DocWriteResponse itemResponse = bulkItemResponse.getResponse();
            if (bulkItemResponse.getOpType() == DocWriteRequest.OpType.INDEX || bulkItemResponse.getOpType() == DocWriteRequest.OpType.CREATE) {
                IndexResponse indexResponse = (IndexResponse) itemResponse;
            } else if (bulkItemResponse.getOpType() == DocWriteRequest.OpType.UPDATE) {
                UpdateResponse updateResponse = (UpdateResponse) itemResponse;
            } else if (bulkItemResponse.getOpType() == DocWriteRequest.OpType.DELETE) {
                DeleteResponse deleteResponse = (DeleteResponse) itemResponse;
            }
        }
    }

    /**
     * Bulk Processor 3 elements
     * 01 RestHighLevelClient: This client is used to execute the BulkRequest and to retrieve the BulkResponse
     * 02 BulkProcessor.Listener: This listener is called before and after every BulkRequest execution or when a BulkRequest failed
     * 03 ThreadPool: The BulkRequest executions are done using threads from this pool, allowing the BulkProcessor to work in a non-blocking manner
     */
    public void process() throws IOException, InterruptedException {
        BulkProcessor.Listener listener = new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(long id, BulkRequest bulkRequest) {
                int numberOfActions = bulkRequest.numberOfActions();
                logger.debug(String.format("Executing bulk %s with %s requests", id, numberOfActions));
            }

            @Override
            public void afterBulk(long id, BulkRequest bulkRequest, BulkResponse bulkResponse) {
                if (bulkResponse.hasFailures()) {
                    logger.warn(String.format("Bulk %s executed with failures", id));
                } else {
                    logger.debug(String.format("Bulk %s completed in %s milliseconds", id, bulkResponse.getTook().getMillis()));
                }
            }

            @Override
            public void afterBulk(long id, BulkRequest bulkRequest, Throwable throwable) {
                logger.error("Failed to execute bulk", throwable);
            }
        };

        // ThreadPool Settings
        BulkProcessor.Builder builder = new BulkProcessor.Builder(client::bulkAsync, listener, null);
        builder.setBulkActions(500);
        builder.setBulkSize(new ByteSizeValue(1L, ByteSizeUnit.MB));
        builder.setConcurrentRequests(0);
        builder.setFlushInterval(TimeValue.timeValueSeconds(10L));
        builder.setBackoffPolicy(BackoffPolicy.constantBackoff(TimeValue.timeValueSeconds(1L), 3));

        IndexRequest one = new IndexRequest("twitter", "doc", "21")
                .source(XContentType.JSON,
                        "user", "JamesiWork",
                        "postDate", new Date(),
                        "message", "Elasticsearch Logstash Kibana 2017.09.15");

        UpdateRequest two = new UpdateRequest("twitter", "doc", "21")
                .doc(XContentType.JSON, "user", "JamesiWork");

        DeleteRequest three = new DeleteRequest("twitter", "doc", "21");

        BulkProcessor bulkProcessor = builder.build();
        bulkProcessor.add(one);
        bulkProcessor.add(two);
        bulkProcessor.add(three);

        // awaitClose() method can be used to wait until all requests have been processed or the specified waiting time elapses
        boolean terminated = bulkProcessor.awaitClose(30L, TimeUnit.SECONDS);

        // close() method can be used to immediately close the BulkProcessor
        bulkProcessor.close();
    }

}
