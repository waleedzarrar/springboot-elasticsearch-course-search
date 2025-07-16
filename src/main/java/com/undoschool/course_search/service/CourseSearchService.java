package com.undoschool.course_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggester; // Ensure this is imported correctly
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.json.JsonData;
import com.undoschool.course_search.document.CourseDocument;
import com.undoschool.course_search.dto.CourseSearchCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private static final String INDEX_NAME = "courses";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_PAGE_NUMBER = 0;

    public Map<String, Object> searchCourses(CourseSearchCriteria criteria) throws IOException {
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .from(criteria.getPage() != null ? criteria.getPage() * (criteria.getSize() != null ? criteria.getSize() : DEFAULT_PAGE_SIZE) : DEFAULT_PAGE_NUMBER)
                .size(criteria.getSize() != null ? criteria.getSize() : DEFAULT_PAGE_SIZE);

        List<Query> filterQueries = new ArrayList<>();
        List<Query> matchQueries = new ArrayList<>();

        if (criteria.getQ() != null && !criteria.getQ().isBlank()) {
            matchQueries.add(Query.of(q -> q
                    .multiMatch(mm -> mm
                            .fields("title", "description")
                            .query(criteria.getQ())
                            .fuzziness("AUTO")
                    )
            ));
        }

        if (criteria.getCategory() != null && !criteria.getCategory().isBlank()) {
            filterQueries.add(Query.of(q -> q.term(t -> t.field("category").value(criteria.getCategory()))));
        }
        if (criteria.getType() != null && !criteria.getType().isBlank()) {
            filterQueries.add(Query.of(q -> q.term(t -> t.field("type").value(criteria.getType()))));
        }
        if (criteria.getMinAge() != null) {
            filterQueries.add(Query.of(q -> q.range(r -> r.field("minAge").gte(JsonData.of(criteria.getMinAge())))));
        }
        if (criteria.getMaxAge() != null) {
            filterQueries.add(Query.of(q -> q.range(r -> r.field("maxAge").lte(JsonData.of(criteria.getMaxAge())))));
        }
        if (criteria.getMinPrice() != null) {
            filterQueries.add(Query.of(q -> q.range(r -> r.field("price").gte(JsonData.of(criteria.getMinPrice())))));
        }
        if (criteria.getMaxPrice() != null) {
            filterQueries.add(Query.of(q -> q.range(r -> r.field("price").lte(JsonData.of(criteria.getMaxPrice())))));
        }
        if (criteria.getStartDate() != null) {
            filterQueries.add(Query.of(q -> q.range(r -> r.field("nextSessionDate").gte(JsonData.of(criteria.getStartDate().toString())))));
        }

        searchRequestBuilder.query(q -> q
                .bool(b -> b
                        .must(matchQueries)
                        .filter(filterQueries)
                )
        );

        if (criteria.getSort() != null && !criteria.getSort().isBlank()) {
            String sortParam = criteria.getSort().toLowerCase();
            if (sortParam.equals("price:asc")) {
                searchRequestBuilder.sort(s -> s.field(f -> f.field("price").order(SortOrder.Asc)));
            } else if (sortParam.equals("price:desc")) {
                searchRequestBuilder.sort(s -> s.field(f -> f.field("price").order(SortOrder.Desc)));
            } else if (sortParam.equals("upcoming")) {
                searchRequestBuilder.sort(s -> s.field(f -> f.field("nextSessionDate").order(SortOrder.Asc)));
            }
        } else {
            searchRequestBuilder.sort(s -> s.field(f -> f.field("nextSessionDate").order(SortOrder.Asc)));
        }

        SearchResponse<CourseDocument> response = elasticsearchClient.search(searchRequestBuilder.build(), CourseDocument.class);
        long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
        List<CourseDocument> courses = response.hits().hits().stream()
                .map(hit -> hit.source())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return Map.of("courses", courses, "total", totalHits);
    }


    public List<String> getSuggestions(String query) throws IOException {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        log.info("💡 Getting suggestions for query: {}", query);

        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .suggest(s -> s
                        .suggesters("course-suggester", c -> c // 'c' is the Suggester.Builder
                                .text(query) // <--- CORRECT FIX: Set the input text here!
                                .completion(co -> co // 'co' is the CompletionSuggester.Builder, for specific options
                                        .field("suggest")
                                        .skipDuplicates(true)
                                        .size(10)
                                )
                        )
                )
                .build();

        SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

        List<String> suggestions = new ArrayList<>();
        if (response.suggest() != null) {
            response.suggest().get("course-suggester")
                    .stream()
                    .flatMap(s -> s.completion().options().stream())
                    .map(o -> o.text())
                    .distinct()
                    .collect(Collectors.toCollection(() -> suggestions));
        }

        log.info("💡 Found {} suggestions for query '{}'", suggestions.size(), query);
        return suggestions;
    }
}