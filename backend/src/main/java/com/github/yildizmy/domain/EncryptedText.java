package com.github.yildizmy.domain;

import com.github.yildizmy.security.FieldCipher;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Put on a column with {@code @Convert(converter = EncryptedText.class)}. See {@link FieldCipher}. */
@Converter
public class EncryptedText implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return FieldCipher.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return FieldCipher.decrypt(dbData);
    }
}
