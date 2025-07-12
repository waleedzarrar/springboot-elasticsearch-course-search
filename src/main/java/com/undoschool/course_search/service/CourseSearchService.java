package com.undoschool.course_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.undoschool.course_search.document.CourseDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseSearchService {

    private final ElasticsearchClient elasticsearchClient;

    public List<CourseDocument> searchCourses(
            String query,
            Integer minAge,
            Integer maxAge,
            String category,
            String type,
            Double minPrice,
            Double maxPrice,
            String startDate
    ) throws IOException {

        List<Query> filters = new ArrayList<>();

        if (minAge != null || maxAge != null) {
            filters.add(Query.of(q -> q.range(r -> r
                    .field("minAge")
                    .gte(JsonData.of(minAge != null ? minAge : 0))
                    .lte(JsonData.of(maxAge != null ? maxAge : 100))
            )));
        }

        if (minPrice != null || maxPrice != null) {
            filters.add(Query.of(q -> q.range(r -> r
                    .field("price")
                    .gte(JsonData.of(minPrice != null ? minPrice : 0.0))
                    .lte(JsonData.of(maxPrice != null ? maxPrice : 10000.0))
            )));
        }

        if (category != null) {
            filters.add(Query.of(q -> q.term(t -> t
                    .field("category.keyword")
                    .value(category)
            )));
        }

        if (type != null) {
            filters.add(Query.of(q -> q.term(t -> t
                    .field("type.keyword")
                    .value(type)
            )));
        }

        if (startDate != null) {
            filters.add(Query.of(q -> q.range(r -> r
                    .field("nextSessionDate")
                    .gte(JsonData.of(startDate))
            )));
        }

        Query finalQuery = Query.of(q -> q.bool(b -> b
                .must(query != null && !query.isEmpty()
                        ? Query.of(m -> m.match(match -> match
                                .field("title")
                                .query(query)))
                        : Query.of(m -> m.matchAll(ma -> ma)))
                .filter(filters)
        ));

        SearchRequest request = SearchRequest.of(s -> s
                .index("courses")
                .query(finalQuery)
                .size(10)
        );

        SearchResponse<CourseDocument> response = elasticsearchClient.search(request, CourseDocument.class);

        return response.hits().hits().stream()
                .map(hit -> hit.source())
                .toList();
    }
}
