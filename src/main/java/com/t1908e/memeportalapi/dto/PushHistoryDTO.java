package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.PushHistory;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class PushHistoryDTO {
    private double amount;
    private UserDTO pusher;
    private Date createdAt;
    private Date updatedAt;

    public PushHistoryDTO(PushHistory pushHistory) {
        this.amount = pushHistory.getTokenAmount();
        this.pusher = new UserDTO(pushHistory.getUser());
        this.createdAt = pushHistory.getCreatedAt();
        this.updatedAt = pushHistory.getUpdateAt();
    }
}
