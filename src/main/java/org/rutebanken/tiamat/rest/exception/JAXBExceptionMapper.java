package org.rutebanken.tiamat.rest.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.JAXBException;

@Provider
public class JAXBExceptionMapper implements ExceptionMapper<JAXBException> {
    @Override
    public Response toResponse(JAXBException exception) {
        return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponseEntity("Error converting payload " +
                "to XML")).build();
    }
}
