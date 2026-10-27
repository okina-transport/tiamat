package org.rutebanken.tiamat.model.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the job errors as a JSON array : [{"code": "...", "severity": "ERROR", "params": {"name": "value"}}]
 */
@Converter
public class JobErrorListConverter implements AttributeConverter<List<JobError>, String> {

    private static final Logger logger = LoggerFactory.getLogger(JobErrorListConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<JobError> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (JsonProcessingException e) {
            logger.error("Could not serialize job errors to JSON", e);
            return null;
        }
    }

    @Override
    public List<JobError> convertToEntityAttribute(String dbData) {
        List<JobError> errors = new ArrayList<>();
        if (dbData == null || dbData.isBlank()) {
            return errors;
        }
        try {
            for (JsonNode node : objectMapper.readTree(dbData)) {
                if (node.isTextual()) {
                    // Legacy format : array of error codes only, e.g. ["TEMPLATE","MISSING_DATA"]
                    errors.add(JobError.error(AnalyzeImportErrorType.valueOf(node.asText())));
                } else {
                    errors.add(objectMapper.treeToValue(node, JobError.class));
                }
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            logger.error("Could not deserialize job errors from JSON: {}", dbData, e);
            return new ArrayList<>();
        }
        return errors;
    }
}
