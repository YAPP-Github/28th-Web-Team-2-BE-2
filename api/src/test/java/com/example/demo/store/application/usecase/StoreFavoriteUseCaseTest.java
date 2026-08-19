package com.example.demo.store.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.common.exception.ApiException;
import com.example.demo.store.application.command.StoreFavoriteCommand;
import com.example.demo.store.application.port.StoreFavoriteCommandPort;
import org.junit.jupiter.api.Test;

class StoreFavoriteUseCaseTest {

    private final StoreFavoriteCommandPort port = mock(StoreFavoriteCommandPort.class);
    private final StoreFavoriteUseCase useCase = new StoreFavoriteUseCase(port);

    @Test
    void 존재하는_가게를_단골로_등록한다() {
        when(port.storeExists(1L)).thenReturn(true);

        useCase.add(new StoreFavoriteCommand(7L, 1L));

        verify(port).add(7L, 1L);
    }

    @Test
    void 존재하지_않는_가게는_등록하지_않고_404_오류를_던진다() {
        when(port.storeExists(99L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.add(new StoreFavoriteCommand(7L, 99L)))
                .isInstanceOf(ApiException.class);
    }
}
