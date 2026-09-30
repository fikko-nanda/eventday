package com.example.eventday.controller;

import com.example.eventday.dto.CategoryResponse;
import com.example.eventday.model.Category;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*") // Sesuaikan CORS jika diperlukan
public class CategoryController {

    @GetMapping
    public List<CategoryResponse> getAllCategories() {
        return Arrays.stream(Category.values())
                .map(cat -> new CategoryResponse(cat.name(), cat.getLabel()))
                .collect(Collectors.toList());
    }
}