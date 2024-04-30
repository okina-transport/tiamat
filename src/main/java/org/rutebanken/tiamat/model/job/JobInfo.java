package org.rutebanken.tiamat.model.job;

import com.amazonaws.services.directory.model.ServiceException;
import io.swagger.models.parameters.AbstractParameter;

import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.*;
import java.util.*;

@XmlRootElement(name = "job_info")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"id", "referential", "action", "type", "created", "started", "updated", "status", "linkInfos",
		                     "actionParameters"})

public class JobInfo {

	@XmlElement(name = "id", required = true)
	private Long id;

	@XmlElement(name = "referential", required = true)
	private String referential;

	@XmlElement(name = "action", required = true)
	private String action;

	@XmlElement(name = "type")
	private String type;

	@XmlElement(name = "created", required = true)
	private Date created;

	@XmlElement(name = "started")
	private Date started;

	@XmlElement(name = "updated")
	private Date updated;

	@XmlElement(name = "status", required = true)
	private STATUS status;

	@XmlElement(name = "links")
	private List<LinkInfo> linkInfos;

	@XmlElementRef(name = "action_parameters")
	private AbstractParameter actionParameters;

	public JobInfo(Job job, UriInfo uriInfo) throws ServiceException {
		this(job, true, uriInfo);
	}

	public JobInfo(Job job, boolean addLink, UriInfo uriInfo) throws ServiceException {
		id = job.getId();
		started = job.getStarted() == null ? null : Date.from(job.getStarted());
		status = STATUS.valueOf(job.getStatus().name());

		if (addLink) {
			linkInfos = new ArrayList<>();
			for (Link link : job.getLinks()) {
				link.setHref(getRelHref(link.getRel(), job));
				link.setMethod(getMethod(link.getRel(), job));
				linkInfos.add(new LinkInfo(link, uriInfo));
			}
		}
	}

	private String getScheduledJobHref(Job job) {
		return referential+"/scheduled_jobs/"+job.getId();
	}

	private String getTerminatedJobHref(Job job) {
		return referential+"/terminated_jobs/"+job.getId();
	}

	private String getRelHref(String rel, Job job) {
		if (rel.equals(Link.LOCATION_REL) && hasTerminatedState(job)) {
			return getTerminatedJobHref(job);
		} else if (rel.equals(Link.LOCATION_REL) && !hasTerminatedState(job)) {
			return getScheduledJobHref(job);
		}
		return null;
	}

	private boolean hasTerminatedState(Job job) {
		return terminatedStates().contains(job.getStatus());
	}

	private Set<JobStatus> terminatedStates() {
		Set<JobStatus> set = new HashSet<JobStatus>();
		set.add(JobStatus.FINISHED);
		return set;
	}

	private String getMethod(String rel, Job jobService) {
		if (rel.equals(Link.LOCATION_REL)) {
			return Link.GET_METHOD;
		}
		return null;
	}

	@XmlType(name = "jobStatus")
	@XmlEnum(String.class)
	public enum STATUS implements java.io.Serializable {
		RESCHEDULED, SCHEDULED, STARTED, TERMINATED, CANCELED, ABORTED
	}

}
