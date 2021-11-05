package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.DashBoardReportDTO;
import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.dto.TransactionDTO;
import com.t1908e.memeportalapi.repository.PostRepository;
import com.t1908e.memeportalapi.repository.TransactionRepository;
import com.t1908e.memeportalapi.repository.UserRepository;
import com.t1908e.memeportalapi.util.RESTResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class DashBoardService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final TransactionRepository transactionRepository;

    public ResponseEntity<?> getDashboardReport() {
        long activeUsersCount = userRepository.countAllByStatusGreaterThan(0);
        int totalActivePosts = postRepository.countAllByStatusGreaterThan(0);
        int totalNewPosts = postRepository.countAllByStatus(1);
        int totalHotPosts = postRepository.countAllByStatus(2);
        int totalTx = transactionRepository.countAllByStatus(2);
        double totalTokenSpent = transactionRepository.getTotalTokenSpent();
        DashBoardReportDTO dashBoardReportDTO = new DashBoardReportDTO();
        dashBoardReportDTO.setHotPosts(totalHotPosts);
        dashBoardReportDTO.setNewPosts(totalNewPosts);
        dashBoardReportDTO.setTotalPosts(totalActivePosts);
        dashBoardReportDTO.setTotalActiveUsers((int) activeUsersCount);
        dashBoardReportDTO.setTotalTransactions(totalTx);
        dashBoardReportDTO.setTotalTokenSpent(totalTokenSpent);
        HashMap<String, ?> restResponse = new RESTResponse.Success()
                .setMessage("OK")
                .setStatus(HttpStatus.OK.value())
                .setData(dashBoardReportDTO).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity getPostCountByCreatedAt(int days) {
        List<Object[]> results = postRepository.countPostByCreatedDate(days);
        List<PostDTO.PostStatisticDTO> statisticDTOList = new ArrayList<>();
        for (Object[] row : results) {
            PostDTO.PostStatisticDTO dto = new PostDTO.PostStatisticDTO();
            Date date = (Date) row[0];
            BigInteger postCountBigInteger = (BigInteger) row[1];
            dto.setPostCount(postCountBigInteger.intValue());
            dto.setCreatedAt(date);
            statisticDTOList.add(dto);
        }
        HashMap<String, Object> restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(statisticDTOList).build();
        return ResponseEntity.ok().body(restResponse);
    }
}
