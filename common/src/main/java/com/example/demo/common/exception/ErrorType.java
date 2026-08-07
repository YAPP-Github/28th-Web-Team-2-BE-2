package com.example.demo.common.exception;

public enum ErrorType {
    EXTERNAL_API_ERROR("외부 API 호출 중 오류가 발생했습니다."),
    UNKNOWN_ERROR("알 수 없는 오류가 발생했습니다."),
    INVALID_PARAMETER_ERROR("잘못된 매개변수가 전달되었습니다."),
    NO_RESOURCE_ERROR("요청한 리소스를 찾을 수 없습니다.");

    private final String description;

    ErrorType(final String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
