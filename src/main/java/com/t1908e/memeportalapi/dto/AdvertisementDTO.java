package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.Advertisement;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;

@Data
@NoArgsConstructor
public class AdvertisementDTO {
    private int id;
    private String url;
    private String image;
    private String title;
    private String content;
    private int status;
    private Date createdAt;
    private Date updatedAt;
    private UserDTO creator;

    public AdvertisementDTO(Advertisement advertisement) {
        this.content = advertisement.getContent();
        this.id = advertisement.getId();
        this.url = advertisement.getUrl();
        this.image = advertisement.getImage();
        this.title = advertisement.getTitle();
        this.status = advertisement.getStatus();
        this.createdAt = advertisement.getCreatedAt();
        this.updatedAt = advertisement.getUpdatedAt();
        this.creator = new UserDTO(advertisement.getUser());
    }

    @Data
    public static class CreateAdvertisementDTO {
        @NotBlank(message = "url is required")
        private String url;
        @NotBlank(message = "image is required")
        private String image;
        @NotBlank(message = "title is required")
        private String title;
        private String content;
    }

    @Data
    public static class VerifyAdsDTO {
        @NotEmpty(message = "list ids is required")
        private ArrayList<Integer> listIds;
    }
}
