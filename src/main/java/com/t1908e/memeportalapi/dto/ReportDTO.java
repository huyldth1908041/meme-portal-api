package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.Report;
import com.t1908e.memeportalapi.enums.ReportType;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class ReportDTO {
    private int id;
    private ReportType type; //1 user report | 2 post report
    private int targetId;
    private String content;
    private Date createdAt;
    private Date updatedAt;
    private int status;
    private UserDTO reporter;

    public ReportDTO(Report report) {
        this.id = report.getId();
        this.type = report.getType();
        this.targetId = report.getTargetId();
        this.createdAt = report.getCreatedAt();
        this.updatedAt = report.getUpdatedAt();
        this.status = report.getStatus();
        this.reporter = new UserDTO(report.getUser());
        this.content = report.getContent();
    }

    @Data
    public static class CreateReportDTO {
        private ReportType type;
        private int targetId;
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ReportPostDTO {
        @NotNull(message = "post id is required")
        private int postId;
        @NotBlank(message = "content is required")
        private String content;
    }
}
