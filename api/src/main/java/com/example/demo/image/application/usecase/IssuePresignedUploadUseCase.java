package com.example.demo.image.application.usecase;

import com.example.demo.image.application.command.IssuePresignedUploadCommand;
import com.example.demo.image.application.port.ImageStoragePort;
import com.example.demo.image.application.result.PresignedUploadResult;
import com.example.demo.image.domain.ImageKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 클라이언트가 S3로 직접 PUT할 수 있는 만료 URL을 발급한다. */
@Service
@RequiredArgsConstructor
public class IssuePresignedUploadUseCase {

    private final ImageStoragePort imageStoragePort;

    public PresignedUploadResult execute(final IssuePresignedUploadCommand command) {
        final ImageKey key = ImageKey.generate(command.contentType());
        return imageStoragePort.presign(key, command.contentType(), command.size());
    }
}
