package com.aios.chat.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MessageRoleConverter implements AttributeConverter<MessageRole, String> {

    @Override
    public String convertToDatabaseColumn(MessageRole role) {
        return role == null ? null : role.getValue();
    }

    @Override
    public MessageRole convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MessageRole.fromValue(dbData);
    }
}
