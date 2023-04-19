package com.example.yrofeeva.helen.elk;

import com.example.yrofeeva.helen.elk.client.ElasticClient;
import com.example.yrofeeva.helen.elk.model.Event;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LogProducer implements CommandLineRunner {
    @Autowired
    ElasticClient lowLevelElasticClient;
    @Value("${produce.logs}")
    private boolean produceLogs;
    @Value("${elastic.client.enabled}")
    private boolean elasticClientEnabled;

    private Random random = new Random();

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
        return random.nextInt(max - min) + min;
    }

    private void updateEvents() {
        for (int i = 1; i < 5; i++) {
            lowLevelElasticClient.updateEvent(buildEvent("" + i, "event " + i));
        }
    }

    @SneakyThrows
    @Override
    public void run(String... args) throws Exception {
        log.info("Console app run -- start");
        log.info("produce.logs: {}", produceLogs);
        log.info("elastic.client.enabled: {}", elasticClientEnabled);

        var produceLogsLocal = produceLogs;

        if (elasticClientEnabled) {
            updateEvents();
        }

        var startTime = LocalDateTime.now();
        while (produceLogsLocal){
            var randomId = getRandom();
            log.info("GET Event by id: {}", randomId);

            if(elasticClientEnabled) {
                var event = lowLevelElasticClient.getEvent("" + randomId);
                event.ifPresent(value -> log.info("Event: " + value.getTitle()));
            }

            TimeUnit.SECONDS.sleep(5);
            produceLogsLocal = ChronoUnit.MINUTES.between(startTime, LocalDateTime.now()) < 30;
        }
        log.info("Console app run -- end");
        System.exit(1);
    }
}