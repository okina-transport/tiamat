package org.rutebanken.tiamat.dtoassembling.dto;

import org.rutebanken.tiamat.model.job.Job;
import org.rutebanken.tiamat.model.job.JobStatus;
import org.rutebanken.tiamat.model.job.JobType;

import java.time.Instant;

public class JobDto {

    private Long id;
    private Instant started;
    private Instant finished;
    private JobStatus status;
    private JobType type;
    private String username;
    private Integer totalCount;
    private Integer remainingCount;

    public static JobDto from(Job job) {
        JobDto dto = new JobDto();
        dto.setId(job.getId());
        dto.setStarted(job.getStarted());
        dto.setFinished(job.getFinished());
        dto.setStatus(job.getStatus());
        dto.setType(job.getType());
        dto.setUsername(job.getUserName());
        dto.setTotalCount(job.getTotalCount());
        dto.setRemainingCount(job.getRemainingCount());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getRemainingCount() {
        return remainingCount;
    }

    public void setRemainingCount(Integer remainingCount) {
        this.remainingCount = remainingCount;
    }
}
