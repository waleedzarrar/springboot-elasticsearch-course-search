package com.undoschool.course_search.loader;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undoschool.course_search.document.CourseDocument;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays; // Import Arrays
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set; // Import Set
import java.util.HashSet; // Import HashSet

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseDataLoader {

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;
    private static final String INDEX_NAME = "courses";

    @PostConstruct
    public void loadData() {
        try {
            ensureIndexWithCorrectMapping();

            InputStream inputStream = new ClassPathResource("sample_courses.json").getInputStream();
            List<CourseDocument> courses = objectMapper.readValue(inputStream, new TypeReference<>() {});

            log.info("📦 Loaded {} courses from JSON", courses.size());

            List<BulkOperation> operations = new ArrayList<>();
            for (CourseDocument course : courses) {
                // Use a Set to automatically handle duplicates and maintain uniqueness
                Set<String> uniqueSuggestValues = new HashSet<>();

                // 1. Add the full course title
                if (course.getTitle() != null && !course.getTitle().trim().isEmpty()) {
                    uniqueSuggestValues.add(course.getTitle().trim());
                    // 2. Add individual words from the title
                    Arrays.stream(course.getTitle().split("\\s+")) // Split by one or more whitespace characters
                          .map(String::trim)
                          .filter(s -> !s.isEmpty())
                          .forEach(uniqueSuggestValues::add);
                }

                // 3. Add the category
                if (course.getCategory() != null && !course.getCategory().trim().isEmpty()) {
                    uniqueSuggestValues.add(course.getCategory().trim());
                    // Also add individual words from category if it's multi-word (e.g., "Computer Science")
                    Arrays.stream(course.getCategory().split("\\s+"))
                          .map(String::trim)
                          .filter(s -> !s.isEmpty())
                          .forEach(uniqueSuggestValues::add);
                }

                // 4. Add the course type (e.g., "Online", "In-person")
                if (course.getType() != null && !course.getType().trim().isEmpty()) {
                    uniqueSuggestValues.add(course.getType().trim());
                }

                // Convert Set to List for the document
                course.setSuggest(new ArrayList<>(uniqueSuggestValues));

                if (course.getNextSessionDate() == null) {
                    course.setNextSessionDate(LocalDateTime.of(2025, 8, 1, 10, 0));
                }

                operations.add(BulkOperation.of(op ->
                        op.index(idx -> idx
                                .index(INDEX_NAME)
                                .id(course.getId())
                                .document(course)
                        )
                ));
            }

            BulkRequest request = new BulkRequest.Builder().operations(operations).build();
            BulkResponse response = elasticsearchClient.bulk(request);

            if (response.errors()) {
                log.error("⚠️ Bulk indexing completed with errors: {}", response.items().stream()
                        .filter(item -> item.error() != null)
                        .map(item -> item.error().reason())
                        .collect(Collectors.joining("; ")));
            } else {
                log.info("✅ Bulk indexing completed successfully.");
            }

        } catch (IOException e) {
            log.error("⚠️ Failed to load sample courses:", e);
        }
    }

    private void ensureIndexWithCorrectMapping() throws IOException {
        try {
            DeleteIndexRequest deleteRequest = new DeleteIndexRequest.Builder().index(INDEX_NAME).build();
            elasticsearchClient.indices().delete(deleteRequest);
            log.info("🗑️ Existing index '{}' deleted (if it existed).", INDEX_NAME);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("index_not_found_exception")) {
                log.info("Index '{}' did not exist, proceeding with creation.", INDEX_NAME);
            } else {
                log.warn("Could not delete index '{}', it might be in use or another error occurred: {}", INDEX_NAME, e.getMessage());
            }
        }

        CreateIndexRequest createRequest = new CreateIndexRequest.Builder()
                .index(INDEX_NAME)
                .mappings(m -> m
                        .properties("id", p -> p.keyword(k -> k))
                        .properties("title", p -> p.text(t -> t))
                        .properties("description", p -> p.text(t -> t))
                        .properties("category", p -> p.keyword(k -> k)) // Use keyword for direct category match
                        .properties("type", p -> p.keyword(k -> k))     // Use keyword for direct type match
                        .properties("gradeRange", p -> p.text(t -> t))
                        .properties("minAge", p -> p.integer(i -> i))
                        .properties("maxAge", p -> p.integer(i -> i))
                        .properties("price", p -> p.float_(f -> f))
                        .properties("nextSessionDate", p -> p.date(d -> d))
                        .properties("suggest", p -> p.completion(c -> c
                                .analyzer("simple")
                                .searchAnalyzer("simple")
                        ))
                )
                .build();

        CreateIndexResponse createResponse = elasticsearchClient.indices().create(createRequest);
        if (createResponse.acknowledged()) {
            log.info("✅ Index '{}' created successfully with correct mapping.", INDEX_NAME);
        } else {
            log.error("❌ Failed to create index '{}' with correct mapping.", INDEX_NAME);
        }
    }
}