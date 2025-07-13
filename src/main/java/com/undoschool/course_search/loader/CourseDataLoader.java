package com.undoschool.course_search.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undoschool.course_search.document.CourseDocument;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CourseDataLoader {

    private static final Logger log = LoggerFactory.getLogger(CourseDataLoader.class);
    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void loadData() {
        try (InputStream inputStream = getClass().getResourceAsStream("/sample-courses.json")) {
            List<CourseDocument> courses = objectMapper.readValue(inputStream, new TypeReference<>() {});
            log.info("📦 Loaded {} courses from JSON", courses.size());

            // Set suggest field
            courses.forEach(course -> course.setSuggest(List.of(course.getTitle())));

            BulkRequest.Builder br = new BulkRequest.Builder();
            for (CourseDocument course : courses) {
                br.operations(op -> op
                        .index(idx -> idx
                                .index("courses")
                                .id(course.getId())
                                .document(course)
                        )
                );
            }

            elasticsearchClient.bulk(br.build());
            log.info("✅ Courses indexed successfully");
        } catch (Exception e) {
            log.warn("⚠️ Failed to load sample courses:", e);
        }
    }
}
