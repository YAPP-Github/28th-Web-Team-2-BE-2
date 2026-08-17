package com.example.demo.item.presentation;

import com.example.demo.auth.domain.UserRole;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.item.application.result.ItemQueryResult;
import com.example.demo.item.application.usecase.GetItemQueryUseCase;
import com.example.demo.item.application.usecase.ItemFavoriteUseCase;
import com.example.demo.item.presentation.converter.ItemQueryRequestConverter;
import com.example.demo.item.presentation.converter.ItemResultConverter;
import com.example.demo.item.presentation.dto.ItemPageResponse;
import com.example.demo.item.presentation.dto.ItemQueryRequest;
import com.example.demo.item.presentation.spec.ItemControllerSpec;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController implements ItemControllerSpec {

    private final GetItemQueryUseCase getItemQueryUseCase;
    private final ItemFavoriteUseCase itemFavoriteUseCase;
    private final ItemQueryRequestConverter itemQueryRequestConverter;
    private final ItemResultConverter itemResultConverter;

    @GetMapping
    @Override
    public ResponseEntity<ItemPageResponse> getItems(
            @Valid @ModelAttribute final ItemQueryRequest request,
            @AuthenticationPrincipal final AuthPrincipal principal,
            final Authentication authentication) {
        final Long userId = userId(principal, authentication);
        validateFavoriteOnly(request.favoriteOnly(), userId);
        final ItemQueryResult result = getItemQueryUseCase.execute(
                itemQueryRequestConverter.toQuery(request), userId);
        final ItemPageResponse data = itemResultConverter.toResponse(result);
        return ResponseEntity.ok(data);
    }

    @PutMapping("/{itemId}/favorite")
    @Override
    public ResponseEntity<Void> addFavorite(
            @PathVariable final Long itemId,
            @AuthenticationPrincipal final AuthPrincipal principal) {
        itemFavoriteUseCase.add(principal.userId(), itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{itemId}/favorite")
    @Override
    public ResponseEntity<Void> deleteFavorite(
            @PathVariable final Long itemId,
            @AuthenticationPrincipal final AuthPrincipal principal) {
        itemFavoriteUseCase.delete(principal.userId(), itemId);
        return ResponseEntity.noContent().build();
    }

    private Long userId(final AuthPrincipal principal, final Authentication authentication) {
        if (principal == null || authentication == null) {
            return null;
        }
        final boolean isUser = authentication.getAuthorities().stream()
                .anyMatch(authority -> UserRole.USER.authority().equals(authority.getAuthority()));
        return isUser ? principal.userId() : null;
    }

    private void validateFavoriteOnly(final Boolean favoriteOnly, final Long userId) {
        if (!Boolean.TRUE.equals(favoriteOnly) || userId != null) {
            return;
        }
        throw new ApiException(
                ErrorType.UNAUTHORIZED.description(),
                ErrorType.UNAUTHORIZED,
                HttpStatus.UNAUTHORIZED);
    }
}
