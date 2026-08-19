package com.example.demo.image.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.image.application.command.IssuePresignedUploadCommand;
import com.example.demo.image.application.command.UploadImageCommand;
import com.example.demo.image.application.port.ImageStoragePort;
import com.example.demo.image.application.result.PresignedUploadResult;
import com.example.demo.image.application.result.UploadedImageResult;
import com.example.demo.image.domain.ImageContentType;
import com.example.demo.image.domain.ImageKey;
import com.example.demo.image.domain.ImageSize;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ImageUseCaseTest {

    private ImageStoragePort imageStoragePort;
    private UploadImageUseCase uploadImageUseCase;
    private IssuePresignedUploadUseCase issuePresignedUploadUseCase;

    @BeforeEach
    void setUp() {
        imageStoragePort = mock(ImageStoragePort.class);
        uploadImageUseCase = new UploadImageUseCase(imageStoragePort);
        issuePresignedUploadUseCase = new IssuePresignedUploadUseCase(imageStoragePort);
    }

    @Test
    void 업로드하면_저장소가_돌려준_영구_URL을_반환한다() {
        when(imageStoragePort.uploadAndReturnUrl(any(), any())).thenReturn("https://cdn.example.com/images/a.jpg");

        final UploadedImageResult result = uploadImageUseCase.execute(uploadCommand());

        assertThat(result.imageUrl()).isEqualTo("https://cdn.example.com/images/a.jpg");
    }

    // 클라이언트가 보낸 파일명을 key에 쓰면 경로 조작과 서명 깨지는 문자가 들어온다.
    @Test
    void key는_형식에_맞는_UUID로_생성해_저장소에_넘긴다() {
        uploadImageUseCase.execute(uploadCommand());

        final ArgumentCaptor<ImageKey> captor = ArgumentCaptor.forClass(ImageKey.class);
        verify(imageStoragePort).uploadAndReturnUrl(captor.capture(), any());
        assertThat(captor.getValue().value()).matches("^images/[0-9a-f-]{36}\\.jpg$");
    }

    @Test
    void presigned_발급은_저장소_결과를_그대로_반환한다() {
        final PresignedUploadResult expected = new PresignedUploadResult(
                "https://s3.example.com/put", "https://cdn.example.com/images/a.png",
                PresignedUploadResult.PUT_METHOD, Instant.parse("2026-08-19T00:10:00Z"), "image/png");
        when(imageStoragePort.presign(any(), any())).thenReturn(expected);

        final PresignedUploadResult result = issuePresignedUploadUseCase.execute(
                new IssuePresignedUploadCommand(ImageContentType.PNG, new ImageSize(2048L)));

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void presigned_key도_형식_확장자를_따른다() {
        issuePresignedUploadUseCase.execute(
                new IssuePresignedUploadCommand(ImageContentType.PNG, new ImageSize(2048L)));

        final ArgumentCaptor<ImageKey> captor = ArgumentCaptor.forClass(ImageKey.class);
        verify(imageStoragePort).presign(captor.capture(), any());
        assertThat(captor.getValue().value()).endsWith(".png");
    }

    private UploadImageCommand uploadCommand() {
        return new UploadImageCommand(ImageContentType.JPEG, new ImageSize(3L), new byte[] {1, 2, 3});
    }
}
