package com.dsa.schedule_manager.common.error;

import java.util.List;

public record ErrorResponse(
        int status,
        String code,
        String message,
        String traceId,
        List<FieldError> fieldErrors
) {
    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                org.slf4j.MDC.get("traceId"),
                List.of()
        );
    }

    public static ErrorResponse from(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return new ErrorResponse(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                ex.getMessage(),
                org.slf4j.MDC.get("traceId"),
                List.of()
        );
    }

    public static ErrorResponse validation(List<FieldError> fieldErrors) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        return new ErrorResponse(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                org.slf4j.MDC.get("traceId"),
                List.copyOf(fieldErrors)
        );
    }

    public record FieldError(
            String field,
            String message
    ) {
        public static FieldError from(org.springframework.validation.FieldError error) {
            return new FieldError(error.getField(), error.getDefaultMessage());
        }
    }
}
