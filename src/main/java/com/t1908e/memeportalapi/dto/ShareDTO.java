package com.t1908e.memeportalapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class ShareDTO {
    private int shareCount;

    @Data
    @AllArgsConstructor
    public static class CheckShareDTO {
        private boolean canShare;
    }
}
