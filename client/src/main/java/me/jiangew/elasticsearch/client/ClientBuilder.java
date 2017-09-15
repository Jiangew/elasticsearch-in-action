package me.jiangew.elasticsearch.client;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

/**
 * Desc: Client Builder
 * <p>
 * Author: Jiangew
 * Date: 14/09/2017
 */
public class ClientBuilder {

    public static RestClient client() {

        return RestClient.builder(
                new HttpHost("localhost", 9200, "http"),
                new HttpHost("localhost", 9201, "http")
        ).build();
    }

    public static RestClientBuilder builder() {
        return RestClient.builder(new HttpHost("localhost", 9200, "http"));
    }

    /**
     * Set the default headers that need to be sent with each request
     *
     * @param builder
     * @return
     */
    public static RestClientBuilder setDefaultHeaders(RestClientBuilder builder) {
        Header[] defaultHeaders = new Header[]{new BasicHeader("Content-Type", "application/json")};
        builder.setDefaultHeaders(defaultHeaders);

        return builder;
    }

    /**
     * Set the timeout that should be honoured in case multiple attempts are made for the same request
     *
     * @param builder
     * @return
     */
    public static RestClientBuilder setMaxRetryTimeoutMillis(RestClientBuilder builder) {
        return builder.setMaxRetryTimeoutMillis(10000);
    }

    /**
     * Set a listener that gets notified every time a node fails, in case actions need to be taken.
     *
     * @param builder
     * @return
     */
    public static RestClientBuilder setFailureListener(RestClientBuilder builder) {
        return builder.setFailureListener(new RestClient.FailureListener() {
            @Override
            public void onFailure(HttpHost host) {

            }
        });
    }

    /**
     * Set a callback that allows to modify the default request configuration
     *
     * @param builder
     * @return
     */
    public static RestClientBuilder setRequestConfigCallback(RestClientBuilder builder) {
        return builder.setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
            @Override
            public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder requestConfigBuilder) {
                return requestConfigBuilder.setSocketTimeout(10000);
            }
        });
    }

    /**
     * Set a callback that allows to modify the http client configuration
     *
     * @param builder
     * @return
     */
    public static RestClientBuilder setHttpClientConfigCallback(RestClientBuilder builder) {
        // Basic authentication
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "elastic"));

        // Set a callback that allows to modify the http client configuration
        builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                // proxy
//                httpClientBuilder.setProxy(new HttpHost("proxy", 9000, "http"));

                // The Apache Http Async Client starts by default one dispatcher thread,
                // and a number of worker threads used by the connection manager,
                // as many as the number of locally detected processors (depending on what Runtime.getRuntime().availableProcessors() returns).
//                httpClientBuilder.setDefaultIOReactorConfig(IOReactorConfig.custom().setIoThreadCount(1).build());

                // Preemptive Authentication can be disabled,
                // which means that every request will be sent without authorization headers to see if it is accepted and,
                // upon receiving a HTTP 401 response, it will resend the exact same request with the basic authentication header.
                httpClientBuilder.disableAuthCaching();
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);

                return httpClientBuilder;
            }
        });

        return builder;
    }

}
