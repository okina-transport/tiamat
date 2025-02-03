package org.rutebanken.tiamat.externalapis.gbfs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.rutebanken.tiamat.model.gbfs.api.GbfsDataApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
public class GbfsClient {

    private static final Logger logger = LoggerFactory.getLogger(GbfsClient.class);

    public <T> GbfsDataApiResponse<T> getData(String endpoint, Class<T> dataType) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(endpoint);

        Response response = target.request(MediaType.APPLICATION_JSON).get();
        GbfsDataApiResponse<T> apiResponse = null;

        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                apiResponse = objectMapper.readValue(response.readEntity(String.class),
                        objectMapper.getTypeFactory().constructParametricType(GbfsDataApiResponse.class, dataType));
            } catch (Exception e) {
                logger.error("Error parsing response for target url {}", endpoint, e);
            }
        } else {
            logger.error("HTTP GET {} - received status code {}", endpoint, response.getStatus());
        }

        response.close();
        client.close();
        return apiResponse;
    }
}
