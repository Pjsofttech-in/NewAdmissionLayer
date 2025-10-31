package com.newadmission.DTO;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeworkSubmissionResponse {

    private Long id;
    private Long homeworkId;
    private String homeworkText;
    private String studentName;
    private String studentEmail;
    private String answerText;
    private String submittedFileUrl;
    private LocalDateTime submittedAt;
    private String status;  // ✅ return status also

}