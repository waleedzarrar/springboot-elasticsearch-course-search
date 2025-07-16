package com.undoschool.course_search.loader;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import com.github.javafaker.Faker;
import com.undoschool.course_search.document.CourseDocument;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream; // Ensure this is imported

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseDataLoader {

    private final ElasticsearchClient elasticsearchClient;
    private static final String INDEX_NAME = "courses";

    @PostConstruct
    public void loadData() {
        try {
            log.info("Starting CourseDataLoader...");
            log.info("Deleting existing index '{}' if present...", INDEX_NAME);
            deleteIndex(); // Call the updated method
            log.info("Creating index '{}' with custom mappings...", INDEX_NAME);
            createIndex();
            log.info("Bulk indexing sample courses...");
            bulkIndexCourses(50);
            log.info("CourseDataLoader finished successfully.");
        } catch (IOException e) {
            log.error("Error during data loading: {}", e.getMessage(), e);
        }
    }

    private void deleteIndex() throws IOException {
        DeleteIndexRequest deleteRequest = DeleteIndexRequest.of(d -> d
                .index(INDEX_NAME)
                .ignoreUnavailable(true) // <-- ADD THIS LINE
        );
        DeleteIndexResponse deleteResponse = elasticsearchClient.indices().delete(deleteRequest);

        // Check if the index was actually deleted (if it existed)
        if (deleteResponse.acknowledged()) {
            log.info("Index '{}' deleted successfully (or did not exist).", INDEX_NAME);
        } else {
            // This else block might be hit if acknowledge is false for other reasons,
            // but for ignoreUnavailable=true, it primarily means it didn't exist or wasn't acknowledged.
            log.warn("Index deletion for '{}' was not acknowledged.", INDEX_NAME);
        }
    }

    private void createIndex() throws IOException {
        ClassPathResource mappingResource = new ClassPathResource("static/es-mappings.json");
        ClassPathResource settingsResource = new ClassPathResource("static/es-settings.json");

        try (InputStream mappingIs = mappingResource.getInputStream();
             InputStream settingsIs = settingsResource.getInputStream()) {

            CreateIndexRequest createRequest = CreateIndexRequest.of(c -> c
                    .index(INDEX_NAME)
                    .mappings(m -> m.withJson(mappingIs))
                    .settings(s -> s.withJson(settingsIs))
            );

            CreateIndexResponse createResponse = elasticsearchClient.indices().create(createRequest);
            if (createResponse.acknowledged()) {
                log.info("Index '{}' created successfully.", INDEX_NAME);
            } else {
                log.error("Failed to create index '{}'.", INDEX_NAME);
            }
        }
    }

    private void bulkIndexCourses(int numberOfCourses) throws IOException {
        Faker faker = new Faker(new Locale("en", "IND"));
        List<CourseDocument> courses = new ArrayList<>();
        String[] categories = {"Data Science", "Programming", "Marketing", "Design", "Business", "Language"};
        String[] types = {"LECTURE", "WORKSHOP", "SEMINAR", "ONLINE"};

        // Corrected IntStream usage: range(start, end)
        IntStream.range(0, numberOfCourses).forEach(i -> {
            String title = faker.educator().course();
            String description = faker.lorem().paragraph(3);
            String category = categories[faker.random().nextInt(categories.length)];
            String type = types[faker.random().nextInt(types.length)];
            Integer minAge = faker.random().nextInt(14, 18);
            Integer maxAge = faker.random().nextInt(60, 100);
            Double price = faker.number().randomDouble(2, 500, 5000);

            Date futureDate = faker.date().future(365, TimeUnit.DAYS);
            LocalDateTime nextSessionDate = futureDate.toInstant()
                                                        .atZone(ZoneId.systemDefault())
                                                        .toLocalDateTime();

         // Prepare suggestions from title. Split by one or more whitespace, filter empty, lowercase, and get distinct words.
            String[] suggest = Arrays.stream(title.split("\\s+")) // Split by one or more whitespace characters (regex)
                                    .filter(s -> !s.trim().isEmpty()) // Filter out empty strings after trimming whitespace
                                    .map(String::toLowerCase)
                                    .distinct() // remove duplicates from title words
                                    .toArray(String[]::new);


            CourseDocument course = CourseDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .title(title)
                    .description(description)
                    .category(category)
                    .type(type)
                    .minAge(minAge)
                    .maxAge(maxAge)
                    .price(price)
                    .nextSessionDate(nextSessionDate)
                    .suggest(suggest)
                    .build();
            courses.add(course);
        });

        if (courses.isEmpty()) {
            log.warn("No courses generated for bulk indexing.");
            return;
        }

        elasticsearchClient.bulk(b -> {
            courses.forEach(course -> b.operations(op -> op.index(idx -> idx
                    .index(INDEX_NAME)
                    .id(course.getId())
                    .document(course)
            )));
            return b;
        });

        log.info("Successfully indexed {} courses.", courses.size());
    }
}