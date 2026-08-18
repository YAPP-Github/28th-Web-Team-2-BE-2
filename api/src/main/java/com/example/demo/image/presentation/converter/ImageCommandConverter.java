package com.example.demo.image.presentation.converter;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.image.application.command.IssuePresignedUploadCommand;
import com.example.demo.image.application.command.UploadImageCommand;
import com.example.demo.image.domain.ImageContentType;
import com.example.demo.image.domain.ImageSize;
import com.example.demo.image.presentation.dto.PresignedUploadRequest;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** HTTP 입력을 Application 커맨드로 바꾼다. {@code MultipartFile}은 여기까지만 등장한다. */
@Component
public class ImageCommandConverter {

    public UploadImageCommand toUploadCommand(final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw emptyImage();
        }
        return new UploadImageCommand(
                ImageContentType.from(file.getContentType()),
                new ImageSize(file.getSize()),
                readBytes(file));
    }

    public IssuePresignedUploadCommand toPresignedCommand(final PresignedUploadRequest request) {
        return new IssuePresignedUploadCommand(
                ImageContentType.from(request.contentType()), new ImageSize(request.size()));
    }

    private byte[] readBytes(final MultipartFile file) {
        try {
            return file.getBytes();
        } catch (final IOException exception) {
            // 스트림을 읽지 못한 건 저장소 문제가 아니라 요청 본문 문제다.
            throw new ApiException(
                    ErrorType.INVALID_IMAGE_FORMAT.description(),
                    ErrorType.INVALID_IMAGE_FORMAT,
                    HttpStatus.BAD_REQUEST,
                    exception);
        }
    }

    private ApiException emptyImage() {
        return new ApiException(
                ErrorType.INVALID_IMAGE_FORMAT.description(),
                ErrorType.INVALID_IMAGE_FORMAT,
                HttpStatus.BAD_REQUEST);
    }
}
