package com.example.pproject.user.entity.convert;

import com.example.pproject.Constant.RoleType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * RoleType <-> DB 문자열 변환기.
 *
 * - DB에는 enum.name() (대문자)를 저장한다.
 * - 과거 데이터(user/admin/master 등) 또는 대소문자 혼용을 읽을 때는
 *   RoleType.from(..)를 통해 안전하게 복구한다.
 */
@Converter(autoApply = false)
public class RoleTypeConverter implements AttributeConverter<RoleType, String> {

    @Override
    public String convertToDatabaseColumn(RoleType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public RoleType convertToEntityAttribute(String dbData) {
        return RoleType.from(dbData);
    }
}
