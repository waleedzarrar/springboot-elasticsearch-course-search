package com.undoschool.course_search.loader;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.undoschool.course_search.document.CourseDocument;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CourseDataLoader {

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void loadData() {
        try {
            // Register JavaTimeModule to support LocalDateTime
            objectMapper.registerModule(new JavaTimeModule());

            InputStream inputStream = new ClassPathResource("sample-courses.json").getInputStream();
            List<CourseDocument> courses = objectMapper.readValue(inputStream, new TypeReference<>() {});

            System.out.println("📦 Loaded " + courses.size() + " courses from JSON");

            List<BulkOperation> operations = courses.stream()
                    .map(course -> BulkOperation.of(b -> b.index(i -> i
                            .index("courses")
                            .id(course.getId())
                            .document(course)
                    )))
                    .collect(Collectors.toList());

            BulkResponse response = elasticsearchClient.bulk(b -> b.operations(operations));

            if (response.errors()) {
                System.out.println("❌ Some documents failed during bulk indexing.");
            } else {
                System.out.println("✅ Courses indexed successfully!");
            }

        } catch (Exception e) {
            System.out.println("⚠️ Failed to load sample courses:");
            e.printStackTrace();
        }
    }
}
