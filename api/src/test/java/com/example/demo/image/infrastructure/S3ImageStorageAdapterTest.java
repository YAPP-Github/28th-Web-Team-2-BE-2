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
import com.example.demo.image.application.result.PresignedUploadResult;
import com.example.demo.image.domain.ImageContentType;
import com.example.demo.image.domain.ImageKey;
import com.example.demo.image.domain.ImageSize;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

class S3ImageStorageAdapterTest {

    private static final ImageKey KEY = new ImageKey("images/abc.jpg");

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private S3ImageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        s3Presigner = mock(S3Presigner.class);
        adapter = new S3ImageStorageAdapter(
                s3Client,
                s3Presigner,
                new S3ImageStorageProperties(
                        "marketgo-images", "https://cdn.example.com/", Duration.ofMinutes(10)));
    }

    @Test
    void 업로드하면_영구_URL을_돌려준다() {
        final String imageUrl = adapter.upload(KEY, uploadCommand());

        assertThat(imageUrl).isEqualTo("https://cdn.example.com/images/abc.jpg");
    }

    @Test
    void 업로드_요청에_bucket_key_형식_크기를_담는다() {
        adapter.upload(KEY, uploadCommand());

        final ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        final PutObjectRequest request = captor.getValue();
        assertThat(request.bucket()).isEqualTo("marketgo-images");
        assertThat(request.key()).isEqualTo("images/abc.jpg");
        assertThat(request.contentType()).isEqualTo("image/jpeg");
        assertThat(request.contentLength()).isEqualTo(3L);
    }

    @Test
    void 업로드가_실패하면_내부_정보_없이_저장소_사용_불가를_반환한다() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("bucket marketgo-images denied"));

        assertThatThrownBy(() -> adapter.upload(KEY, uploadCommand()))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.IMAGE_STORAGE_UNAVAILABLE);
    }

    // 503 응답 메시지에 bucket 이름이나 SDK 메시지가 새면 저장소 구성이 드러난다.
    @Test
    void 저장소_실패_메시지에_내부_정보를_담지_않는다() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("bucket marketgo-images denied"));

        assertThatThrownBy(() -> adapter.upload(KEY, uploadCommand()))
                .isInstanceOf(ApiException.class)
                .hasMessage(ErrorType.IMAGE_STORAGE_UNAVAILABLE.description())
                .hasMessageNotContaining("marketgo-images");
    }

    @Test
    void presigned_URL과_영구_URL을_함께_발급한다() {
        givenPresignedUrl("https://s3.example.com/images/abc.jpg?X-Amz-Signature=abc");

        final PresignedUploadResult result =
                adapter.presign(KEY, ImageContentType.JPEG, new ImageSize(3L));

        assertThat(result.uploadUrl()).contains("X-Amz-Signature");
        assertThat(result.imageUrl()).isEqualTo("https://cdn.example.com/images/abc.jpg");
        assertThat(result.method()).isEqualTo("PUT");
        assertThat(result.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void presigned_만료는_설정한_기간_뒤로_잡는다() {
        givenPresignedUrl("https://s3.example.com/images/abc.jpg?X-Amz-Signature=abc");

        final Instant before = Instant.now();
        final PresignedUploadResult result =
                adapter.presign(KEY, ImageContentType.JPEG, new ImageSize(3L));

        assertThat(result.expiresAt()).isBetween(before.plus(Duration.ofMinutes(10)),
                Instant.now().plus(Duration.ofMinutes(10)));
    }

    // Content-Type과 Content-Length를 서명에 넣지 않으면 클라이언트가 신고보다 큰 파일을 올릴 수 있다.
    @Test
    void presigned_서명에_형식과_크기를_묶는다() {
        givenPresignedUrl("https://s3.example.com/images/abc.jpg?X-Amz-Signature=abc");

        adapter.presign(KEY, ImageContentType.PNG, new ImageSize(2048L));

        final ArgumentCaptor<PutObjectPresignRequest> captor =
                ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());
        final PutObjectRequest signed = captor.getValue().putObjectRequest();
        assertThat(signed.contentType()).isEqualTo("image/png");
        assertThat(signed.contentLength()).isEqualTo(2048L);
        assertThat(captor.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void presigned_발급이_실패하면_저장소_사용_불가를_반환한다() {
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenThrow(SdkClientException.create("presign failed"));

        assertThatThrownBy(() -> adapter.presign(KEY, ImageContentType.JPEG, new ImageSize(3L)))
                .isInstanceOf(ApiException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.IMAGE_STORAGE_UNAVAILABLE);
    }

    private void givenPresignedUrl(final String url) {
        final PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(toUrl(url));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);
    }

    private URL toUrl(final String url) {
        try {
            return java.net.URI.create(url).toURL();
        } catch (final java.net.MalformedURLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private UploadImageCommand uploadCommand() {
        return new UploadImageCommand(
                ImageContentType.JPEG, new ImageSize(3L), new byte[] {1, 2, 3});
    }
}
