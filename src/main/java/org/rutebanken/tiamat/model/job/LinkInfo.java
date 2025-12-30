package org.rutebanken.tiamat.model.job;

import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder={"rel","href","type","method"})
public class LinkInfo {

	@XmlElement(name = "rel", required=true)
	private String rel;

	@XmlElement(name = "href", required=true)
	private String href;

	@XmlElement(name = "type", required=true)
	private String type;

	@XmlElement(name = "method", required=true)
	private String method;
	
	public LinkInfo(Link link, UriInfo uriInfo)
	{
		rel = link.getRel();
		href = uriInfo.getBaseUri()+link.getHref();
		type = link.getType();
		method = link.getMethod();
	}

}
