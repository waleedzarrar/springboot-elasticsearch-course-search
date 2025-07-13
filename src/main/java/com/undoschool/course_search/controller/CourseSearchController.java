package com.undoschool.course_search.controller;

import com.undoschool.course_search.document.CourseDocument;
import com.undoschool.course_search.service.CourseSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CourseSearchController {

    private final CourseSearchService courseSearchService;

    @GetMapping
    public Map<String, Object> searchCourses(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws IOException {

        List<CourseDocument> courses = courseSearchService.searchCourses(
                q, minAge, maxAge, category, type,
                minPrice, maxPrice, startDate, sort, page, size
        );

        Map<String, Object> response = new HashMap<>();
        response.put("total", courses.size());
        response.put("courses", courses);
        return response;
    }
}
