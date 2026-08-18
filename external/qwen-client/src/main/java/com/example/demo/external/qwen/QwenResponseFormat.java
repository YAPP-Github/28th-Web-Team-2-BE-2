package com.example.demo.external.qwen;

/** {@code response_format}. JSON 객체만 받겠다는 선언이다. */
public record QwenResponseFormat(String type) {

    private static final String JSON_OBJECT = "json_object";

    public static QwenResponseFormat jsonObject() {
        return new QwenResponseFormat(JSON_OBJECT);
    }
}
