package com.example.demo.image.presentation.converter;

import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.exception.ImageValidationException;
import com.example.demo.image.application.command.UploadImageCommand;
import com.example.demo.image.domain.ImageContentType;
import com.example.demo.image.domain.ImageSize;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * HTTP 입력을 Application 커맨드로 바꾼다. {@code MultipartFile}은 여기까지만 등장한다.
 *
 * <p>형식·크기 규칙은 도메인 타입이 소유한다. 여기서는 HTTP 관심사(빈 part, 본문 읽기 실패)만
 * 판단하고 값 변환에 그친다({@code docs/ARCHITECTURE.md} §8).
 */
@Component
public class ImageCommandConverter {

    public UploadImageCommand toUploadCommand(final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageValidationException(ErrorType.INVALID_IMAGE_FORMAT);
        }
        final byte[] content = readBytes(file);
        return new UploadImageCommand(
                ImageContentType.from(file.getContentType(), content),
                new ImageSize(file.getSize()),
                content);
    }

    private byte[] readBytes(final MultipartFile file) {
        try {
            return file.getBytes();
        } catch (final IOException exception) {
            // 스트림을 읽지 못한 건 저장소 문제가 아니라 요청 본문 문제다.
            throw new ImageValidationException(ErrorType.INVALID_IMAGE_FORMAT);
        }
    }
}
