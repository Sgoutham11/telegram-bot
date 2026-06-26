package com.telegram.bot.utils.converter;

import com.telegram.bot.utils.ApprovalStatus;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ApprovalStatusConverter implements AttributeConverter<ApprovalStatus, String> {


    @Override
    public String convertToDatabaseColumn(ApprovalStatus status) {

        if (null == status) {

            return null;
        }
        return status.getValue();
    }

    @Override
    public ApprovalStatus convertToEntityAttribute(String value) {

        if (StringUtils.isBlank(value)) {
            return null;
        }
        return ApprovalStatus.fromValue(value);
    }
}
