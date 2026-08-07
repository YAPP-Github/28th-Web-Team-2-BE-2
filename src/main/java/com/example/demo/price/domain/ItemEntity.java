package com.example.demo.price.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false)
    private Integer itemCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_unit", nullable = false, length = 20)
    private PriceUnit targetUnit;

    @Column(nullable = false)
    private boolean active;

    public ItemEntity(
            final Integer itemCode,
            final String name,
            final PriceUnit targetUnit,
            final boolean active) {
        if (itemCode == null || itemCode <= 0) {
            throw new IllegalArgumentException("item code must be positive");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("item name must not be blank");
        }
        if (targetUnit == null) {
            throw new IllegalArgumentException("target unit must not be null");
        }
        this.itemCode = itemCode;
        this.name = name;
        this.targetUnit = targetUnit;
        this.active = active;
    }
}
