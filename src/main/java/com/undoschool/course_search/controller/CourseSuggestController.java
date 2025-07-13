package com.undoschool.course_search.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CourseSuggestController {

    private final ElasticsearchClient elasticsearchClient;

    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam String q) throws IOException {
        SearchRequest request = SearchRequest.of(s -> s
                .index("courses")
                .suggest(sg -> sg
                        .suggesters("course-suggest", sgt -> sgt
                                .prefix(q)
                                .completion(c -> c
                                        .field("suggest")
                                        .skipDuplicates(true)
                                        .size(10)
                                )
                        )
                )
        );

        SearchResponse<Object> response = elasticsearchClient.search(request, Object.class);

        List<Suggestion<Object>> suggestions = response.suggest().get("course-suggest");

        return suggestions.stream()
                .flatMap(suggestion -> suggestion.completion().options().stream())
                .map(option -> option.text())
                .toList();
    }
}
