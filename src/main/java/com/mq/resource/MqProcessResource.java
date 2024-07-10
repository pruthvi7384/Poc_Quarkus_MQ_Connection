package com.mq.resource;

import com.mq.dto.ResponseDto;
import com.mq.service.MqProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/mq")
public class MqProcessResource {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    MqProducerService mqProducerService;

    @POST
    @Path("/send-message")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response mqProcess(@NotEmpty(message = "Pass Any Message") @NotNull(message = "Pass Any Message") String requestMessage){
        try {
            ResponseDto response = mqProducerService.processToProduceMessage(requestMessage).await().indefinitely();
            return Response.status(Response.Status.OK).entity(response).build();
        }catch (Exception e){
            logger.error("Error:- ",e);
            ResponseDto response = new ResponseDto();
            response.setErrorCode("500");
            response.setErrorDescription("FAILURE");
            response.setErrorDescription("Something went wrong during the send message to amqp. For more details please check the logs.");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }

    @GET
    @Path("/receive-message")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response mqProcessGetMessage(@NotEmpty(message = "Pass Correlation Id in Header") @NotNull(message = "Pass Correlation Id in Header") @HeaderParam("correlation_id") String correlationId){
        try {
            ResponseDto response = mqProducerService.processToConsumeResponse(correlationId).await().indefinitely();
            return Response.status(Response.Status.OK).entity(response).build();
        }catch (Exception e){
            logger.error("Error:- ",e);
            ResponseDto response = new ResponseDto();
            response.setErrorCode("500");
            response.setErrorDescription("FAILURE");
            response.setErrorDescription("Something went wrong during the consume the message from amqp. For more details please check the logs.");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
        }
    }
}
