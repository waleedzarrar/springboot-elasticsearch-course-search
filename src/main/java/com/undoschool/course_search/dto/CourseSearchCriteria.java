package com.undoschool.course_search.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class CourseSearchCriteria {
    private String q; // search keyword for title and description (and other fields)
    private Integer minAge;
    private Integer maxAge;
    private String category;
    private String type;
    private Double minPrice;
    private Double maxPrice;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) // Required for Spring to parse ISO-8601 strings
    private LocalDateTime startDate; // ISO-8601 date-time string, e.g., "2025-06-10T15:00:00"

    private String sort; // e.g., "price:asc", "price:desc", "upcoming"
    private Integer page; // default 0
    private Integer size; // default 10
}