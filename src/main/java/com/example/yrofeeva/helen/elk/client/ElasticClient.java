package com.example.yrofeeva.helen.elk.client;

import com.example.yrofeeva.helen.elk.model.Event;

import java.util.List;
import java.util.Optional;

public interface ElasticClient {

    boolean createIndex(String indexName);

    boolean deleteIndex(String indexName);

    boolean applyMapping(String indexName, String mappingJson);

    String addEvent(Event event);

    Optional<Event> getEvent(String id);

    String updateEvent(Event event);

    boolean deleteEvent(String id);

    List<Event> searchAll();

    List<Event> searchByField(String field, String value);

    List<Event> searchByFieldAfterDate(String field, String fieldValue, String date);
}
