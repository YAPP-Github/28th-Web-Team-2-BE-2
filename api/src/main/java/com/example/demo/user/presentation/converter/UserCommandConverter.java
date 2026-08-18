package com.example.demo.user.presentation.converter;

import com.example.demo.user.application.command.UpdateUserNicknameCommand;
import com.example.demo.user.presentation.dto.UpdateUserNicknameRequest;
import org.springframework.stereotype.Component;

@Component
public class UserCommandConverter {

    public UpdateUserNicknameCommand toUpdateUserNicknameCommand(
            final Long userId, final UpdateUserNicknameRequest request) {
        return new UpdateUserNicknameCommand(userId, request.nickname());
    }
}
