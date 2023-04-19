package com.example.yrofeeva.helen.elk;

import com.example.yrofeeva.helen.elk.client.ElasticClient;
import com.example.yrofeeva.helen.elk.model.Event;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LogProducer implements CommandLineRunner {
    @Autowired
    ElasticClient lowLevelElasticClient;

    private static Event buildEvent(String id, String title) {
        var event = new Event();
        event.setId(id);
        event.setTitle(title);
        event.setEventType("workshop");
        event.setDate("2021-12-01T11:00:00Z");
        event.setPlace("Rome");
        event.setDescription("description");
        event.setSubTopics(Set.of("subtopic"));
        return event;
    }

    private int getRandom() {
        int min = 1;
        int max = 10;
        Random random = new Random();
        return random.nextInt(max - min) + min;
    }

    @SneakyThrows
    @Override
    public void run(String... args) {
        for (int i = 1; i < 5; i++) {
            lowLevelElasticClient.updateEvent(buildEvent("" + i, "event " + i));
        }
        while (true){
            var randomId = getRandom();
            log.info("GET Event by id: {}", randomId);
            var event = lowLevelElasticClient.getEvent(""+randomId);
            if(event.isPresent()){
                log.info("Event: " + event.get().getTitle());
            }
            TimeUnit.SECONDS.sleep(2);
        }
    }
}
