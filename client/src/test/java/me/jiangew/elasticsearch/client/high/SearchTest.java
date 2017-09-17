package me.jiangew.elasticsearch.client.high;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jiangew.elasticsearch.client.ClientBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.profile.ProfileResult;
import org.elasticsearch.search.profile.ProfileShardResult;
import org.elasticsearch.search.profile.aggregation.AggregationProfileShardResult;
import org.elasticsearch.search.profile.query.CollectorResult;
import org.elasticsearch.search.profile.query.QueryProfileShardResult;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.SuggestionBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestion;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Desc: High Level Client Search API Test
 * <p>
 * Author: Jiangew
 * Date: 15/09/2017
 */
public class SearchTest {
    private static final Log log = LogFactory.getLog(SearchTest.class);

    private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static RestHighLevelClient client;

    static {
        RestClientBuilder builder = ClientBuilder.builder();
        builder = ClientBuilder.setHttpClientConfigCallback(builder);

        client = new RestHighLevelClient(builder.build());
    }

    public SearchRequest buildRequest() throws IOException {
        SearchRequest request = new SearchRequest("twitter");
        request.types("doc");

        request.routing("routing");

        // Setting IndicesOptions controls how unavailable indices are resolved and how wildcard expressions are expanded
        request.indicesOptions(IndicesOptions.lenientExpandOpen());

        // 使用偏好参数 执行搜索以更喜欢本地分片；默认值是随机化分片
        request.preference("_local");

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery("user", "JamesiWork"));
        sourceBuilder.from(0);
        sourceBuilder.size(10);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        // By default, search requests return the contents of the document _source,
        // but like in the Rest API you can overwrite this behavior.
        sourceBuilder.fetchSource(false);

        // 该方法还接受一个或多个通配符模式的数组，以便以更细粒度的方式控制哪些字段包含或排除
        String[] includeFields = new String[]{"title", "user", "innerObject.*"};
        String[] excludeFields = new String[]{"_type"};
        sourceBuilder.fetchSource(includeFields, excludeFields);

        // Highlighting
        HighlightBuilder highlightBuilder = new HighlightBuilder();

        HighlightBuilder.Field title = new HighlightBuilder.Field("title");
        title.highlighterType("unified");
        highlightBuilder.field(title);

        HighlightBuilder.Field user = new HighlightBuilder.Field("user");
        highlightBuilder.field(user);

        sourceBuilder.highlighter(highlightBuilder);

        // Aggregations
        TermsAggregationBuilder aggregation = AggregationBuilders.terms("by_company")
                .field("company.keyword");
        aggregation.subAggregation(AggregationBuilders.avg("average_age")
                                           .field("age"));

        sourceBuilder.aggregation(aggregation);

        // Suggestions
        SuggestionBuilder termSuggestionBuilder = SuggestBuilders.termSuggestion("user").text("JamesiWork");

        SuggestBuilder suggestBuilder = new SuggestBuilder();
        suggestBuilder.addSuggestion("suggest_user", termSuggestionBuilder);

        sourceBuilder.suggest(suggestBuilder);

        // Profiling Queries and Aggregations
        sourceBuilder.profile(true);

        // Sort
        sourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
        sourceBuilder.sort(new FieldSortBuilder("_uid").order(SortOrder.ASC));

        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("user", "JamesiWork");
        matchQueryBuilder.fuzziness(Fuzziness.AUTO);
        matchQueryBuilder.prefixLength(3);
        matchQueryBuilder.maxExpansions(10);

        QueryBuilder queryBuilder = QueryBuilders.matchQuery("user", "JamesiWork")
                .fuzziness(Fuzziness.AUTO)
                .prefixLength(3)
                .maxExpansions(10);

        // 01 query match all
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        // 02 query builder
        sourceBuilder.query(queryBuilder);
        // 03 query match builder
        sourceBuilder.query(matchQueryBuilder);

