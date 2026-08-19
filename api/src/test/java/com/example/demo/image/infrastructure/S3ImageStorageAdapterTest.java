package com.example.demo.image.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
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

    private UploadImageCommand uploadCommand() {
        return new UploadImageCommand(
                ImageContentType.JPEG, new ImageSize(3L), new byte[] {1, 2, 3});
    }
}
