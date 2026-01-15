package com.example.pproject.Constant;

public enum SaleStatus {
    ON_SALE("판매 중"),
    PAUSED("일시 중지"),
    STOPPED("판매 종료");

    private final String description;

    /**
     * Creates a SaleStatus enum constant with the specified description.
     *
     * @param description the display string describing the sale status (for example, "판매 중")
     */
    SaleStatus(String description) {
        this.description = description;
    }

    /**
     * Gets the human-readable description for the sale status.
     *
     * @return the description string associated with this enum constant
     */
    public String getDescription() {
        return description;
    }
}