package com.example.yrofeeva.helen.elk;

import com.example.yrofeeva.helen.elk.client.ElasticClient;
import com.example.yrofeeva.helen.elk.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;

@RequiredArgsConstructor
abstract class AbstractElasticClientTest {

    private final ElasticClient elasticClient;

    private static Event newEvent() {
        var event = new Event();
        event.setTitle("title_1");
        event.setEventType("workshop");
        event.setDate("2021-12-01T11:00:00Z");
        event.setPlace("Rome");
        event.setDescription("description_1");
        event.setSubTopics(Set.of("subtopic_1","subtopic_2","subtopic_3"));
        return event;
    }

    @SneakyThrows(InterruptedException.class)
    private static void sleepForAWhile(int sec) {
        TimeUnit.SECONDS.sleep(sec);
    }

    @BeforeEach
    void createIndex() {
        assertThat(true, equalTo(elasticClient.createIndex(Event.INDEX)));
        sleepForAWhile(2);
    }

    @AfterEach
    void deleteIndex() {
        elasticClient.deleteIndex(Event.INDEX);
    }

    @Test
    void applyMapping() {
        var mappingJson = "{\"properties\":{\"description\":{\"type\":\"text\"}}}";

        assertThat(true, equalTo(elasticClient.applyMapping(Event.INDEX, mappingJson)));
    }

    @Test
    void eventAddGetDelete() {
        // ----- add -----
        var event = newEvent();
        var eventId = elasticClient.addEvent(event);
        assertThat(eventId, is(not(emptyOrNullString())));

        // ----- get -----
        var created = elasticClient.getEvent(eventId);
        assertThat(created.isPresent(), is(true));
        MatcherAssert.assertThat(created.get().getTitle(), is(event.getTitle()));

        // ----- update -----
        created.get().setTitle("new title");
        elasticClient.updateEvent(created.get());
        var updated = elasticClient.getEvent(eventId);
        assertThat(updated.isPresent(), is(true));
        MatcherAssert.assertThat(updated.get().getTitle(), CoreMatchers.is(created.get().getTitle()));

        // ----- delete -----
        elasticClient.deleteEvent(eventId);
        var deleted = elasticClient.getEvent(eventId);
        assertThat(deleted.isEmpty(), is(true));
    }

    @Test
    void eventSearchAll() {
        // ----- add -----
        var event = newEvent();
        var eventId = elasticClient.addEvent(event);
        sleepForAWhile(2);

        // ----- searchAll -----
        var searchAllResult = elasticClient.searchAll();
        assertThat(searchAllResult.isEmpty(), is(false));

        // ----- delete -----
        elasticClient.deleteEvent(eventId);
    }

    @Test
    void eventSearchWithQuery() {
        // ----- add -----
        var workshop = newEvent();
        var workshopId = elasticClient.addEvent(workshop);
        var techtalk = newEvent();
        var title = "May155";
        techtalk.setTitle(title);
        techtalk.setEventType("tech_talk");
        var techtalkId = elasticClient.addEvent(techtalk);
        sleepForAWhile(2);

        // ----- search by Title -----
        var searchResult = elasticClient.searchByField("title", title);
        assertThat(searchResult.isEmpty(), is(false));

        // ----- search Workshops -----
        searchResult = elasticClient.searchByField("eventType", workshop.getEventType());
        assertThat(searchResult.isEmpty(), is(false));

        // ----- search after Date -----
        var date = "2020-10-01T11:00:00Z";
        searchResult = elasticClient.searchByFieldAfterDate("title", title, date);
        assertThat(searchResult.isEmpty(), is(false));
        date = "2022-01-01T11:00:00Z";
        searchResult = elasticClient.searchByFieldAfterDate("title", title, date);
        assertThat(searchResult.isEmpty(), is(true));

        // ----- delete -----
        elasticClient.deleteEvent(workshopId);
        elasticClient.deleteEvent(techtalkId);
    }

}
