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


import jakarta.validation.ValidationException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.rutebanken.tiamat.config.Messages;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.TransactionSystemException;

import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GeneralExceptionMapperTest {

    @Mock
    Messages messages;

    @Test
    void rawAccessDeniedExceptionYieldsForbidden() {
        Response rsp = new GeneralExceptionMapper(messages).toResponse(new AccessDeniedException("Nope"));
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), rsp.getStatus());
    }

    @Test
    void nestedAccessDeniedExceptionYieldsForbidden() {
        Response rsp = new GeneralExceptionMapper(messages).toResponse(new TransactionSystemException("", new AccessDeniedException("Nope")));
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), rsp.getStatus());
        assertEquals("Forbidden", ((ErrorResponseEntity) rsp.getEntity()).errors.get(0).message);
    }


    @Test
    void nestedValidationExceptionYieldsBadRequest() {
        Response rsp = new GeneralExceptionMapper(messages).toResponse(new TransactionSystemException("", new ValidationException()));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rsp.getStatus());
    }

    @Test
    void nestedUnknownExceptionYieldsInternalServerError() {
        Response rsp = new GeneralExceptionMapper(messages).toResponse(new TransactionSystemException("", new RuntimeException()));
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rsp.getStatus());
    }

    @Test
    void nestedNotAuthorizedExceptionYieldsUnauthorized() {
        Response rsp = new GeneralExceptionMapper(messages).toResponse(new TransactionSystemException("", new NotAuthorizedException("Njet")));
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), rsp.getStatus());
    }

    @Test
    void rawUnknownExceptionYieldsInternalServerError() {
        Response rsp = new GeneralExceptionMapper(messages).toResponse(new FileNotFoundException());
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rsp.getStatus());
    }
}
