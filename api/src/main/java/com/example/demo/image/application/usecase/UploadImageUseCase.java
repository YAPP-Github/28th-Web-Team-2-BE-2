package com.example.demo.image.application.usecase;

import com.example.demo.image.application.command.UploadImageCommand;
import com.example.demo.image.application.port.ImageStoragePort;
import com.example.demo.image.application.result.UploadedImageResult;
import com.example.demo.image.domain.ImageKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 서버를 거쳐 이미지를 저장한다.
 *
 * <p>{@code @Transactional}을 붙이지 않는다. DB를 건드리지 않고 외부 네트워크 호출만 하므로
 * 트랜잭션 안에 둘 이유가 없다({@code docs/ARCHITECTURE.md} §6).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UploadImageUseCase {

    private final ImageStoragePort imageStoragePort;

    public UploadedImageResult execute(final UploadImageCommand command) {
        return execute(ImageKey.generate(command.contentType(), command.extension()), command);
    }

    public UploadedImageResult execute(final ImageKey key, final UploadImageCommand command) {
        log.info(
                "image upload started key={} contentType={} sizeBytes={}",
                key.value(), command.contentType().mimeType(), command.size().bytes());
        final String imageUrl = imageStoragePort.uploadAndReturnUrl(key, command);
        log.info(
                "image upload completed key={} contentType={} sizeBytes={}",
                key.value(), command.contentType().mimeType(), command.size().bytes());
        return new UploadedImageResult(imageUrl);
    }
}
