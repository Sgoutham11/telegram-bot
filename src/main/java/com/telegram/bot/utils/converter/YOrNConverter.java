package com.telegram.bot.utils.converter;

import com.telegram.bot.utils.YOrN;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class YOrNConverter implements AttributeConverter<YOrN, String> {


    @Override
    public String convertToDatabaseColumn(YOrN status) {

        if (null == status) {

            return null;
        }
        return status.toString();
    }

    @Override
    public YOrN convertToEntityAttribute(String value) {

        if (StringUtils.isBlank(value)) {
            return null;
        }
        return YOrN.fromValue(value);
    }
}
