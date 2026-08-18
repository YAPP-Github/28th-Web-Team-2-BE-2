package com.example.demo.item.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.demo.common.exception.AuthenticationRequiredException;
import com.example.demo.item.application.port.ItemQueryPort;
import com.example.demo.item.application.port.PublicPriceQueryPort;
import com.example.demo.item.application.query.ItemQuery;
import com.example.demo.item.application.query.ItemSort;
import org.junit.jupiter.api.Test;

class GetItemQueryUseCaseTest {

    private final ItemQueryPort itemQueryPort = mock(ItemQueryPort.class);
    private final PublicPriceQueryPort publicPriceQueryPort = mock(PublicPriceQueryPort.class);
    private final GetItemQueryUseCase useCase =
            new GetItemQueryUseCase(itemQueryPort, publicPriceQueryPort);

    @Test
    void favoriteOnly가_활성화되면_userId가_필수다() {
        final ItemQuery query =
                new ItemQuery("1121510100", 0, 20, ItemSort.NAME_ASC, null, true);

        assertThatThrownBy(() -> useCase.execute(query, null))
                .isInstanceOf(AuthenticationRequiredException.class);
        verifyNoInteractions(itemQueryPort, publicPriceQueryPort);
    }
}
