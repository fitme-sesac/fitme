package com.example.pproject.Constant;

public enum ProductType {
    ONE_TIME("단건 결제"),
    SUBSCRIPTION("정기 구독");

    private final String description;

    /**
     * Creates a ProductType enum constant with the specified description.
     *
     * @param description the human-readable description for this product type (e.g. "단건 결제", "정기 구독")
     */
    ProductType(String description) {
        this.description = description;
    }

    /**
     * Gets the human-readable description associated with this product type.
     *
     * @return the description string for this enum constant
     */
    public String getDescription() {
        return description;
    }
}