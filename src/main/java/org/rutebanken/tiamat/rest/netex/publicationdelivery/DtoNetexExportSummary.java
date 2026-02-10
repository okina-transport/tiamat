package org.rutebanken.tiamat.rest.netex.publicationdelivery;

import org.rutebanken.tiamat.exporter.params.ExportParams;
import org.rutebanken.tiamat.importer.ImportParams;
import org.rutebanken.tiamat.model.job.JobAction;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;
import org.rutebanken.tiamat.model.job.Link;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DtoNetexExportSummary {

    Long id;
    String jobUrl;
    String fileName;
    String subFolder;
    String message;
    Instant started;
    Instant finished;
    JobStatus status;
    JobType type;
    JobAction action;
    ExportParams exportParams;
    ImportParams importParams;
    List<Link> links = new ArrayList<Link>();
    String userName;
    Boolean isLugCompleted;
    Set<String> operators = new HashSet<>();

    public ImportParams getImportParams() {
        return importParams;
    }

    public void setImportParams(ImportParams importParams) {
        this.importParams = importParams;
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

    public String getSubFolder() {
        return subFolder;
    }

    public void setSubFolder(String subFolder) {
        this.subFolder = subFolder;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public ExportParams getExportParams() {
        return exportParams;
    }

    public void setExportParams(ExportParams exportParams) {
        this.exportParams = exportParams;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
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

    public Set<String> getOperators() {
        return operators;
    }

    public void setOperators(Set<String> operators) {
        this.operators = operators;
    }
}
