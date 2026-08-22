package com.example.demo.image.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.auth.application.port.TokenProvider;
import com.example.demo.auth.application.result.AccessTokenPayload;
import com.example.demo.auth.domain.UserRole;
import com.example.demo.common.config.security.SecurityConfig;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.security.JwtAccessDeniedHandler;
import com.example.demo.common.security.JwtAuthenticationEntryPoint;
import com.example.demo.common.security.SecurityErrorResponseWriter;
import com.example.demo.image.application.command.UploadImageCommand;
import com.example.demo.image.application.result.UploadedImageResult;
import com.example.demo.image.application.usecase.UploadImageUseCase;
import com.example.demo.image.presentation.converter.ImageCommandConverter;
import com.example.demo.image.presentation.converter.ImageResultConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImageController.class)
@Import({
    SecurityConfig.class,
    SecurityErrorResponseWriter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class,
    ImageCommandConverter.class,
    ImageResultConverter.class,
    ImageControllerTest.MockBeans.class
})
class ImageControllerTest {

    private static final String UPLOAD_PATH = "/api/v1/images";
    private static final String IMAGE_URL = "https://cdn.example.com/images/abc.jpg";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private UploadImageUseCase uploadImageUseCase;

    @BeforeEach
    void setUp() {
        // clearInvocations로는 부족하다. 저장소 실패 테스트가 남긴 thenThrow 스텁이 살아 있으면
        // 다음 테스트의 when(...) 안에서 mock이 호출되며 그 자리에서 예외가 터진다.
        reset(uploadImageUseCase);
        when(tokenProvider.parseAccessTokenPayload("access-token"))
                .thenReturn(new AccessTokenPayload(1L, UserRole.USER));
        when(uploadImageUseCase.execute(any())).thenReturn(new UploadedImageResult(IMAGE_URL));
    }

    @Test
    void 인증한_사용자가_이미지를_올리면_created와_영구_URL을_응답한다() throws Exception {
        mockMvc.perform(multipart(UPLOAD_PATH)
                        .file(jpegPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.imageUrl").value(IMAGE_URL));
    }

    @Test
    void 확장자_제한없이_대문자_확장자를_소문자로_변환한다() throws Exception {
        mockMvc.perform(multipart(UPLOAD_PATH)
                        .file(new MockMultipartFile(
                                "image", "receipt.WEBP", "image/jpeg",
                                new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1}))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isCreated());

        final ArgumentCaptor<UploadImageCommand> captor = ArgumentCaptor.forClass(UploadImageCommand.class);
        verify(uploadImageUseCase).execute(captor.capture());
        assertThat(captor.getValue().extension()).isEqualTo("webp");
    }

    @Test
    void 로그인하지_않으면_업로드를_거부한다() throws Exception {
        mockMvc.perform(multipart(UPLOAD_PATH).file(jpegPart()))
                .andExpect(status().isUnauthorized());

        verify(uploadImageUseCase, never()).execute(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/gif", "application/pdf"})
    void 허용하지_않는_형식은_저장소에_닿기_전에_거부한다(final String contentType) throws Exception {
        mockMvc.perform(multipart(UPLOAD_PATH)
                        .file(new MockMultipartFile("image", "a.gif", contentType, new byte[] {1}))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_IMAGE_FORMAT.name()));

        verify(uploadImageUseCase, never()).execute(any());
    }

    // 확장자·MIME 을 위조해도 바이트가 다르면 거부한다.
    @Test
    void 형식을_위조한_내용은_거부한다() throws Exception {
        mockMvc.perform(multipart(UPLOAD_PATH)
                        .file(new MockMultipartFile(
                                "image", "evil.png", "image/png", new byte[] {0x50, 0x4B, 0x03, 0x04}))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_IMAGE_FORMAT.name()));

        verify(uploadImageUseCase, never()).execute(any());
    }

    @Test
    void image_part가_없으면_공통_envelope으로_400을_응답한다() throws Exception {
        mockMvc.perform(multipart(UPLOAD_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_PARAMETER_ERROR.name()));
    }

    @Test
    void 빈_파일은_거부한다() throws Exception {
        mockMvc.perform(multipart(UPLOAD_PATH)
                        .file(new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[0]))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorType.INVALID_IMAGE_FORMAT.name()));
    }

    @Test
    void 상한을_넘는_파일은_IMAGE_TOO_LARGE로_거부한다() throws Exception {
        final byte[] tooLarge = new byte[5 * 1024 * 1024 + 1];
        tooLarge[0] = (byte) 0xFF;
        tooLarge[1] = (byte) 0xD8;
        tooLarge[2] = (byte) 0xFF;

        mockMvc.perform(multipart(UPLOAD_PATH)
                        .file(new MockMultipartFile("image", "big.jpg", "image/jpeg", tooLarge))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorType.IMAGE_TOO_LARGE.name()));
    }

    @Test
    void 저장소를_쓸_수_없으면_503을_응답한다() throws Exception {
        when(uploadImageUseCase.execute(any())).thenThrow(new ApiException(
                ErrorType.IMAGE_STORAGE_UNAVAILABLE.description(),
                ErrorType.IMAGE_STORAGE_UNAVAILABLE,
                HttpStatus.SERVICE_UNAVAILABLE));

        mockMvc.perform(multipart(UPLOAD_PATH)
                        .file(jpegPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(ErrorType.IMAGE_STORAGE_UNAVAILABLE.name()));
    }

    /** 선두 바이트가 JPEG 시그니처여야 통과한다. */
    private MockMultipartFile jpegPart() {
        return new MockMultipartFile(
                "image", "receipt.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1});
    }

    @TestConfiguration
    static class MockBeans {

        @Bean
        TokenProvider tokenProvider() {
            return mock(TokenProvider.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        UploadImageUseCase uploadImageUseCase() {
            return mock(UploadImageUseCase.class);
        }

    }
}
