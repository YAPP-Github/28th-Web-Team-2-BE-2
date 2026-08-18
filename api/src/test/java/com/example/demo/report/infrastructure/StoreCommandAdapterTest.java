package com.example.demo.report.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.report.application.command.StoreSnapshot;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StoreCommandAdapterTest {

    @Test
    void Kakao_장소_upsert는_저장된_store_id를_반환한다() {
        final EntityManager entityManager = mock(EntityManager.class);
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(7L);

        assertThat(new StoreCommandAdapter(entityManager).save(new StoreSnapshot(
                "166", "장보고", null, null, "서울", null, null, null, null,
                new BigDecimal("127"), new BigDecimal("37"), null))).isEqualTo(7L);
    }
}
