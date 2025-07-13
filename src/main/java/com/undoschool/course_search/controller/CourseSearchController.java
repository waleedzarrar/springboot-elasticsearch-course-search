package com.undoschool.course_search.controller;

import com.undoschool.course_search.document.CourseDocument;
import com.undoschool.course_search.dto.CourseSearchRequest;
import com.undoschool.course_search.service.CourseSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class CourseSearchController {

    private final CourseSearchService searchService;

    @PostMapping("/autocomplete")
    public List<CourseDocument> autocompleteSearch(@RequestBody CourseSearchRequest request) {
        return searchService.autocompleteSearch(request);
    }
}
