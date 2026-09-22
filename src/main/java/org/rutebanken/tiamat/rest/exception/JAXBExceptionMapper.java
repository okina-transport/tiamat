package org.rutebanken.tiamat.rest.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class JAXBExceptionMapper implements ExceptionMapper<JAXBException> {
    private static final Logger log = LoggerFactory.getLogger(JAXBExceptionMapper.class);

    @Override
    public Response toResponse(JAXBException exception) {
        log.error("Exception caught", exception);

        return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponseEntity("Error converting payload " +
                "to XML")).build();
    }
}
