package com.undoschool.course_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseSuggestService {

    private final ElasticsearchClient elasticsearchClient;

    public List<String> suggestCourses(String query) throws IOException {
        SearchResponse<Void> response = elasticsearchClient.search(s -> s
                        .index("courses")
                        .suggest(sg -> sg
                                .suggesters("course-suggest", sgt -> sgt
                                        .prefix(query)
                                        .completion(c -> c
                                                .field("suggest")
                                                .skipDuplicates(true)
                                                .size(10)
                                        )
                                )
                        ),
                Void.class
        );

        List<? extends Suggestion<?>> rawSuggestions = response.suggest().get("course-suggest");

        if (rawSuggestions == null || rawSuggestions.isEmpty()) {
            return Collections.emptyList();
        }

        return rawSuggestions.stream()
                .flatMap(suggestion -> {
                    if (suggestion.completion() != null) {
                        return suggestion.completion().options().stream();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .map(CompletionSuggestOption::text) // This is correct: option.text() returns String
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}