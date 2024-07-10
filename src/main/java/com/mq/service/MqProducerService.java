package com.mq.service;

import com.mq.dto.ResponseDto;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.amqp.IncomingAmqpMetadata;
import io.smallrye.reactive.messaging.amqp.OutgoingAmqpMetadata;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * MQ Message Produce Service
 */
@ApplicationScoped
public class MqProducerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Channel("request-topic-out")
    Emitter<String> requestEmitter;

    @ConfigProperty(name = "mq.outgoing.request.message.subject")
    String outgoingMessage;

    @ConfigProperty(name = "mq.outgoing.response.waiting.time")
    int responseWaitingPoint;

    @Inject
    MqConsumerService mqConsumerService;

    /**
     * Process Of Produce the message
     */
    public Uni<ResponseDto> processToProduceMessage(String requestMessage){
        return sendMessageToMQ(requestMessage)
                .onItem()
                .delayIt().by(Duration.ofSeconds(responseWaitingPoint))
                .onItem()
                .transform(correlationId -> {
                    // Validate and get response
                    String validateResponse = processOfReceiveTheResponse(correlationId);

                    // Preparing the final response that need to sent client
                    ResponseDto response = new ResponseDto();

                    if(validateResponse.isEmpty()){
                        response.setResponse("Message is produce successfully. But unable to get response message, Please refer '"+correlationId+"' this correlation id to track the request message.");
                    }else {
                        response.setResponse("Message is produce successfully. Please refer '"+correlationId+"' this correlation id to track the request message.");
                    }

                    response.setErrorCode("200");
                    response.setErrorDescription("SUCCESS");
                    response.setMqResponseReceived(validateResponse);

                    return response;
                });
    }

    /**
     * Produce the message to MQ Topic
     */
    private Uni<String> sendMessageToMQ(String requestMessage) {
        // Code for the first operation
        return Uni.createFrom().item(generateCorrelationId.get())
                .onItem()
                .transform(correlationId  -> {
                    processSendMessageToMQ.accept(requestMessage,correlationId);
                    return correlationId;
                });
    }

    /**
     * Process to send the message to mq
     */
    BiConsumer<String, String> processSendMessageToMQ = (requestMessage, correlationId) -> {
        logger.info("MQ request message that need to process:- {}", requestMessage);
        logger.info("MQ request message send correlation id:- {}", correlationId);

        // 1. Build metadata for the outgoing message
        OutgoingAmqpMetadata outgoingMetadata = OutgoingAmqpMetadata.builder()
                .withSubject(outgoingMessage)
                .withCorrelationId(correlationId)
                .withDurable(true)
                .build();

        // 2. Send the message using the Reactive Messaging Emitter
        requestEmitter.send(Message.of(requestMessage, Metadata.of(outgoingMetadata)));
    };

    /**
     * Receive the message from another topic
     */
    private String processOfReceiveTheResponse(String correlationId) {
        // 1. Receive the response from another topic
        List<Message<String>> responseMessageList = mqConsumerService.getAllProcessedMessages();

        // 2. Validate and get the response
        return validateAndGetResponse.apply(responseMessageList,correlationId);
    }

    /**
     * Get Correlation Id Dynamically
     */
    Supplier<String> generateCorrelationId = () -> UUID.randomUUID().toString();

    /**
     * Process And Validate the response
     */
    BiFunction<List<Message<String>>,String,String> validateAndGetResponse = (List<Message<String>> responseMessageList,String requestCorrelId) -> responseMessageList.stream()
            .filter(responseMessage -> {
                boolean status = false;

                // Get Response Metadata
                Optional<IncomingAmqpMetadata> incomingMetadata = responseMessage.getMetadata(IncomingAmqpMetadata.class);
                AtomicReference<String> correlId = new AtomicReference<>("");

                // Getting MQ Metadata Details And Set to Validate
                incomingMetadata
                        .ifPresent(incomingAmqpMetadata -> correlId.set(incomingAmqpMetadata.getCorrelationId()));

                // Validate the Response Message
                logger.info("MQ Response Message Correlation Id:- {}",correlId.get());
                if (correlId.get().equalsIgnoreCase(requestCorrelId)){
                    status = true;
                }

                return status;
            }).findFirst().get().getPayload();

    /**
     * Process Of Get Response Message
     */
    public Uni<ResponseDto> processToConsumeResponse(String correlationId){
        logger.info("Correlation Id For Consume The Response Message:- {}",correlationId);
        return Uni.createFrom().item(()->{
            // Validate and get response
            String validateResponse = processOfReceiveTheResponse(correlationId);

            // Preparing the final response that need to sent client
            ResponseDto response = new ResponseDto();

            if(validateResponse.isEmpty()){
                response.setResponse("Response Message is empty please check '"+correlationId+"' this correlation is it correct.");
            }else {
                response.setResponse("Response Message Get Successfully!");
            }

            response.setErrorCode("200");
            response.setErrorDescription("SUCCESS");
            response.setMqResponseReceived(validateResponse);

            return response;
        });
    }
}
