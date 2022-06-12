package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.Transaction;
import com.t1908e.memeportalapi.enums.TransactionType;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDTO {
    private int id;
    private TransactionType type;
    private double amount;
    private String reason;
    private long targetId;

    public TransactionDTO(Transaction transaction) {
        this.id = transaction.getId();
        this.type = transaction.getType();
        this.amount = transaction.getAmount();
        this.reason = transaction.getReason();
        this.targetId = transaction.getTargetId();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TransferTokenDTO {
        @NotNull(message = "amount is required")
        private double amount;
        @NotBlank(message = "reason is required")
        private String reason;
        @NotNull(message = "receiver id is required")
        private long receiverId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PushHotDTO {
        @NotNull(message = "amount is required")
        private double amount;
        @NotNull(message = "post id is required")
        private long postId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProcessTransactionDTO {
        @NotNull(message = "tx id is required")
        private int txId;
        @NotBlank(message = "verify code is required")
        private String verifyCode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class GiveTokenDTO {
        @NotNull(message = "userId is required")
        private long userId;
        @NotNull(message = "amount is required")
        private double amount;
    }
}
