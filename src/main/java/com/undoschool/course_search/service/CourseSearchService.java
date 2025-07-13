package com.undoschool.course_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.undoschool.course_search.document.CourseDocument;
import com.undoschool.course_search.dto.CourseSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseSearchService {

    private final ElasticsearchClient elasticsearchClient;

    public List<CourseDocument> autocompleteSearch(CourseSearchRequest request) {
        try {
            String query = request.getQuery();
            log.info("🔍 Autocomplete search for: {}", query);

            SearchResponse<CourseDocument> response = elasticsearchClient.search(s -> s
                    .index("courses")
                    .query(q -> q
                        .match(m -> m
                            .field("title")
                            .query(query)
                            .fuzziness("AUTO")
                        )
                    ), CourseDocument.class);

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .toList();

        } catch (IOException e) {
            log.error("❌ Failed autocomplete search", e);
            return Collections.emptyList();
        }
    }
}
