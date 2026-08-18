package com.example.demo.common.exception;

public enum ErrorType {
    EXTERNAL_API_ERROR("외부 API 호출 중 오류가 발생했습니다."),
    UNKNOWN_ERROR("알 수 없는 오류가 발생했습니다."),
    INVALID_PARAMETER_ERROR("잘못된 매개변수가 전달되었습니다."),
    DUPLICATE_NICKNAME_ERROR("이미 사용 중인 닉네임입니다."),
    DUPLICATE_USER_REPORT_ERROR("동일한 가격 제보가 이미 존재합니다."),
    NO_RESOURCE_ERROR("요청한 리소스를 찾을 수 없습니다."),
    CONFIGURATION_ERROR("애플리케이션 설정값 오류입니다."),
    KAKAO_TOKEN_INVALID("유효하지 않은 Kakao idToken입니다."),
    INVALID_TOKEN("유효하지 않은 인증 토큰입니다."),
    UNAUTHORIZED("로그인이 필요한 서비스입니다."),
    FORBIDDEN("해당 권한이 없습니다."),
    NEWS_UNAVAILABLE("뉴스를 조회할 수 없습니다."),
    STORE_SYNC_ERROR("가게 정보를 동기화할 수 없습니다.");

    private final String description;

    ErrorType(final String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
