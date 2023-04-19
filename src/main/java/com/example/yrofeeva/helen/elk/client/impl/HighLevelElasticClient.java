package com.example.yrofeeva.helen.elk.client.impl;

import com.example.yrofeeva.helen.elk.client.ElasticClient;
import com.example.yrofeeva.helen.elk.client.EventUtil;
import com.example.yrofeeva.helen.elk.model.Event;
import lombok.SneakyThrows;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class HighLevelElasticClient implements ElasticClient {
    private RestHighLevelClient client;

    @PostConstruct
    void init(){
        client = new RestHighLevelClient(
                        RestClient.builder(
                                new HttpHost("localhost", 9200, "http")));
    }

    @SneakyThrows(IOException.class)
    @PreDestroy
    void preDestroy() {
        client.close();
    }

    RestHighLevelClient getRestClient() {
        return client;
    }

    @SneakyThrows(IOException.class)
    @Override
    public boolean createIndex(String indexName) {
        var request = new CreateIndexRequest(indexName);
        return getRestClient().indices().create(request, RequestOptions.DEFAULT).isAcknowledged();
    }

    @SneakyThrows(IOException.class)
    @Override
    public boolean deleteIndex(String indexName) {
        var request = new DeleteIndexRequest(indexName);
        return getRestClient().indices().delete(request, RequestOptions.DEFAULT).isAcknowledged();
    }

    @SneakyThrows(IOException.class)
    @Override
    public boolean applyMapping(String indexName, String mappingJson) {
        var request = new PutMappingRequest(indexName);
        request.source(mappingJson, XContentType.JSON);
        return getRestClient().indices().putMapping(request, RequestOptions.DEFAULT).isAcknowledged();
    }

    @SneakyThrows(IOException.class)
    @Override
    public String addEvent(Event event) {
        var request = new IndexRequest(Event.INDEX);
        var jsonEntity = EventUtil.event2JsonEntity(event);
        request.source(jsonEntity, XContentType.JSON);
        return getRestClient().index(request, RequestOptions.DEFAULT).getId();
    }

    @SneakyThrows(IOException.class)
    @Override
    public String updateEvent(Event event) {
        var request = new IndexRequest(Event.INDEX);
        request.id(event.getId());
        var jsonEntity = EventUtil.event2JsonEntity(event);
        request.source(jsonEntity, XContentType.JSON);
        return getRestClient().index(request, RequestOptions.DEFAULT).getId();
    }

    @SneakyThrows(IOException.class)
    @Override
    public Optional<Event> getEvent(String id) {
        var request = new GetRequest(Event.INDEX, id);
        var response = getRestClient().get(request, RequestOptions.DEFAULT);
        if (response.isExists()) {
            return Optional.of(EventUtil.getEventFromGetResponse(response));
        }
        return Optional.empty();
    }

    @SneakyThrows(IOException.class)
    @Override
    public boolean deleteEvent(String id) {
        var request = new DeleteRequest(Event.INDEX, id);
        return getRestClient().delete(request, RequestOptions.DEFAULT).status().getStatus() == HttpStatus.SC_OK;
    }

    @Override
    public List<Event> searchAll() {
        return search(QueryBuilders.matchAllQuery());
    }

    @Override
    public List<Event> searchByField(String field, String value) {
        return search(new MatchQueryBuilder(field, value));
    }

    @Override
    public List<Event> searchByFieldAfterDate(String field, String fieldValue, String date) {
        MatchQueryBuilder match = QueryBuilders.matchQuery(field, fieldValue);
        RangeQueryBuilder range = QueryBuilders.rangeQuery("date").gte(date);
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        query.must(match).must(range);
        return search(query);
    }

    @SneakyThrows(IOException.class)
    private List<Event> search(QueryBuilder query) {
        var searchRequest = new SearchRequest(Event.INDEX);
        searchRequest.source(new SearchSourceBuilder().query(query));
        var searchResponse = getRestClient().search(searchRequest, RequestOptions.DEFAULT);
        return EventUtil.getEventsFromSearchResponse(searchResponse);
    }

}
