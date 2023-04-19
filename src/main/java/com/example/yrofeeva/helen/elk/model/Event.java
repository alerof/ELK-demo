package com.example.yrofeeva.helen.elk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class Event {
    public static final String INDEX = "events2";

    @JsonIgnore
    private String id;
    private String title;
    private String eventType;
    private String date;
    private String place;
    private String description;
    private Set<String> subTopics = new HashSet<>();
}
