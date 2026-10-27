package org.rutebanken.tiamat.model.job;

import com.google.common.base.MoreObjects;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Error raised by a job, stored as JSON in the job.errors column
 */
public class JobError {

    private AnalyzeImportErrorType code;
    private JobErrorSeverity severity;
    private Map<String, String> params = new LinkedHashMap<>();

    public JobError() {
    }

    public JobError(AnalyzeImportErrorType code, JobErrorSeverity severity) {
        this.code = code;
        this.severity = severity;
    }

    public JobError(AnalyzeImportErrorType code, JobErrorSeverity severity, Map<String, String> params) {
        this.code = code;
        this.severity = severity;
        setParams(params);
    }

    public static JobError error(AnalyzeImportErrorType code) {
        return new JobError(code, JobErrorSeverity.ERROR);
    }

    public static JobError warning(AnalyzeImportErrorType code) {
        return new JobError(code, JobErrorSeverity.WARNING);
    }

    public JobError withParam(String name, String value) {
        params.put(name, value);
        return this;
    }

    public AnalyzeImportErrorType getCode() {
        return code;
    }

    public void setCode(AnalyzeImportErrorType code) {
        this.code = code;
    }

    public JobErrorSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(JobErrorSeverity severity) {
        this.severity = severity;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobError jobError)) return false;
        return code == jobError.code && severity == jobError.severity && Objects.equals(params, jobError.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, severity, params);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("code", code)
                .add("severity", severity)
                .add("params", params)
                .toString();
    }
}
