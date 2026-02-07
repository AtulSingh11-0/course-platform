package com.courseplatform.service;

import com.courseplatform.dto.response.CourseDetailResponseDto;
import com.courseplatform.dto.response.CourseSummaryResponseDto;
import com.courseplatform.dto.response.SearchResponseDto;

import java.util.List;

public interface CourseService {
	List< CourseSummaryResponseDto > getAllCourseSummaries();
	CourseDetailResponseDto getCourseDetailById(String courseId);
	SearchResponseDto search(String query);
	SearchResponseDto searchSemantic(String query);
}
