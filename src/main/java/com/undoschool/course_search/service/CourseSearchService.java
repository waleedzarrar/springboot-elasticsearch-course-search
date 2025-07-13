package com.undoschool.course_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.undoschool.course_search.document.CourseDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseSearchService {

    private final ElasticsearchClient elasticsearchClient;

    public List<CourseDocument> searchCourses(
            String keyword,
            Integer minAge,
            Integer maxAge,
            String category,
            String type,
            Double minPrice,
            Double maxPrice,
            LocalDateTime startDate,
            String sort,
            int page,
            int size
    ) throws IOException {

        List<Query> filters = new ArrayList<>();

        if (category != null && !category.isBlank()) {
            filters.add(TermQuery.of(t -> t.field("category.keyword").value(category))._toQuery());
        }

        if (type != null && !type.isBlank()) {
            filters.add(TermQuery.of(t -> t.field("type.keyword").value(type))._toQuery());
        }

        if (minAge != null || maxAge != null) {
            RangeQuery.Builder ageRange = new RangeQuery.Builder().field("minAge");
            if (minAge != null) ageRange.gte(JsonData.of(minAge));
            if (maxAge != null) ageRange.lte(JsonData.of(maxAge));
            filters.add(ageRange.build()._toQuery());
        }

        if (minPrice != null || maxPrice != null) {
            RangeQuery.Builder priceRange = new RangeQuery.Builder().field("price");
            if (minPrice != null) priceRange.gte(JsonData.of(minPrice));
            if (maxPrice != null) priceRange.lte(JsonData.of(maxPrice));
            filters.add(priceRange.build()._toQuery());
        }

        if (startDate != null) {
            filters.add(RangeQuery.of(r -> r
                    .field("nextSessionDate")
                    .gte(JsonData.of(startDate.toString()))
            )._toQuery());
        }

        Query textQuery = (keyword != null && !keyword.isBlank())
                ? MultiMatchQuery.of(m -> m
                    .fields("title", "description")
                    .query(keyword)
                )._toQuery()
                : MatchAllQuery.of(m -> m)._toQuery();

        Query finalQuery = BoolQuery.of(b -> b
                .must(textQuery)
                .filter(filters)
        )._toQuery();

        // ✅ Create SortOptions directly (no variable reassignment)
        SortOptions sortOptions = SortOptions.of(s -> s.field(f -> f
                .field(resolveSortField(sort))
                .order(resolveSortOrder(sort))
        ));

        SearchRequest request = SearchRequest.of(s -> s
                .index("courses")
                .query(finalQuery)
                .from(page * size)
                .size(size)
                .sort(sortOptions)
        );

        SearchResponse<CourseDocument> response = elasticsearchClient.search(request, CourseDocument.class);
        return response.hits().hits().stream()
                .map(hit -> hit.source())
                .toList();
    }

    // ✅ Helper method to resolve field name
    private String resolveSortField(String sort) {
        if (sort == null) return "nextSessionDate";
        return switch (sort.toLowerCase()) {
            case "priceasc", "pricedesc" -> "price";
            case "upcoming" -> "nextSessionDate";
            default -> "nextSessionDate";
        };
    }

    // ✅ Helper method to resolve sort order
    private SortOrder resolveSortOrder(String sort) {
        if (sort == null) return SortOrder.Asc;
        return switch (sort.toLowerCase()) {
            case "priceasc" -> SortOrder.Asc;
            case "pricedesc" -> SortOrder.Desc;
            case "upcoming" -> SortOrder.Asc;
            default -> SortOrder.Asc;
        };
    }
}
