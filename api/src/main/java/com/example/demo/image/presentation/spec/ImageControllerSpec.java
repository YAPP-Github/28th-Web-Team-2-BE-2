package com.example.demo.image.presentation.spec;

import com.example.demo.image.presentation.dto.ImageUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface ImageControllerSpec {

    @Operation(
            summary = "이미지를 서버를 거쳐 업로드한다",
            description = "이미지 MIME·확장자 종류 제한 없이 업로드한다. 5MB를 넘을 수 없으며 응답의 imageUrl을 제보의 photoUrl로 사용한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "업로드 성공"),
        @ApiResponse(responseCode = "400", description = "빈 파일이거나 크기가 초과했다"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "503", description = "이미지 저장소를 사용할 수 없다")
    })
    ResponseEntity<ImageUploadResponse> uploadImage(MultipartFile image);
}