        request.source(sourceBuilder);
        return request;
    }

    public SearchResponse execSync(SearchRequest request) throws IOException {
        return client.search(request);
    }

    public void execAsync(SearchRequest request) throws IOException {
        client.searchAsync(request, new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {

            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    public void handleResponse(SearchResponse searchResponse) throws IOException {
        RestStatus status = searchResponse.status();
        TimeValue took = searchResponse.getTook();
        Boolean terminatedEarly = searchResponse.isTerminatedEarly();
        boolean timedOut = searchResponse.isTimedOut();

        int totalShards = searchResponse.getTotalShards();
        int successfulShards = searchResponse.getSuccessfulShards();
        int failedShards = searchResponse.getFailedShards();
        for (ShardSearchFailure failure : searchResponse.getShardFailures()) {
            // failures should be handled here
        }

        // Retrieving SearchHits
        SearchHits hits = searchResponse.getHits();
        long totalHits = hits.getTotalHits();
        float maxScore = hits.getMaxScore();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            // do something with SearchHit
            String index = hit.getIndex();
            String type = hit.getType();
            String id = hit.getId();
            float score = hit.getScore();

            String sourceAsString = hit.getSourceAsString();
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String documentTitle = (String) sourceAsMap.get("title");
            List<Object> users = (List<Object>) sourceAsMap.get("user");
            Map<String, Object> innerObject = (Map<String, Object>) sourceAsMap.get("innerObject");

            // Retrieving Highlighting
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField highlight = highlightFields.get("title");
            Text[] fragments = highlight.fragments();
            String fragmentString = fragments[0].string();
        }

        // Retrieving Aggregations
        Aggregations aggregations = searchResponse.getAggregations();
        Terms byCompanyAggregation = aggregations.get("by_company");
        Terms.Bucket elasticBucket = byCompanyAggregation.getBucketByKey("Elastic");
        Avg averageAge = elasticBucket.getAggregations().get("average_age");
        double avg = averageAge.getValue();

        Range range = aggregations.get("by_company");

        Map<String, Aggregation> aggregationMap = aggregations.getAsMap();
        Terms companyAggregation = (Terms) aggregationMap.get("by_company");

        List<Aggregation> aggregationList = aggregations.asList();

        for (Aggregation agg : aggregations) {
            String type = agg.getType();
            if (type.equals(TermsAggregationBuilder.NAME)) {
                elasticBucket = ((Terms) agg).getBucketByKey("Elastic");
                long numberOfDocs = elasticBucket.getDocCount();
            }
        }

        // Retrieving Suggestions
        Suggest suggest = searchResponse.getSuggest();
        TermSuggestion termSuggestion = suggest.getSuggestion("suggest_user");
        for (TermSuggestion.Entry entry : termSuggestion.getEntries()) {
            for (TermSuggestion.Entry.Option option : entry) {
                String suggestText = option.getText().string();
            }
        }

        // Retrieving Profiling Results
        Map<String, ProfileShardResult> profilingResults = searchResponse.getProfileResults();
        for (Map.Entry<String, ProfileShardResult> profilingResult : profilingResults.entrySet()) {
            String key = profilingResult.getKey();
            ProfileShardResult profileShardResult = profilingResult.getValue();
            List<QueryProfileShardResult> queryProfileShardResults = profileShardResult.getQueryProfileResults();
            for (QueryProfileShardResult queryProfileResult : queryProfileShardResults) {

                for (ProfileResult profileResult : queryProfileResult.getQueryResults()) {
                    String queryName = profileResult.getQueryName();
                    long queryTimeInMillis = profileResult.getTime();
                    List<ProfileResult> profiledChildren = profileResult.getProfiledChildren();
                }

                CollectorResult collectorResult = queryProfileResult.getCollectorResult();
                String collectorName = collectorResult.getName();
                Long collectorTimeInMillis = collectorResult.getTime();
                List<CollectorResult> profiledChildren = collectorResult.getProfiledChildren();
            }

            AggregationProfileShardResult aggsProfileResults = profileShardResult.getAggregationProfileResults();
            for (ProfileResult profileResult : aggsProfileResults.getProfileResults()) {
                String aggName = profileResult.getQueryName();
                long aggTimeInMillis = profileResult.getTime();
                List<ProfileResult> profiledChildren = profileResult.getProfiledChildren();
            }
        }

    }

}
