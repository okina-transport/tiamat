package org.rutebanken.tiamat.model.job;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

@Embeddable
public class Link implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	public static final String GET_METHOD = "get";

	public static final String LOCATION_REL = "location";

	@Column(name = "type")
	private String type;
 
	@Column(name = "rel")
	private String rel;

	@Transient
	private String method;

	@Transient
	private String href;

    public Link(String type, String rel)
    {
    	this.type=type;
    	this.rel=rel;
    }

	public String getRel() {
		return rel;
	}

	public String getHref() {
		return href;
	}

	public String getType() {
		return type;
	}

	public String getMethod() {
		return method;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public void setMethod(String method) {
		this.method = method;
	}
}
