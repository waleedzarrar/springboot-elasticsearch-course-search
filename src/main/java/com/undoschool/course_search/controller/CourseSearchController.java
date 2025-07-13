package com.undoschool.course_search.controller;

import com.undoschool.course_search.document.CourseDocument;
import com.undoschool.course_search.service.CourseSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class CourseSearchController {

    private final CourseSearchService courseSearchService;

    @GetMapping
    public List<CourseDocument> searchCourses(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String startDate
    ) throws IOException {
        return courseSearchService.searchCourses(q, minAge, maxAge, category, type, minPrice, maxPrice, startDate);
    }
}
