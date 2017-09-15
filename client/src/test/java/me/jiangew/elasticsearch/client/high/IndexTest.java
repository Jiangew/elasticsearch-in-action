package me.jiangew.elasticsearch.client.high;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jiangew.elasticsearch.client.ClientBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Desc: High Level Client Index API Test
 * <p>
 * Author: Jiangew
 * Date: 15/09/2017
 */
public class IndexTest {
    private static final Log log = LogFactory.getLog(IndexTest.class);

    private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static RestHighLevelClient client;

    static {
        RestClientBuilder builder = ClientBuilder.builder();
        builder = ClientBuilder.setHttpClientConfigCallback(builder);

        client = new RestHighLevelClient(builder.build());
    }

    public IndexRequest buildDocSourceWithMap() {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("user", "JamesiWork");
        jsonMap.put("postDate", new Date());
        jsonMap.put("message", "Elasticsearch Logstash Kibana 2017.09.15");

        return new IndexRequest("twitter", "doc", "11").source(jsonMap);
    }

    public IndexRequest buildDocSourceWithJsonBuilder() throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            builder.field("user", "JamesiWork");
            builder.field("postDate", new Date());
            builder.field("message", "Elasticsearch Logstash Kibana 2017.09.15");
        }
        builder.endObject();

        return new IndexRequest("twitter", "doc", "12").source(builder);
    }

    public IndexRequest buildDocSourceWithObjKv() {
        return new IndexRequest("twitter", "doc", "13")
                .source(
                        "user", "JamesiWork",
                        "postDate", new Date(),
                        "message", "Elasticsearch Logstash Kibana 2017.09.15"
                );
    }

    public IndexRequest setRequestArguments(IndexRequest request) {
        request.routing("routing");
        request.parent("parent");

        request.timeout("1s");
        request.timeout(TimeValue.timeValueSeconds(1));

        request.setRefreshPolicy("wait_for");
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);

        request.version(2);
        request.versionType(VersionType.EXTERNAL);

        request.opType("create");
        request.opType(DocWriteRequest.OpType.CREATE);

        request.setPipeline("pipeline");

        return request;
    }

    public IndexResponse execSync(IndexRequest request) throws IOException {
        return client.index(request);
    }

    public void execAsync(IndexRequest request) throws IOException {
        client.indexAsync(request, new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse indexResponse) {

            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    public void handleResponse(IndexResponse indexResponse) {
        String index = indexResponse.getIndex();
        String type = indexResponse.getType();
        String id = indexResponse.getId();
        long version = indexResponse.getVersion();

        if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
            // Handle (if needed) the case where the document was created for the first time
        } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
            // Handle (if needed) the case where the document was rewritten as it was already existing
        }

        ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
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

    public void handleVersionConflict(IndexRequest request) throws IOException {
        request.version(1);
        try {
            IndexResponse response = client.index(request);
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.CONFLICT) {
                // If there is a version conflict, an ElasticsearchException will be thrown
            }
        }
    }

    public void handleOpTypeConflict(IndexRequest request) throws IOException {
        request.opType(DocWriteRequest.OpType.CREATE);
        try {
            IndexResponse response = client.index(request);
        } catch (ElasticsearchException e) {
            if (e.status() == RestStatus.CONFLICT) {
                // Same will happen in case opType was set to create and a document with same index, type and id already existed
            }
        }
    }

}
