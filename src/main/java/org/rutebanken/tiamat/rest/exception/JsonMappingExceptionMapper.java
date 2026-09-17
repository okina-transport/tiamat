package org.rutebanken.tiamat.rest.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {
    private static final Logger log = LoggerFactory.getLogger(JsonMappingExceptionMapper.class);

    @Override
    public Response toResponse(JsonMappingException exception) {
        log.error("Exception caught", exception);

        return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponseEntity("Error mapping JSON " +
                "payload")).build();
    }
}
