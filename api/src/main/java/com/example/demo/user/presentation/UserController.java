package com.example.demo.user.presentation;

import com.example.demo.common.presentation.DirectResponse;
import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.user.application.command.UpdateUserNicknameCommand;
import com.example.demo.user.application.result.GetUserMeResult;
import com.example.demo.user.application.usecase.GetUserMeUseCase;
import com.example.demo.user.application.usecase.UpdateUserNicknameUseCase;
import com.example.demo.user.presentation.converter.UserCommandConverter;
import com.example.demo.user.presentation.converter.UserResultConverter;
import com.example.demo.user.presentation.dto.UpdateUserNicknameRequest;
import com.example.demo.user.presentation.dto.UserMeResponse;
import com.example.demo.user.presentation.spec.UserControllerSpec;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserControllerSpec {

    private final UpdateUserNicknameUseCase updateUserNicknameUseCase;
    private final GetUserMeUseCase getUserMeUseCase;
    private final UserCommandConverter userCommandConverter;
    private final UserResultConverter userResultConverter;

    @DirectResponse
    @GetMapping("/me")
    @Override
    public ResponseEntity<UserMeResponse> getMe(
            @AuthenticationPrincipal final AuthPrincipal principal) {
        final GetUserMeResult result = getUserMeUseCase.execute(principal.userId());
        return ResponseEntity.ok(userResultConverter.toUserMeResponse(result));
    }

    @PatchMapping("/me")
    @Override
    public ResponseEntity<Void> updateNickname(
            @Valid @RequestBody final UpdateUserNicknameRequest request,
            @AuthenticationPrincipal final AuthPrincipal principal) {
        final UpdateUserNicknameCommand command =
                userCommandConverter.toUpdateUserNicknameCommand(principal.userId(), request);
        updateUserNicknameUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }
}
