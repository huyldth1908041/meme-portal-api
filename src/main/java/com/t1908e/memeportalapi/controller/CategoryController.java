package com.t1908e.memeportalapi.controller;

import com.t1908e.memeportalapi.dto.CategoryDTO;
import com.t1908e.memeportalapi.service.CategoryService;
import com.t1908e.memeportalapi.util.RESTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@CrossOrigin
public class CategoryController {
    private final CategoryService categoryService;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<?> getAll() {
        return categoryService.getAllCategories();
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<?> createCategory(@RequestBody CategoryDTO.CreateCategoryDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "Save category failed");
        }

        return categoryService.createCategory(dto.getName());
    }


    @RequestMapping(value = "", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteCategory(@RequestBody CategoryDTO.CreateCategoryDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "Save category failed");
        }

        return categoryService.deleteCate(dto.getName());
    }
}
