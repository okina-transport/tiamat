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

package org.rutebanken.tiamat.rest.dto;

import org.junit.Before;
import org.junit.Test;
import org.rutebanken.tiamat.dtoassembling.dto.IdMappingDto;
import org.rutebanken.tiamat.repository.StopPlaceRepository;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;


public class DtoStopPlaceResourceTest {

    private StopPlaceRepository stopPlaceRepository = mock(StopPlaceRepository.class);
    private DtoStopPlaceResource dtoStopPlaceResource;

    @Before
    public void setUp() {
        dtoStopPlaceResource = new DtoStopPlaceResource(stopPlaceRepository, mock(DtoMappingSemaphore.class));
    }

    @Test
    public void keyValueStopPlaceMappingWithWithSize() throws IOException, InterruptedException {
        int keyValueMappingCount = 3;
        int size = 1;

        when(stopPlaceRepository.findKeyValueMappingsForStop(any(Instant.class), anyInt(), anyInt()))
                .thenReturn(Arrays.asList(new IdMappingDto("original id", BigInteger.ONE.toString())))
                .thenReturn(Arrays.asList(new IdMappingDto("original id", BigInteger.TEN.toString())))
                .thenReturn(Arrays.asList(new IdMappingDto("original id", BigInteger.ZERO.toString())))
                .thenReturn(new ArrayList<>());

        Response response = dtoStopPlaceResource.getIdMapping(size, false);
        StreamingOutput output = (StreamingOutput) response.getEntity();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        output.write(baos);
        // plus one for the last empty call.
        verify(stopPlaceRepository, times((keyValueMappingCount/size)+1)).findKeyValueMappingsForStop(any(Instant.class), anyInt(), anyInt());
    }

}