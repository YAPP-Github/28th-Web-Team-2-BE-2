package com.example.demo.news.presentation.spec;

import com.example.demo.news.presentation.dto.NewsArticleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "News", description = "농산물 뉴스 API")
public interface NewsControllerSpec {

    @Operation(summary = "농산물 뉴스 목록을 조회한다")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "뉴스 목록 조회 성공",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = NewsArticleResponse.class)))),
        @ApiResponse(responseCode = "503", description = "조회 가능한 뉴스가 없다")
    })
    ResponseEntity<List<NewsArticleResponse>> getNews();
}
