package com.example.demo.image.presentation.converter;

import com.example.demo.image.application.result.UploadedImageResult;
import com.example.demo.image.presentation.dto.ImageUploadResponse;
import org.springframework.stereotype.Component;

/** Application 결과를 HTTP 응답으로 바꾼다. */
@Component
public class ImageResultConverter {

    public ImageUploadResponse toUploadResponse(final UploadedImageResult result) {
        return new ImageUploadResponse(result.imageUrl());
    }

}
