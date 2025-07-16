package com.undoschool.course_search.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor; // Add this import
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; // Add this import
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;

@Data
@Builder // Keep this for convenient instance creation (e.g., in CourseDataLoader)
@NoArgsConstructor // Explicit no-arg constructor for Jackson during deserialization
@AllArgsConstructor // Constructor with all fields, works well with @Builder
// @Document and @Setting are good for context, even if mappings are from JSON
@Document(indexName = "courses")
@Setting(settingPath = "static/es-settings.json")
public class CourseDocument {

    @Id // This annotation is primarily for Spring Data Elasticsearch's repository abstraction.
        // For the low-level client, Jackson will just look for a field named "id" in _source.
    private String id;
    private String title;
    private String description;
    private String category;
    private String type;
    private Integer minAge;
    private Integer maxAge;
    private Double price;

    // Ensure this format matches exactly how Elasticsearch stores the date after indexing
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime nextSessionDate;

    private String[] suggest;
}