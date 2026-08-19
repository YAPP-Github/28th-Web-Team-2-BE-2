package com.example.demo.image.application.usecase;

import com.example.demo.image.application.command.UploadImageCommand;
import com.example.demo.image.application.port.ImageStoragePort;
import com.example.demo.image.application.result.UploadedImageResult;
import com.example.demo.image.domain.ImageKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 서버를 거쳐 이미지를 저장한다.
 *
 * <p>{@code @Transactional}을 붙이지 않는다. DB를 건드리지 않고 외부 네트워크 호출만 하므로
 * 트랜잭션 안에 둘 이유가 없다({@code docs/ARCHITECTURE.md} §6).
 */
@Service
@RequiredArgsConstructor
public class UploadImageUseCase {

    private final ImageStoragePort imageStoragePort;

    public UploadedImageResult execute(final UploadImageCommand command) {
        final ImageKey key = ImageKey.generate(command.contentType());
        return new UploadedImageResult(imageStoragePort.uploadAndReturnUrl(key, command));
    }
}
