package org.rutebanken.tiamat.model.job;

import com.google.common.base.MoreObjects;

import java.util.List;

public class AnalyzeImportError {

    private AnalyzeImportErrorType type;
    private String message;
    private Integer line;
    private String field;
    private String errorValue;
    private List<String> allowedValues;

    public AnalyzeImportError(AnalyzeImportErrorType type, String message, Integer line, String field) {
        this(type, message, line, field, null);
    }

    public AnalyzeImportError(AnalyzeImportErrorType type, String message, Integer line, String field, String errorValue) {
        this(type, message, line, field, errorValue, null);
    }

    public AnalyzeImportError(AnalyzeImportErrorType type, String message, Integer line, String field, String errorValue, List<String> allowedValues) {
        this.type = type;
        this.message = message;
        this.line = line;
        this.field = field;
        this.errorValue = errorValue;
        this.allowedValues = allowedValues;
    }

    public static Builder builder() {
        return new Builder();
    }

    public AnalyzeImportErrorType getType() {
        return type;
    }

    public void setType(AnalyzeImportErrorType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getErrorValue() {
        return errorValue;
    }

    public void setErrorValue(String errorValue) {
        this.errorValue = errorValue;
    }

    public List<String> getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(List<String> allowedValues) {
        this.allowedValues = allowedValues;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("type", type)
                .add("message", message)
                .add("line", line)
                .add("field", field)
                .add("errorValue", errorValue)
                .add("allowedValues", allowedValues)
                .toString();
    }

    public static class Builder {
        private AnalyzeImportErrorType type;
        private String message;
        private Integer line;
        private String field;
        private String errorValue;
        private List<String> allowedValues;

        public Builder type(AnalyzeImportErrorType type) {
            this.type = type;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder line(Integer line) {
            this.line = line;
            return this;
        }

        public Builder field(String field) {
            this.field = field;
            return this;
        }

        public Builder errorValue(String errorValue) {
            this.errorValue = errorValue;
            return this;
        }

        public Builder allowedValues(List<String> allowedValues) {
            this.allowedValues = allowedValues;
            return this;
        }

        public AnalyzeImportError build() {
            return new AnalyzeImportError(type, message, line, field, errorValue, allowedValues);
        }
    }
}
