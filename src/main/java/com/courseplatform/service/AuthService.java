package com.courseplatform.service;

import com.courseplatform.dto.request.LoginRequestDto;
import com.courseplatform.dto.request.RegisterRequestDto;
import com.courseplatform.dto.response.AuthResponseDto;
import com.courseplatform.dto.response.RegisterResponseDto;

public interface AuthService {
	RegisterResponseDto register( RegisterRequestDto request );
	AuthResponseDto login( LoginRequestDto request );
}
