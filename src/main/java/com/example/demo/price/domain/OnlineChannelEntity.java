package com.example.demo.price.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "online_channels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OnlineChannelEntity {

    @Id
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private ChannelCode code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private boolean active;

    public OnlineChannelEntity(
            final Integer id, final ChannelCode code, final String name, final boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.active = active;
    }
}
