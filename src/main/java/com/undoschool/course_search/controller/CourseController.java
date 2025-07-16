package com.undoschool.course_search.controller;

import com.undoschool.course_search.dto.CourseSearchCriteria;
import com.undoschool.course_search.service.CourseSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class CourseController {

    private final CourseSearchService courseSearchService;

    @GetMapping
    public ResponseEntity<?> searchCourses(CourseSearchCriteria criteria) {
        try {
            return ResponseEntity.ok(courseSearchService.searchCourses(criteria));
        } catch (IOException e) {
            log.error("Error during search: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error performing search: " + e.getMessage());
        }
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<String>> getSuggestions(@RequestParam String q) {
        try {
            List<String> suggestions = courseSearchService.getSuggestions(q);
            return ResponseEntity.ok(suggestions);
        } catch (IOException e) {
            log.error("Error during suggestion search: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(List.of("Error getting suggestions: " + e.getMessage()));
        }
    }
}