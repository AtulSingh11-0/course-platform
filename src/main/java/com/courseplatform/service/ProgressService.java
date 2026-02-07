package com.courseplatform.service;

import com.courseplatform.dto.response.ProgressReportResponseDto;
import com.courseplatform.dto.response.SubtopicCompletionResponseDto;

public interface ProgressService {
	SubtopicCompletionResponseDto markAsCompleted(String userEmail, String subtopicId);
	ProgressReportResponseDto getProgressReport(String userEmail, Long enrollmentId);
}
