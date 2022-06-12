package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.CategoryDTO;
import com.t1908e.memeportalapi.dto.UserDTO;
import com.t1908e.memeportalapi.entity.Category;
import com.t1908e.memeportalapi.repository.CategoryRepository;
import com.t1908e.memeportalapi.util.RESTResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public ResponseEntity<?> getAllCategories() {
        List<Category> activeCategories = categoryRepository.findCategoryByStatus(1);
        List<CategoryDTO> categoryDTOS = activeCategories.stream().map(item -> new CategoryDTO(item)).collect(Collectors.toList());
        HashMap<String, Object> success = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(categoryDTOS).build();

        return ResponseEntity.ok().body(success);
    }

    public ResponseEntity<?> createCategory(String cateName) {
        List<Category> existedCate = categoryRepository.findAllByName(cateName);
        if(existedCate.size() > 0) {
            HashMap<String, Object> error  = new RESTResponse.CustomError()
                    .setMessage("Category already exists")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(error);
        }
        Category category = new Category();
        category.setName(cateName);
        category.setStatus(1);
        category.setCreatedAt(new Date());
        category.setUpdatedAt(new Date());
        Category save = categoryRepository.save(category);
        HashMap<String, Object> success = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(new CategoryDTO(save)).build();

        return ResponseEntity.ok().body(success);
    }

    public ResponseEntity<?> deleteCate(String cateName) {
        List<Category> existedCate = categoryRepository.findAllByName(cateName);
        if(existedCate.size() <= 0) {
            HashMap<String, Object> error  = new RESTResponse.CustomError()
                    .setMessage("Not found")
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .build();
            return ResponseEntity.badRequest().body(error);
        }
        categoryRepository.deleteAll(existedCate);
        HashMap<String, Object> success = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData("Deleted").build();

        return ResponseEntity.ok().body(success);
    }


}
