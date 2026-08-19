package com.example.demo.store.presentation.converter;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.store.application.command.StoreFavoriteCommand;
import org.springframework.stereotype.Component;

@Component
public class StoreCommandConverter {

    public StoreFavoriteCommand toStoreFavoriteCommand(final Long storeId, final AuthPrincipal principal) {
        return new StoreFavoriteCommand(principal.userId(), storeId);
    }
}
