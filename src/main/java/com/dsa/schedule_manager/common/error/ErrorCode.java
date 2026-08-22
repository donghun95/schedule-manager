package com.dsa.schedule_manager.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT("E_400_001", HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    CANNOT_ADD_OWNER_AS_PARTICIPANT(
            "E_400_002", HttpStatus.BAD_REQUEST, "작성자는 참여자로 추가할 수 없습니다."),
    INVALID_CURSOR(
            "E_400_003", HttpStatus.BAD_REQUEST, "커서 시간과 커서 ID는 함께 전달해야 합니다."),
    INVALID_DATE_RANGE(
            "E_400_004", HttpStatus.BAD_REQUEST, "조회 시작 일시는 종료 일시보다 늦을 수 없습니다."),
    EMAIL_ALREADY_USED("E_409_001", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    UNAUTHORIZED("E_401_001", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN("E_403_001", HttpStatus.FORBIDDEN, "권한이 없습니다."),
    SCHEDULE_NOT_FOUND("E_404_001", HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."),
    PARTICIPANT_NOT_FOUND(
            "E_404_002", HttpStatus.NOT_FOUND, "참여자를 찾을 수 없습니다."),
    USER_NOT_FOUND("E_404_003", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED("E_405_001", HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    SCHEDULE_CONFLICT("E_409_002", HttpStatus.CONFLICT,
            "다른 사용자가 먼저 수정했습니다. 다시 조회 후 시도해 주세요."),
    DATA_INTEGRITY_CONFLICT("E_409_003", HttpStatus.CONFLICT,
            "요청이 데이터 제약 조건과 충돌합니다."),
    INVALID_STATUS_TRANSITION(
            "E_409_004", HttpStatus.CONFLICT, "현재 상태에서는 요청한 상태로 변경할 수 없습니다."),
    INVALID_PARTICIPANT_STATUS_TRANSITION(
            "E_409_008",
            HttpStatus.CONFLICT,
            "현재 참여 상태에서는 요청을 처리할 수 없습니다."
    ),
    INVITATION_CLOSED("E_409_009", HttpStatus.CONFLICT, "종료되거나 취소된 일정의 초대는 처리할 수 없습니다."
    ),
    SAME_STATUS_TRANSITION(
            "E_409_005", HttpStatus.CONFLICT, "이미 요청한 상태입니다."),
    PARTICIPANT_ALREADY_EXISTS(
            "E_409_006", HttpStatus.CONFLICT, "이미 참여 중인 사용자입니다."),
    SCHEDULE_DELETE_NOT_ALLOWED(
            "E_409_007", HttpStatus.CONFLICT, "예정 상태의 일정만 삭제할 수 있습니다."),
    UNSUPPORTED_MEDIA_TYPE("E_415_001", HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "지원하지 않는 Content-Type입니다."),
    INTERNAL_ERROR("E_500_001", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다.");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;
}
