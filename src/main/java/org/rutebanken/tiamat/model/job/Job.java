/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.rutebanken.tiamat.model.job;

import com.google.common.base.MoreObjects;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.importer.ImportParams;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@XmlRootElement
@Schema(description = "Job model")
public class Job {

    @Id
    @GeneratedValue(generator = "sequence_per_table_generator")
    @Schema(description = "Unique id for the entity")
    private Long id;

    @Deprecated
    @Schema(description = "JobUrl property  is deprecated")
    private String jobUrl;

    @Schema(description = "File name of exported file")
    private String fileName;

    private String subFolder;

    private String message;

    private Instant started;

    private Instant finished;

    @Schema(description = "Job status")
    private JobStatus status;

    @Enumerated(EnumType.STRING)
    private JobType type;

    @Enumerated(EnumType.STRING)
    private JobAction action;

    @Transient
    private ExportParams exportParams;

    @Transient
    private ImportParams importParams;

    @Transient
    private List<Link> links = new ArrayList<>();

    private String userName;

    private Boolean isLugCompleted;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "job_operators",
            joinColumns = @JoinColumn(name = "job_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "operator"})
    )
    @Column(name = "operator", nullable = false, length = 100)
    private Set<String> operators = new HashSet<>();

    @Convert(converter = AnalyzeErrorTypeSetConverter.class)
    @Column(columnDefinition = "text")
    private Set<AnalyzeImportErrorType> errors = new LinkedHashSet<>();


    public Job() {
    }

    public Job(JobStatus jobStatus) {
        status = jobStatus;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id", id)
                .add("status", status)
                .add("type", type)
                .add("jobUrl", getJobUrl())
                .add("fileName", fileName)
                .add("subFolder", subFolder)
                .add("started", started)
                .add("finished", finished)
                .add("message", message)
                .add("username", userName)
                .add("errors", errors)
                .toString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Instant getStarted() {
        return started;
    }

    public void setStarted(Instant started) {
        this.started = started;
    }

    public Instant getFinished() {
        return finished;
    }

    public void setFinished(Instant finished) {
        this.finished = finished;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ExportParams getExportParams() {
        return exportParams;
    }

    public void setExportParams(ExportParams exportParams) {
        this.exportParams = exportParams;
    }

    public ImportParams getImportParams() {
        return importParams;
    }

    public void setImportParams(ImportParams importParams) {
        this.importParams = importParams;
    }

    public String getSubFolder() {
        return subFolder;
    }

    public void setSubFolder(String subFolder) {
        this.subFolder = subFolder;
    }

    public List<Link> getLinks() {
        return links;
    }

    public JobType getType() {
        return type;
    }

    public void setType(JobType type) {
        this.type = type;
    }

    public JobAction getAction() {
        return action;
    }

    public void setAction(JobAction action) {
        this.action = action;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Boolean getLugCompleted() {
        return isLugCompleted;
    }

    public void setLugCompleted(Boolean lugCompleted) {
        isLugCompleted = lugCompleted;
    }

    @XmlType
    @XmlEnum(String.class)
    public enum STATUS implements java.io.Serializable {
        PROCESSING,
        FINISHED,
        FAILED;
        public static List<STATUS> getCompletedStatuses() {
            return Arrays.stream(STATUS.values()).filter(status -> status.ordinal() > PROCESSING.ordinal()).collect(Collectors.toList());
        }
    }

    public Set<String> getOperators() {
        return operators;
    }

    public void setOperators(Set<String> operators) {
        this.operators = operators;
    }

    public Set<AnalyzeImportErrorType> getErrors() {
        return errors;
    }

    public void setErrors(Set<AnalyzeImportErrorType> errors) {
        this.errors = errors;
    }
}
