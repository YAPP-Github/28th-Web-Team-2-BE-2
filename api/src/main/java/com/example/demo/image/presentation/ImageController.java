package com.example.demo.image.presentation;

import com.example.demo.image.application.result.PresignedUploadResult;
import com.example.demo.image.application.result.UploadedImageResult;
import com.example.demo.image.application.usecase.IssuePresignedUploadUseCase;
import com.example.demo.image.application.usecase.UploadImageUseCase;
import com.example.demo.image.presentation.converter.ImageCommandConverter;
import com.example.demo.image.presentation.converter.ImageResultConverter;
import com.example.demo.image.presentation.dto.ImageUploadResponse;
import com.example.demo.image.presentation.dto.PresignedUploadRequest;
import com.example.demo.image.presentation.dto.PresignedUploadResponse;
import com.example.demo.image.presentation.spec.ImageControllerSpec;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드.
 *
 * <p>경로를 {@code /api/v1} 아래에 둔다. 계약 문서는 {@code /api/images}로 적고 있지만 그러면
 * {@code ResponseWrapper}의 envelope 적용 범위 밖이 되어 같은 서비스에서 응답 모양이 둘로
 * 갈린다. 클라이언트가 엔드포인트마다 파싱을 달리해야 하는 비용이 경로 표기를 맞추는 비용보다
 * 크다고 판단했다.
 */
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController implements ImageControllerSpec {

    private final UploadImageUseCase uploadImageUseCase;
    private final IssuePresignedUploadUseCase issuePresignedUploadUseCase;
    private final ImageCommandConverter commandConverter;
    private final ImageResultConverter resultConverter;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @RequestPart("image") final MultipartFile image) {
        final UploadedImageResult result = uploadImageUseCase.execute(
                commandConverter.toUploadCommand(image));
        return ResponseEntity.status(HttpStatus.CREATED).body(resultConverter.toUploadResponse(result));
    }

    @PostMapping("/presigned-url")
    @Override
    public ResponseEntity<PresignedUploadResponse> issuePresignedUrl(
            @Valid @RequestBody final PresignedUploadRequest request) {
        final PresignedUploadResult result = issuePresignedUploadUseCase.execute(
                commandConverter.toPresignedCommand(request));
        return ResponseEntity.ok(resultConverter.toPresignedResponse(result));
    }
}
