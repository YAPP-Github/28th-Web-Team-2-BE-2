package com.example.demo.common.presentation;

public record ApiResponse<T>(String code, String message, T data) {

    public static final String SUCCESS_CODE = "SUCCESS";
    public static final String SUCCESS_MESSAGE = "요청이 성공적으로 처리되었습니다.";
}
