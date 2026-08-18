package com.example.demo.item.domain;

public enum ItemCategory {
    ROOT_VEGETABLES("감자·뿌리"),
    LEAFY_GREENS("잎채소"),
    FRUITING_VEGETABLES("열매채소"),
    PEPPERS("고추"),
    SEASONINGS("양념"),
    MUSHROOMS("버섯"),
    FRUITS("과채");

    private final String displayName;

    ItemCategory(final String displayName) {
        this.displayName = displayName;
    }

    public String code() {
        return name();
    }

    public String displayName() {
        return displayName;
    }
}
