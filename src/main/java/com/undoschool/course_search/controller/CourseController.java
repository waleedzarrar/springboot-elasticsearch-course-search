package com.undoschool.course_search.controller;

import com.undoschool.course_search.document.CourseDocument;
import com.undoschool.course_search.dto.CourseSearchCriteria;
import com.undoschool.course_search.service.CourseSearchService;
import com.undoschool.course_search.service.CourseSuggestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // <<< ADD THIS IMPORT
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j // <<< ADD THIS ANNOTATION
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class CourseController {

    private final CourseSearchService searchService;
    private final CourseSuggestService courseSuggestService;

    /**
     * Main search endpoint for courses. Handles full-text search, filtering, sorting, and pagination.
     * This endpoint covers the requirements of Assignment A and the fuzzy search enhancement of Assignment B.
     *
     * @param criteria Comprehensive search criteria from query parameters.
     * @return A ResponseEntity containing a map with "total" hits and "courses" (list of CourseDocument).
     * @throws IOException If there's an issue communicating with Elasticsearch.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> searchCourses(CourseSearchCriteria criteria) throws IOException {
        log.info("Received search request: {}", criteria); // This line should now be fine
        List<CourseDocument> courses = searchService.searchCourses(criteria);

        Map<String, Object> response = Map.of(
                "total", courses.size(),
                "courses", courses
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint for providing autocomplete suggestions for course titles/categories.
     * This endpoint covers the requirements of Assignment B - Part 1.
     *
     * @param query The partial text query for suggestions.
     * @return A list of suggested course titles/categories.
     * @throws IOException If there's an issue communicating with Elasticsearch.
     */
    @GetMapping("/suggest")
    public List<String> suggestCourses(@RequestParam("q") String query) throws IOException {
        return courseSuggestService.suggestCourses(query);
    }
}