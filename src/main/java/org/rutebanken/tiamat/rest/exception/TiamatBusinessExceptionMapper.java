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

package org.rutebanken.tiamat.rest.exception;

import com.google.common.collect.Sets;
import org.rutebanken.helper.organisation.NotAuthenticatedException;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.OptimisticLockException;
import javax.validation.ValidationException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.*;
import static org.rutebanken.tiamat.rest.exception.TiamatBusinessException.DUPLICATE_IMPORTED_ID;
import static org.rutebanken.tiamat.rest.exception.TiamatBusinessException.TRANSPORT_MODE_MISMATCH;

@Provider
public class TiamatBusinessExceptionMapper implements ExceptionMapper<TiamatBusinessException> {


    public TiamatBusinessExceptionMapper() {
    }


    public Response toResponse(TiamatBusinessException ex) {
        int errorCode = ex.getCode();
        Response.Status status = mapBusinessCodeToHttpCode(errorCode);
        return Response.status(status)
                       .entity(new ErrorResponseEntity(ex.getMessage(), errorCode))
                       .build();
    }

    public static Response.Status mapBusinessCodeToHttpCode(int businessErrorCode) {
        Response.Status responseStatus;
        switch (businessErrorCode) {
            case TRANSPORT_MODE_MISMATCH:
                responseStatus = BAD_REQUEST;
                break;
            default:
                responseStatus = INTERNAL_SERVER_ERROR;
        }
        return responseStatus;
    }
}
