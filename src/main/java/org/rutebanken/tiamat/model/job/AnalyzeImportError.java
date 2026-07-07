package org.rutebanken.tiamat.model.job;

import com.google.common.base.MoreObjects;

public class AnalyzeImportError {

    private AnalyzeImportErrorType type;
    private String message;
    private Integer line;
    private String field;

    public AnalyzeImportError(AnalyzeImportErrorType type, String message, Integer line, String field) {
        this.type = type;
        this.message = message;
        this.line = line;
        this.field = field;
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("type", type)
                .add("message", message)
                .add("line", line)
                .add("field", field)
                .toString();
    }

    public static class Builder {
        private AnalyzeImportErrorType type;
        private String message;
        private Integer line;
        private String field;

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

        public AnalyzeImportError build() {
            return new AnalyzeImportError(type, message, line, field);
        }
    }
}
