package com.example.demo.user.presentation;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.user.application.usecase.AddUserRegionUseCase;
import com.example.demo.user.application.usecase.SetCurrentUserRegionUseCase;
import com.example.demo.user.presentation.command.UserRegionCommandConverter;
import com.example.demo.user.presentation.dto.AddUserRegionRequest;
import com.example.demo.user.presentation.spec.UserRegionControllerSpec;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/regions")
@RequiredArgsConstructor
public class UserRegionController implements UserRegionControllerSpec {

    private final AddUserRegionUseCase addUserRegionUseCase;
    private final SetCurrentUserRegionUseCase setCurrentUserRegionUseCase;
    private final UserRegionCommandConverter userRegionCommandConverter;

    @PostMapping
    @Override
    public ResponseEntity<Void> addRegion(
            @Valid @RequestBody final AddUserRegionRequest request,
            @AuthenticationPrincipal final AuthPrincipal principal) {
        addUserRegionUseCase.execute(userRegionCommandConverter.toAddCommand(request, principal));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{regionId}/current")
    @Override
    public ResponseEntity<Void> setCurrentRegion(
            @PathVariable final String regionId,
            @AuthenticationPrincipal final AuthPrincipal principal) {
        setCurrentUserRegionUseCase.execute(
                userRegionCommandConverter.toSetCurrentCommand(regionId, principal));
        return ResponseEntity.noContent().build();
    }
}
