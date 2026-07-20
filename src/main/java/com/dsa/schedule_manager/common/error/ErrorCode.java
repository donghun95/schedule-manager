package com.dsa.schedule_manager.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT("E_400_001", HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    EMAIL_ALREADY_USED("E_409_001", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    FORBIDDEN("E_403_001", HttpStatus.FORBIDDEN, "권한이 없습니다."), // 신규
    SCHEDULE_NOT_FOUND("E_404_001", HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),
    SCHEDULE_CONFLICT("E_409_002", HttpStatus.CONFLICT,"다른 사용자가 먼저 수정했습니다. 다시 조회 후 시도해 주세요."),
    DATA_INTEGRITY_CONFLICT("E_409_003", HttpStatus.CONFLICT, "요청이 데이터 제약 조건과 충돌합니다."),
    UNAUTHORIZED("E_401_001", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INTERNAL_ERROR("E_500_001", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다.");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;
}