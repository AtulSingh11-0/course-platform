package com.courseplatform.search.service;

import com.courseplatform.dto.response.SearchResponseDto;

public interface SearchService {
	SearchResponseDto searchCourses( String query);
}
