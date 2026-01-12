package com.example.pproject.user.entity.convert;

import com.example.pproject.Constant.SocialType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * SocialType <-> DB 문자열 변환기.
 *
 * - DB에는 enum.name() (NAVER/GOOGLE/KAKAO/OTHER)만 저장한다.
 * - 과거 데이터(google/naver/kakao/nate/Else 등) 또는 대소문자 혼용을 읽을 때는
 *   SocialType.from(..)를 통해 OTHER로 수렴시킨다.
 */
@Converter(autoApply = false)
public class SocialTypeConverter implements AttributeConverter<SocialType, String> {

    @Override
    public String convertToDatabaseColumn(SocialType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public SocialType convertToEntityAttribute(String dbData) {
        return SocialType.from(dbData);
    }
}
