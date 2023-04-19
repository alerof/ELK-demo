package com.example.yrofeeva.helen.elk.client.impl;

import com.example.yrofeeva.helen.elk.client.EventUtil;
import com.example.yrofeeva.helen.elk.client.ElasticClient;
import com.example.yrofeeva.helen.elk.model.Event;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class LowLevelElasticClient implements ElasticClient {
    public static final String ELASTICSEARCH_ACTION_FAILED = "Elasticsearch action {} {} failed";
    private RestClient client;

    @PostConstruct
    void init(){
        client = RestClient.builder(
                new HttpHost("localhost", 9200, "http")
        ).build();
    }

    @SneakyThrows
    @PreDestroy
    void preDestroy() {
        client.close();
    }

    RestClient getRestClient() {
        return client;
    }

    @Override
    public boolean createIndex(String indexName) {
        var method = HttpMethod.PUT.name();
        var requestQuery = String.format("/%s",indexName);
        var request = new Request(method, requestQuery);
        try {
            getRestClient().performRequest(request);
        } catch (IOException e) {
            log.warn(ELASTICSEARCH_ACTION_FAILED, method, requestQuery, e);
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteIndex(String indexName) {
        var method = HttpMethod.DELETE.name();
        var requestQuery = String.format("/%s",indexName);
        var request = new Request(method, requestQuery);
        try {
            getRestClient().performRequest(request);
        } catch (IOException e) {
            log.warn(ELASTICSEARCH_ACTION_FAILED, method, requestQuery, e);
            return false;
        }
        return true;
    }

    @Override
    public boolean applyMapping(String indexName, String mappingJson) {
        var method = HttpMethod.PUT.name();
        var requestQuery = String.format("/%s/_mapping", indexName);
        var request = new Request(method, requestQuery);
        request.setJsonEntity(mappingJson);
        try {
            var response = getRestClient().performRequest(request);
            return  response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (IOException e) {
            log.warn(ELASTICSEARCH_ACTION_FAILED, method, requestQuery, e);
            return false;
        }
    }

    @Override
    public String addEvent(Event event) {
        var method = HttpMethod.POST.name();
        var requestQuery = String.format("/%s/_doc", Event.INDEX);
        return upsertEvent(method, requestQuery, event);
    }

    @Override
    public String updateEvent(Event event) {
        var method = HttpMethod.PUT.name();
        var requestQuery = String.format("/%s/_doc/%s", Event.INDEX, event.getId());
        return upsertEvent(method, requestQuery, event);
    }

    private String upsertEvent(String method, String requestQuery, Event event){
        var id = "";
        var request = new Request(method, requestQuery);
        try {
            request.setJsonEntity(EventUtil.event2JsonEntity(event));

            Response response = getRestClient().performRequest(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED ||
                    response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return EventUtil.getEventIdFromUpsertResponse(response.getEntity().getContent());
            }
        } catch (IOException e) {
            log.warn(ELASTICSEARCH_ACTION_FAILED, method, requestQuery, e);
        }
        return id;
    }

    @Override
    public Optional<Event> getEvent(String id) {
        var method = HttpMethod.GET.name();
        var requestQuery = String.format("/%s/_doc/%s", Event.INDEX, id);
        var request = new Request(method, requestQuery);
        try {
            Response response = client.performRequest(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                var event  = EventUtil.getEventFromGetResponse(response.getEntity().getContent());
                return Optional.of(event);
            }
        } catch (ResponseException e) {
            log.warn(ELASTICSEARCH_ACTION_FAILED, method, requestQuery);
            if (e.getResponse().getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND){
                return Optional.empty();
            }
        } catch (IOException e) {
            log.warn(ELASTICSEARCH_ACTION_FAILED, method, requestQuery, e);
        }
        return Optional.empty();
    }

    @Override
    public boolean deleteEvent(String id) {
        var method = HttpMethod.DELETE.name();
        var requestQuery = String.format("/%s/_doc/%s", Event.INDEX, id);
        var request = new Request(method, requestQuery);
        try {
            return client.performRequest(request).getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (IOException e) {
            log.warn(ELASTICSEARCH_ACTION_FAILED, method, requestQuery, e);
        }
        return false;
    }

    @Override
    public List<Event> searchAll() {
        var method = HttpMethod.POST.name();
        var requestQuery = String.format("/%s/_search", Event.INDEX);
        var request = new Request(method, requestQuery);
        return processSearchRequest(method, requestQuery, request);
    }

    @Override
    public List<Event> searchByField(String field, String value) {
        String requestBody = String.format("{\"query\":{ \"match\": { \"%s\":\"%s\" }}}", field, value);
        return processSearchWithQuery(requestBody);
    }

    @Override
    public List<Event> searchByFieldAfterDate(String field, String fieldValue, String date) {
        String requestBody = String.format("{ \"query\":{ \"bool\":{ \"must\":[{ \"match\": { \"%s\":\"%s\" }}], \"filter\":[{ \"range\":{ \"date\":{ \"gte\":\"%s\" }}}]}}}", field, fieldValue, date);
        return processSearchWithQuery(requestBody);
    }

    private List<Event> processSearchWithQuery(String requestBody) {
        var method = HttpMethod.POST.name();
        var requestQuery = String.format("/%s/_search", Event.INDEX);
        var request = new Request(method, requestQuery);
        request.setJsonEntity(requestBody);
        return processSearchRequest(method, requestQuery, request);
    }

    private List<Event> processSearchRequest(String method, String requestQuery, Request request) {
        try {
            Response response = client.performRequest(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return EventUtil.getEventsFromSearchResponse(response.getEntity().getContent());
            }
        } catch (IOException e) {
            log.warn(ELASTICSEARCH_ACTION_FAILED, method, requestQuery, e);
        }
        return new ArrayList<>();
    }

}
