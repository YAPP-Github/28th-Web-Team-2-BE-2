package com.example.demo.image.presentation.converter;

import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.exception.ImageValidationException;
import com.example.demo.image.application.command.UploadImageCommand;
import com.example.demo.image.domain.ImageContentType;
import com.example.demo.image.domain.ImageSize;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** HTTP 입력을 Application 커맨드로 바꾼다. {@code MultipartFile}은 여기까지만 등장한다. */
@Component
public class ImageCommandConverter {

    public UploadImageCommand toUploadCommand(final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageValidationException(ErrorType.INVALID_IMAGE_FORMAT);
        }
        final ImageContentType contentType = ImageContentType.from(file.getContentType());
        final byte[] content = readBytes(file);
        // 신고된 Content-Type 만 믿으면 인증 사용자가 우리 버킷을 임의 파일 호스트로 쓸 수 있다.
        if (!contentType.matchesSignature(content)) {
            throw new ImageValidationException(ErrorType.INVALID_IMAGE_FORMAT);
        }
        return new UploadImageCommand(contentType, new ImageSize(file.getSize()), content);
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
