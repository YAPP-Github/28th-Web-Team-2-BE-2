package com.example.demo.image.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.exception.ImageValidationException;
import com.example.demo.image.application.command.UploadImageCommand;
import com.example.demo.image.domain.ImageContentType;
import com.example.demo.image.domain.ImageKey;
import com.example.demo.image.domain.ImageSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3ImageStorageAdapterTest {

    private static final ImageKey KEY = new ImageKey("images/abc.jpg");

    private S3Client s3Client;
    private S3ImageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        adapter = new S3ImageStorageAdapter(
                s3Client, new S3ImageStorageProperties("marketgo-images", "https://cdn.example.com/"));
    }

    @Test
    void 업로드하면_영구_URL을_돌려준다() {
        final String imageUrl = adapter.uploadAndReturnUrl(KEY, uploadCommand());

        assertThat(imageUrl).isEqualTo("https://cdn.example.com/images/abc.jpg");
    }

    @Test
    void 업로드_요청에_bucket_key_형식_크기를_담는다() {
        adapter.uploadAndReturnUrl(KEY, uploadCommand());

        final ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        final PutObjectRequest request = captor.getValue();
        assertThat(request.bucket()).isEqualTo("marketgo-images");
        assertThat(request.key()).isEqualTo("images/abc.jpg");
        assertThat(request.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void 업로드가_실패하면_내부_정보_없이_저장소_사용_불가를_반환한다() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("bucket marketgo-images denied"));

        assertThatThrownBy(() -> adapter.uploadAndReturnUrl(KEY, uploadCommand()))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.IMAGE_STORAGE_UNAVAILABLE);
    }

    // 503 응답 메시지에 bucket 이름이나 SDK 메시지가 새면 저장소 구성이 드러난다.
    @Test
    void 저장소_실패_메시지에_내부_정보를_담지_않는다() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("bucket marketgo-images denied"));

        assertThatThrownBy(() -> adapter.uploadAndReturnUrl(KEY, uploadCommand()))
                .isInstanceOf(ApiException.class)
                .hasMessage(ErrorType.IMAGE_STORAGE_UNAVAILABLE.description())
                .hasMessageNotContaining("marketgo-images");
    }

    @Test
    void 우리_저장소의_URL은_그대로_돌려준다() {
        assertThat(adapter.requireOwnedUrl("https://cdn.example.com/images/abc.jpg"))
                .isEqualTo("https://cdn.example.com/images/abc.jpg");
    }

    // 임의 URL 을 통과시키면 사용자가 우리 비용으로 아무 호스트나 가져오게 만들 수 있다.
    @Test
    void 우리_저장소의_URL이_아니면_거부한다() {
        assertThatThrownBy(() -> adapter.requireOwnedUrl("https://evil.example.com/images/abc.jpg"))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_PARAMETER_ERROR);
    }

    // 이전에는 IllegalArgumentException 이 그대로 올라가 클라이언트가 500 을 받았다.
    @Test
    void 접두사_규칙을_벗어난_key는_거부한다() {
        assertThatThrownBy(() -> adapter.requireOwnedUrl("https://cdn.example.com/uploads/abc.jpg"))
                .isInstanceOf(ImageValidationException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_PARAMETER_ERROR);
    }

    @Test
    void base_URL만_주어져_key가_비면_거부한다() {
        assertThatThrownBy(() -> adapter.requireOwnedUrl("https://cdn.example.com/"))
                .isInstanceOf(ImageValidationException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INVALID_PARAMETER_ERROR);
    }

    private UploadImageCommand uploadCommand() {
        return new UploadImageCommand(
                ImageContentType.JPEG, new ImageSize(3L), new byte[] {1, 2, 3});
    }
}
