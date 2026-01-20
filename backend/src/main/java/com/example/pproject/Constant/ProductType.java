package com.example.pproject.Constant;

public enum ProductType {
    ONE_TIME("단건 결제"),
    SUBSCRIPTION("정기 구독");

    private final String description;

    ProductType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
