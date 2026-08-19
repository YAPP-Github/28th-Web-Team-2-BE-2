package com.example.demo.report.presentation.spec;

import com.example.demo.report.presentation.dto.ImageAnalysisRequest;
import com.example.demo.report.presentation.dto.ImageAnalysisResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;

public interface UserReportImageAnalysisControllerSpec {

    @Operation(
            summary = "제보 사진에서 입력값 후보를 인식한다",
            description = """
                    업로드된 사진을 분석해 품목·가격·수량 후보를 제안한다. 결과를 저장하지 않으며
                    사용자가 확인·수정한 뒤 POST /api/v1/items/{itemId}/reports 로 제출한다.

                    item.unit은 모델이 아니라 매칭된 품목의 default_unit이다. 저장 API가 이 문자열과의
                    일치를 요구하므로 응답의 unit을 그대로 실어 보내야 한다.

                    price.basis는 사진에 적힌 가격 기준 수량이다("3kg"). price.unitMatched가 false면
                    그 가격은 item.unit 기준값이 아니므로, 사용자가 단위 기준 가격을 확정해야 한다.
                    그대로 저장하면 공시가 대비 차이가 왜곡된다.

                    인식하지 못한 값은 null이다. 후보가 여럿이면 item이 null이고 candidates가 채워진다.
                    confidence는 참고용이며 자동 확정 근거로 쓰지 않는다.

                    이 응답에 없는 저장 API 필수값이 있다 — regionId, reportType, amount는 사진에서
                    나올 값이 아니므로 클라이언트가 채워야 한다.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "인식 성공. 일부 값이 null일 수 있다"),
        @ApiResponse(responseCode = "400", description = "imageUrl이 우리 저장소의 URL이 아니다"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "404", description = "지정한 itemId의 품목이 없다"),
        @ApiResponse(responseCode = "503", description = "인식 모델 쿼터를 소진했다. 잠시 후 재시도"),
        @ApiResponse(responseCode = "502", description = "인식 모델 오류(재시도 가능) 또는 응답 해석 실패(재시도 무의미). code 로 구분한다"),
        @ApiResponse(responseCode = "504", description = "인식이 제한 시간 안에 끝나지 않았다")
    })
    ResponseEntity<ImageAnalysisResponse> analyzeImage(ImageAnalysisRequest request);
}
