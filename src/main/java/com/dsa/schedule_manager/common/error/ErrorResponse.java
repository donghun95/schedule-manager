package com.dsa.schedule_manager.common.error;

public record ErrorResponse(
        int status,
        String code,
        String message
) {
    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getDefaultMessage()
        );
    }
    public static ErrorResponse from(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return new ErrorResponse(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                ex.getMessage()
        );
    }
}