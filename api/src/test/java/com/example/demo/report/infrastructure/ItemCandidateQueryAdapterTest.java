package com.example.demo.report.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.item.domain.Item;
import com.example.demo.item.domain.ItemCategory;
import com.example.demo.report.application.contract.ItemCandidate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** 이 기능이 품목을 맞추느냐를 결정하는 유일한 코드인데 테스트가 없었다. */
class ItemCandidateQueryAdapterTest {

    private ItemNameJpaRepository repository;
    private ItemCandidateQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(ItemNameJpaRepository.class);
        adapter = new ItemCandidateQueryAdapter(repository);
    }

    @Test
    void 이름이_정확히_맞으면_단위까지_함께_돌려준다() {
        when(repository.findByName("오이")).thenReturn(Optional.of(item("오이", "1개")));

        final Optional<ItemCandidate> found = adapter.findByName("오이");

        assertThat(found).get().extracting(ItemCandidate::name, ItemCandidate::defaultUnit)
                .containsExactly("오이", "1개");
    }

    // 모델이 "오 이"처럼 공백을 끼워 오는 경우가 있다. DB 이름에는 공백이 없다.
    @Test
    void 모델_응답의_공백은_제거하고_조회한다() {
        when(repository.findByName("오이")).thenReturn(Optional.of(item("오이", "1개")));

        assertThat(adapter.findByName("오 이")).isPresent();
    }

    @Test
    void 이름이_맞지_않으면_비어_있다() {
        when(repository.findByName("알수없는채소")).thenReturn(Optional.empty());

        assertThat(adapter.findByName("알수없는채소")).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void 이름이_없으면_조회하지_않는다(final String itemName) {
        assertThat(adapter.findByName(itemName)).isEmpty();

        verify(repository, never()).findByName(org.mockito.ArgumentMatchers.any());
    }

    private Item item(final String name, final String defaultUnit) {
        return new Item(name, defaultUnit, null, ItemCategory.ROOT_VEGETABLES);
    }
}
