package com.t1908e.memeportalapi.dto;

import lombok.*;

import java.math.BigInteger;


@Getter
@Setter
@NoArgsConstructor
public class TopCreatorDTO {
    private TopCreatorUser user;
    private int postCounts;

    public TopCreatorDTO(long id, String fullName, String avatar, int postCounts) {
        this.postCounts = postCounts;
        this.user = new TopCreatorUser(fullName, avatar, id);
    }

    @Data
    @AllArgsConstructor
    private static class TopCreatorUser {
        private String fullName;
        private String avatar;
        private long id;
    }

}
