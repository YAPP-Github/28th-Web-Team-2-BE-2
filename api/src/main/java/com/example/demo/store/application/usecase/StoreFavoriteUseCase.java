package com.example.demo.store.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.store.application.command.StoreFavoriteCommand;
import com.example.demo.store.application.port.StoreFavoriteCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreFavoriteUseCase {

    private final StoreFavoriteCommandPort storeFavoriteCommandPort;

    @Transactional
    public void add(final StoreFavoriteCommand command) {
        validateStoreExists(command.storeId());
        storeFavoriteCommandPort.add(command.userId(), command.storeId());
    }

    private void validateStoreExists(final Long storeId) {
        if (storeFavoriteCommandPort.storeExists(storeId)) {
            return;
        }
        throw new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND);
    }
}
