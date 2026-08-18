package com.example.demo.image.presentation.spec;

import com.example.demo.image.presentation.dto.ImageUploadResponse;
import com.example.demo.image.presentation.dto.PresignedUploadRequest;
import com.example.demo.image.presentation.dto.PresignedUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface ImageControllerSpec {

    @Operation(
            summary = "이미지를 서버를 거쳐 업로드한다",
            description = "PNG·JPEG만 허용하고 5MB를 넘을 수 없다. 응답의 imageUrl을 제보의 photoUrl로 사용한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "업로드 성공"),
        @ApiResponse(responseCode = "400", description = "형식이 허용되지 않거나 크기가 초과했다"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "503", description = "이미지 저장소를 사용할 수 없다")
    })
    ResponseEntity<ImageUploadResponse> uploadImage(MultipartFile image);

    @Operation(
            summary = "클라이언트 직접 업로드용 presigned PUT URL을 발급한다",
            description = "uploadUrl로 직접 PUT한다. 요청과 같은 Content-Type·Content-Length를 실어야 한다. "
                    + "만료되는 uploadUrl은 저장하지 않고, 제보에는 imageUrl을 저장한다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "발급 성공"),
        @ApiResponse(responseCode = "400", description = "형식이 허용되지 않거나 크기가 초과했다"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요하다"),
        @ApiResponse(responseCode = "503", description = "이미지 저장소를 사용할 수 없다")
    })
    ResponseEntity<PresignedUploadResponse> issuePresignedUrl(PresignedUploadRequest request);
}
