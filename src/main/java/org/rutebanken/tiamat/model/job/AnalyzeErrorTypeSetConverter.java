package org.rutebanken.tiamat.model.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

@Converter
public class AnalyzeErrorTypeSetConverter implements AttributeConverter<Set<AnalyzeImportErrorType>, String> {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeErrorTypeSetConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Set<AnalyzeImportErrorType> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (JsonProcessingException e) {
            logger.error("Could not serialize CsvErrorType set to JSON", e);
            return null;
        }
    }

    @Override
    public Set<AnalyzeImportErrorType> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new LinkedHashSet<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<LinkedHashSet<AnalyzeImportErrorType>>() {
            });
        } catch (JsonProcessingException e) {
            logger.error("Could not deserialize CsvErrorType set from JSON: {}", dbData, e);
            return new LinkedHashSet<>();
        }
    }
}