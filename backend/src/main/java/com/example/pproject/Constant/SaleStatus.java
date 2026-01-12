package com.example.pproject.Constant;

public enum SaleStatus {
    ON_SALE("판매 중"),
    PAUSED("일시 중지"),
    STOPPED("판매 종료");

    private final String description;

    SaleStatus(String description) {
        this.description = description;
    }
}
