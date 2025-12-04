package com.example.yrofeeva.helen.elk.client.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.example.yrofeeva.helen.elk.client.ElasticClient;
import com.example.yrofeeva.helen.elk.model.Event;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class HighLevelElasticClient implements ElasticClient {
    private RestClient restClient;
    private ElasticsearchTransport transport;
    private ElasticsearchClient client;

    @PostConstruct
    void init() {
        restClient = RestClient.builder(
                new HttpHost("localhost", 9200, "http")).build();
        transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        client = new ElasticsearchClient(transport);
    }

    @SneakyThrows(IOException.class)
    @PreDestroy
    void preDestroy() {
        transport.close();
        restClient.close();
    }

    ElasticsearchClient getClient() {
        return client;
    }

    @SneakyThrows(IOException.class)
    @Override
    public boolean createIndex(String indexName) {
        var response = getClient().indices().create(
                CreateIndexRequest.of(c -> c.index(indexName)));
        return response.acknowledged();
    }

    @SneakyThrows(IOException.class)
    @Override
    public boolean deleteIndex(String indexName) {
        var response = getClient().indices().delete(
                DeleteIndexRequest.of(d -> d.index(indexName)));
        return response.acknowledged();
    }

    @SneakyThrows(IOException.class)
    @Override
    public boolean applyMapping(String indexName, String mappingJson) {
        var response = getClient().indices().putMapping(
                PutMappingRequest.of(p -> p
                        .index(indexName)
                        .withJson(new StringReader(mappingJson))));
        return response.acknowledged();
    }

    @SneakyThrows(IOException.class)
    @Override
    public String addEvent(Event event) {
        IndexResponse response = getClient().index(
                IndexRequest.of(i -> i
                        .index(Event.INDEX)
                        .document(event)));
        return response.id();
    }

    @SneakyThrows(IOException.class)
    @Override
    public String updateEvent(Event event) {
        IndexResponse response = getClient().index(
                IndexRequest.of(i -> i
                        .index(Event.INDEX)
                        .id(event.getId())
                        .document(event)));
        return response.id();
    }

    @SneakyThrows(IOException.class)
    @Override
    public Optional<Event> getEvent(String id) {
        GetResponse<Event> response = getClient().get(
                GetRequest.of(g -> g
                        .index(Event.INDEX)
                        .id(id)),
                Event.class);
        if (response.found() && response.source() != null) {
            Event event = response.source();
            event.setId(response.id());
            return Optional.of(event);
        }
        return Optional.empty();
    }

    @SneakyThrows(IOException.class)
    @Override
    public boolean deleteEvent(String id) {
        DeleteResponse response = getClient().delete(
                DeleteRequest.of(d -> d
                        .index(Event.INDEX)
                        .id(id)));
        return "deleted".equals(response.result().jsonValue());
    }

    @Override
    public List<Event> searchAll() {
        Query query = MatchAllQuery.of(m -> m)._toQuery();
        return search(query);
    }

    @Override
    public List<Event> searchByField(String field, String value) {
        Query query = MatchQuery.of(m -> m
                .field(field)
                .query(value))._toQuery();
        return search(query);
    }

    @Override
    public List<Event> searchByFieldAfterDate(String field, String fieldValue, String date) {
        Query matchQuery = MatchQuery.of(m -> m
                .field(field)
                .query(fieldValue))._toQuery();

        Query rangeQuery = RangeQuery.of(r -> r
                .date(d -> d
                        .field("date")
                        .gte(date)))._toQuery();

        Query boolQuery = BoolQuery.of(b -> b
                .must(matchQuery)
                .must(rangeQuery))._toQuery();

        return search(boolQuery);
    }

    @SneakyThrows(IOException.class)
    private List<Event> search(Query query) {
        SearchResponse<Event> response = getClient().search(
                SearchRequest.of(s -> s
                        .index(Event.INDEX)
                        .query(query)),
                Event.class);

        return response.hits().hits().stream()
                .map(hit -> {
                    Event event = hit.source();
                    if (event != null) {
                        event.setId(hit.id());
                    }
                    return event;
                })
                .filter(event -> event != null)
                .collect(Collectors.toList());
    }
}
