package com.example.demo.report.presentation;

import com.example.demo.report.application.result.ImageAnalysisResult;
import com.example.demo.report.application.usecase.AnalyzeReportImageUseCase;
import com.example.demo.report.presentation.converter.UserReportCommandConverter;
import com.example.demo.report.presentation.converter.UserReportResultConverter;
import com.example.demo.report.presentation.dto.ImageAnalysisRequest;
import com.example.demo.report.presentation.dto.ImageAnalysisResponse;
import com.example.demo.report.presentation.spec.UserReportImageAnalysisControllerSpec;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 제보 사진 인식.
 *
 * <p>경로를 {@code /api/v1/items/{itemId}/reports} 하위에 두지 않는다. {@code itemId}가 선택값이고,
 * 품목을 알아내는 것이 이 API의 목적 중 하나라서 품목을 경로에 요구할 수 없다.
 *
 * <p>{@code POST}이지만 아무것도 저장하지 않는다. 요청 본문이 필요하고 캐시되면 안 되는 조회라
 * {@code POST}를 쓴다.
 */
@RestController
@RequestMapping("/api/v1/user-reports")
@RequiredArgsConstructor
public class UserReportImageAnalysisController implements UserReportImageAnalysisControllerSpec {

    private final AnalyzeReportImageUseCase analyzeReportImageUseCase;
    private final UserReportCommandConverter commandConverter;
    private final UserReportResultConverter resultConverter;

    @PostMapping("/image-analysis")
    @Override
    public ResponseEntity<ImageAnalysisResponse> analyzeImage(
            @Valid @RequestBody final ImageAnalysisRequest request) {
        final ImageAnalysisResult result = analyzeReportImageUseCase.execute(
                commandConverter.toAnalyzeReportImageCommand(request));
        return ResponseEntity.ok(resultConverter.toImageAnalysisResponse(result));
    }
}
