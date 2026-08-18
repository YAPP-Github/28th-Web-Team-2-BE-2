package com.example.demo.image.application.command;

import com.example.demo.image.domain.ImageContentType;
import com.example.demo.image.domain.ImageSize;

/**
 * 서버 경유 업로드 입력.
 *
 * <p>Presentation이 {@code MultipartFile}에서 형식·크기를 도메인 타입으로 바꿔 넘긴다. Application은
 * multipart라는 전송 방식을 알 필요가 없다.
 */
public record UploadImageCommand(ImageContentType contentType, ImageSize size, byte[] content) {

    public UploadImageCommand {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("image content must not be empty");
        }
    }
}
