package com.example.demo.item.application.usecase;

import com.example.demo.common.exception.ApiException;
import com.example.demo.common.exception.ErrorType;
import com.example.demo.item.application.port.ItemFavoriteCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemFavoriteUseCase {

    private final ItemFavoriteCommandPort itemFavoriteCommandPort;

    @Transactional
    public void add(final Long userId, final Long itemId) {
        validateItemExists(itemId);
        itemFavoriteCommandPort.add(userId, itemId);
    }

    @Transactional
    public void delete(final Long userId, final Long itemId) {
        validateItemExists(itemId);
        itemFavoriteCommandPort.delete(userId, itemId);
    }

    private void validateItemExists(final Long itemId) {
        if (itemFavoriteCommandPort.itemExists(itemId)) {
            return;
        }
        throw new ApiException(
                ErrorType.NO_RESOURCE_ERROR.description(),
                ErrorType.NO_RESOURCE_ERROR,
                HttpStatus.NOT_FOUND);
    }
}
