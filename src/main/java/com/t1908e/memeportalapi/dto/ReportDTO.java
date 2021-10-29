package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.Report;
import com.t1908e.memeportalapi.entity.User;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
public class ReportDTO {
    private int id;
    private int type; //1 user report | 2 post report
    private int targetId;
    private String content;
    private Date createdAt;
    private Date updatedAt;
    private int status;
    private UserDTO userDTO;

    public ReportDTO(Report report){
        this.id = report.getId();
        this.type = report.getType();
        this.targetId = report.getTargetId();
        this.content = report.getContent();
        this.createdAt = report.getCreatedAt();
        this.updatedAt = report.getUpdatedAt();
        this.status = report.getStatus();
        this.userDTO = new UserDTO(report.getUser());
    }

    @Data
    public static class CreateReportDTO {
        @NotNull(message = "type is required")
        private int type;
        @NotNull(message = "targetId is required")
        private int targetId;
        private String content;
        @NotNull(message = "userId is required")
        private int userId;
    }

}
