package com.example.demo.user.application.usecase;

import com.example.demo.auth.application.port.UserRepository;
import com.example.demo.auth.domain.User;
import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.user.application.command.UpdateUserNicknameCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateUserNicknameUseCase {

    private final UserRepository userRepository;

    @Transactional
    public void execute(final UpdateUserNicknameCommand command) {
        final User user = userRepository.findById(command.userId()).orElseThrow(() -> new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND));
        validateNicknameAvailable(user, command.nickname());
        user.changeNickname(command.nickname());
        try {
            userRepository.saveAndFlush(user);
        } catch (final DataIntegrityViolationException exception) {
            throw new ApiException(
                    ErrorType.DUPLICATE_NICKNAME_ERROR.description(),
                    ErrorType.DUPLICATE_NICKNAME_ERROR,
                    HttpStatus.CONFLICT,
                    exception);
        }
    }

    private void validateNicknameAvailable(final User user, final String nickname) {
        userRepository.findByNickname(nickname).ifPresent(existingUser -> {
            if (!existingUser.id().equals(user.id())) {
                throw new ApiException(
                        ErrorType.DUPLICATE_NICKNAME_ERROR.description(),
                        ErrorType.DUPLICATE_NICKNAME_ERROR,
                        HttpStatus.CONFLICT);
            }
        });
    }
}
