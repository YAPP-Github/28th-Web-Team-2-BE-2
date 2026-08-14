package com.example.demo.common.presentation;

public record ApiV1Response<T>(String code, String message, T data) {}
