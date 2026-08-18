package com.example.demo.item.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @Column(name = "item_name", nullable = false, length = 50)
    private String name;

    @Column(name = "default_unit", length = 20)
    private String defaultUnit;

    @Column(name = "item_image_url", length = 255)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_code", length = 32)
    private ItemCategory category;

    public Item(final String name, final String defaultUnit) {
        this(name, defaultUnit, null);
    }

    public Item(final String name, final String defaultUnit, final String imageUrl) {
        this(name, defaultUnit, imageUrl, null);
    }

    public Item(
            final String name,
            final String defaultUnit,
            final String imageUrl,
            final ItemCategory category) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("item name must not be blank");
        }
        this.name = name;
        this.defaultUnit = defaultUnit;
        this.imageUrl = imageUrl;
        this.category = category;
    }
}
