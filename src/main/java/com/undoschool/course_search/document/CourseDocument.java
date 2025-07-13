package com.undoschool.course_search.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "courses")
public class CourseDocument {

    @Id
    private String id;

    private String title;

    private String description;

    private String category;

    private String type;

    private String gradeRange;

    private Integer minAge;

    private Integer maxAge;

    private Double price;

    private LocalDateTime nextSessionDate;

    private List<String> suggest;  // used by completion suggester
}
