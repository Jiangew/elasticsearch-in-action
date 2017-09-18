package me.jiangew.elasticsearch.client.high;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jiangew.elasticsearch.client.ClientBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.Build;
import org.elasticsearch.Version;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.ClusterName;

import java.io.IOException;

/**
 * Desc: High Level Client Info API Test
 * <p>
 * Author: Jiangew
 * Date: 18/09/2017
 */
public class InfoAPI {
    private static final Log log = LogFactory.getLog(InfoAPI.class);

    private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static RestHighLevelClient client;

    static {
        RestClientBuilder builder = ClientBuilder.builder();
        builder = ClientBuilder.setHttpClientConfigCallback(builder);

        client = new RestHighLevelClient(builder.build());
    }

    public void getClusterInfo() throws IOException {
        MainResponse response = client.info();

        ClusterName clusterName = response.getClusterName();
        String clusterUuid = response.getClusterUuid();
        String nodeName = response.getNodeName();
        Version version = response.getVersion();
        Build build = response.getBuild();
    }

}
