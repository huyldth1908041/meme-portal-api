package com.t1908e.memeportalapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashBoardReportDTO {
    private int totalActiveUsers;
    private int totalPosts;
    private int hotPosts;
    private int newPosts;
    private int totalTransactions;
    private double totalTokenSpent;
}
