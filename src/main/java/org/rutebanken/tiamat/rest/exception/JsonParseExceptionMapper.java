package org.rutebanken.tiamat.rest.exception;

import com.fasterxml.jackson.core.JsonParseException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {
    private static final Logger log = LoggerFactory.getLogger(JsonParseExceptionMapper.class);

    @Override
    public Response toResponse(JsonParseException exception) {
        log.error("Exception caught", exception);

        return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponseEntity("Payload is not a valid " +
                "JSON")).build();
    }
}
