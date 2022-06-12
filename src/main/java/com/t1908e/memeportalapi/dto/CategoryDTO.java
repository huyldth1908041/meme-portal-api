package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.Category;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Date;

@Data
@NoArgsConstructor
public class CategoryDTO {
    private int id;
    private String name;
    private int status;

    public CategoryDTO(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.status = category.getStatus();
    }

    @Data
    public static class CreateCategoryDTO {
        @NotBlank
        private String name;
    }
}
