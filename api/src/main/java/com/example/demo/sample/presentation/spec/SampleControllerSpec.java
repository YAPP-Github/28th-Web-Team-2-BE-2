package com.example.demo.sample.presentation.spec;

import com.example.demo.sample.presentation.dto.CreateSampleMessageRequest;
import com.example.demo.sample.presentation.dto.SampleMessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Sample", description = "샘플 메시지 API")
public interface SampleControllerSpec {

    @Operation(
            summary = "샘플 메시지를 생성한다",
            description = "메시지 내용을 받아 샘플 메시지를 생성한다.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateSampleMessageRequest.class))))
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "샘플 메시지 생성 성공",
                content = @Content(schema = @Schema(implementation = SampleMessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "message가 비어 있으면 요청이 거부된다")
    })
    ResponseEntity<SampleMessageResponse> createSampleMessage(CreateSampleMessageRequest request);

    @Operation(summary = "샘플 메시지를 조회한다", description = "현재 저장된 샘플 메시지를 조회한다.")
    @ApiResponse(
            responseCode = "200",
            description = "샘플 메시지 조회 성공",
            content = @Content(schema = @Schema(implementation = SampleMessageResponse.class)))
    ResponseEntity<SampleMessageResponse> getSampleMessage();
}
