package com.example.demo.mypage.presentation;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.mypage.application.usecase.GetFavoriteStoresUseCase;
import com.example.demo.mypage.presentation.converter.FavoriteStoreQueryConverter;
import com.example.demo.mypage.presentation.converter.FavoriteStoreResultConverter;
import com.example.demo.mypage.presentation.dto.FavoriteStoresRequest;
import com.example.demo.mypage.presentation.dto.FavoriteStoresResponse;
import com.example.demo.mypage.presentation.spec.MyPageControllerSpec;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class MyPageController implements MyPageControllerSpec {

    private final GetFavoriteStoresUseCase getFavoriteStoresUseCase;
    private final FavoriteStoreQueryConverter queryConverter;
    private final FavoriteStoreResultConverter resultConverter;

    @GetMapping("/favorite-stores")
    @Override
    public ResponseEntity<FavoriteStoresResponse> getFavoriteStores(
            @Valid @ModelAttribute final FavoriteStoresRequest request,
            @AuthenticationPrincipal final AuthPrincipal principal) {
        final FavoriteStoresResponse response = resultConverter.toResponse(
                getFavoriteStoresUseCase.execute(queryConverter.toQuery(request, principal)));
        return ResponseEntity.ok(response);
    }
}
