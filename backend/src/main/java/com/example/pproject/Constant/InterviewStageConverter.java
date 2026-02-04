package com.example.pproject.Constant;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * InterviewStage enum과 DB 값(1ST, 2ND, FINAL) 간의 변환을 담당하는 Converter
 */
@Converter(autoApply = true)
public class InterviewStageConverter implements AttributeConverter<InterviewStage, String> {

    @Override
    public String convertToDatabaseColumn(InterviewStage attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode(); // FIRST -> "1ST", SECOND -> "2ND", FINAL -> "FINAL"
    }

    @Override
    public InterviewStage convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        
        for (InterviewStage stage : InterviewStage.values()) {
            if (stage.getCode().equals(dbData)) {
                return stage;
            }
        }
        
        throw new IllegalArgumentException("Unknown InterviewStage code: " + dbData);
    }
}
