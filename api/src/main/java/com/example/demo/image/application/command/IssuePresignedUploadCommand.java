package com.example.demo.image.application.command;

import com.example.demo.image.domain.ImageContentType;
import com.example.demo.image.domain.ImageSize;

/** 클라이언트 직접 업로드용 presigned PUT 발급 입력. */
public record IssuePresignedUploadCommand(ImageContentType contentType, ImageSize size) {}
