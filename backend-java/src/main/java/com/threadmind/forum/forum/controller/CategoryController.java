package com.threadmind.forum.forum.controller;

import com.threadmind.forum.common.api.ApiResponse;
import com.threadmind.forum.common.web.TraceContext;
import com.threadmind.forum.forum.dto.CategoryDto;
import com.threadmind.forum.forum.service.ForumDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final ForumDemoService forumDemoService;

    public CategoryController(ForumDemoService forumDemoService) {
        this.forumDemoService = forumDemoService;
    }

    @GetMapping
    public ApiResponse<List<CategoryDto>> listCategories() {
        return ApiResponse.ok(forumDemoService.listCategories(), TraceContext.currentTraceId());
    }
}

