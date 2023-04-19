package com.example.yrofeeva.helen.elk.client;

import com.example.yrofeeva.helen.elk.model.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@UtilityClass
public final class EventUtil {
    private static final ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder()
            .indentOutput(true)
            .propertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
            .build();

    private static final String ID = "_id";
    private static final String SOURCE = "_source";
    private static final String HITS = "hits";

    public static String event2JsonEntity(Event event) throws JsonProcessingException {
        return objectMapper.writeValueAsString(event);
    }

    // ======================= Low level client ==================

    public static String getEventIdFromUpsertResponse(InputStream content) throws IOException {
        var jsonMap = objectMapper.readValue(content, Map.class);
        return jsonMap.get(ID).toString();
    }

    @SneakyThrows
    public static Event getEventFromGetResponse(InputStream content) {
        return extractEvent(objectMapper.readValue(content, ObjectNode.class));
    }

    public static List<Event> getEventsFromSearchResponse(InputStream content) throws IOException {
        var node = objectMapper.readValue(content, ObjectNode.class);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(node.get(HITS).get(HITS).elements(), Spliterator.ORDERED), false)
                .map(EventUtil::extractEvent)
                .collect(Collectors.toUnmodifiableList());
    }

    @SneakyThrows({JsonProcessingException.class})
    private static Event extractEvent(JsonNode node) {
        var id = node.get(ID).textValue();
        var event = objectMapper.readValue(node.get(SOURCE).toString(), Event.class);
        event.setId(id);
        return event;
    }

    // ======================= High level client ==================

    public static Event getEventFromGetResponse(GetResponse response) throws JsonProcessingException {
        var event = objectMapper.readValue(response.getSourceAsString(), Event.class);
        event.setId(response.getId());
        return event;
    }

    public static List<Event> getEventsFromSearchResponse(SearchResponse searchResponse) {
        return Stream.of(searchResponse.getHits().getHits())
                .map(EventUtil::extractEvent)
                .collect(Collectors.toList());
    }

    @SneakyThrows({JsonProcessingException.class})
    private static Event extractEvent(SearchHit hit) {
        var event = objectMapper.readValue(hit.getSourceAsString(), Event.class);
        event.setId(hit.getId());
        return event;
    }
}
