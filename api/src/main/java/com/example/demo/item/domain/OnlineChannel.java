package com.example.demo.item.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "online_channels")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class OnlineChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_id")
    private Integer id;

    @Column(name = "channel_name", nullable = false, length = 50)
    private String name;

    /**
     * 채널 성격(새벽배송·오픈마켓 등). 배송 조건이 다른 가격을 같은 상품처럼 오해하지 않게 금액과 함께 노출한다.
     *
     * <p>운영 스키마는 {@code NOT NULL}이다(V20). 엔티티에서 null 을 허용하는 것은 채널을 만드는 기존 테스트들이
     * 이 값을 모르기 때문이며, 테스트가 Flyway 스키마를 쓰지 않아 제약이 재현되지 않는 현 구조의 결과다.
     */
    @Column(name = "channel_kind", length = 20)
    private String kind;

    public OnlineChannel(final String name) {
        this(name, null);
    }

    public OnlineChannel(final String name, final String kind) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("online channel name must not be blank");
        }
        this.name = name;
        this.kind = kind;
    }
}
