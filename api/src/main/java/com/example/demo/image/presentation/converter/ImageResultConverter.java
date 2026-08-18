package com.example.demo.image.presentation.converter;

import com.example.demo.image.application.result.PresignedUploadResult;
import com.example.demo.image.application.result.UploadedImageResult;
import com.example.demo.image.presentation.dto.ImageUploadResponse;
import com.example.demo.image.presentation.dto.PresignedUploadResponse;
import org.springframework.stereotype.Component;

/** Application 결과를 HTTP 응답으로 바꾼다. */
@Component
public class ImageResultConverter {

    public ImageUploadResponse toUploadResponse(final UploadedImageResult result) {
        return new ImageUploadResponse(result.imageUrl());
    }

    public PresignedUploadResponse toPresignedResponse(final PresignedUploadResult result) {
        return new PresignedUploadResponse(
                result.uploadUrl(),
                result.imageUrl(),
                result.method(),
                result.expiresAt(),
                result.contentType());
    }
}
