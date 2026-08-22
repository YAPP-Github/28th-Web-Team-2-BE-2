package com.example.demo.image.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.image.application.command.UploadImageCommand;
import com.example.demo.image.application.port.ImageStoragePort;
import com.example.demo.image.application.result.UploadedImageResult;
import com.example.demo.image.domain.ImageContentType;
import com.example.demo.image.domain.ImageKey;
import com.example.demo.image.domain.ImageSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class UploadImageUseCaseTest {

    private ImageStoragePort imageStoragePort;
    private UploadImageUseCase uploadImageUseCase;

    @BeforeEach
    void setUp() {
        imageStoragePort = mock(ImageStoragePort.class);
        uploadImageUseCase = new UploadImageUseCase(imageStoragePort);
    }

    @Test
    void 업로드하면_저장소가_돌려준_영구_URL을_반환한다() {
        when(imageStoragePort.uploadAndReturnUrl(any(), any())).thenReturn("https://cdn.example.com/images/a.jpg");

        final UploadedImageResult result = uploadImageUseCase.execute(uploadCommand());

        assertThat(result.imageUrl()).isEqualTo("https://cdn.example.com/images/a.jpg");
    }

    @Test
    void 업로드_성공_로그에_저장소_추적값을_남긴다(final CapturedOutput output) {
        when(imageStoragePort.uploadAndReturnUrl(any(), any()))
                .thenReturn("https://cdn.example.com/images/a.jpg");

        uploadImageUseCase.execute(uploadCommand());

        assertThat(output)
                .contains("image upload completed")
                .contains("contentType=image/jpeg")
                .contains("sizeBytes=3");
    }

    // 클라이언트가 보낸 파일명을 key에 쓰면 경로 조작과 서명 깨지는 문자가 들어온다.
    @Test
    void key는_형식에_맞는_UUID로_생성해_저장소에_넘긴다() {
        uploadImageUseCase.execute(uploadCommand());

        final ArgumentCaptor<ImageKey> captor = ArgumentCaptor.forClass(ImageKey.class);
        verify(imageStoragePort).uploadAndReturnUrl(captor.capture(), any());
        assertThat(captor.getValue().value()).matches("^images/[0-9a-f-]{36}\\.jpg$");
    }

    private UploadImageCommand uploadCommand() {
        return new UploadImageCommand(ImageContentType.JPEG, new ImageSize(3L), new byte[] {1, 2, 3});
    }
}
