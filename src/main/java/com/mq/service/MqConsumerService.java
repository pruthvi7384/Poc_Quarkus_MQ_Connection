package com.mq.service;

import io.smallrye.reactive.messaging.amqp.IncomingAmqpMetadata;
import io.smallrye.reactive.messaging.amqp.OutgoingAmqpMetadata;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class MqConsumerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @ConfigProperty(name = "mq.outgoing.response.message.subject")
    String outgoingMessage;

    /**
     * Store the response messages
     */
    private final List<Message<String>> processedResponseMessages = new ArrayList<>();

    /**
     * Consume the message and produce same message as response to another topic (chanel)
     */
    @Incoming("request-topic-in")
    @Outgoing("response-topic-out")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    @Transactional
    public Message<String> consumeAndProcessRequestMessage(Message<String> requestMessage){
        logger.info("MQ request message received through AMQP channel is:- {}",requestMessage.getPayload());

        Optional<IncomingAmqpMetadata> incomingMetadata = requestMessage.getMetadata(IncomingAmqpMetadata.class);
        AtomicReference<String> correlId = new AtomicReference<>("");
        AtomicReference<String> subject = new AtomicReference<>("");

        // Getting MQ Metadata Details
        incomingMetadata
                .ifPresent(incomingAmqpMetadata -> {
                    correlId.set(incomingAmqpMetadata.getCorrelationId());
                    subject.set(incomingAmqpMetadata.getSubject());
                });

        logger.info("MQ Metadata details:-  correlation Id - {} | subject - {}",correlId,subject);

        // Process the incoming message and create a response
        String processedMessage = "Processed: " + requestMessage.getPayload();


        // Build metadata for the outgoing message
        OutgoingAmqpMetadata outgoingMetadata = OutgoingAmqpMetadata.builder()
                .withSubject(outgoingMessage)
                .withCorrelationId(correlId.get())
                .withDurable(true)
                .build();

        // Return the processed message as an outgoing message with metadata
        return Message.of(processedMessage, Metadata.of(outgoingMetadata));
    }

    /**
     *  Get the response from another topic (chanel)
     */
    @Incoming("response-topic-in")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    @Transactional
    public CompletionStage<Void> consumeResponseMessage(Message<String> responseMessage){
        logger.info("MQ response message received through AMQP channel is:- {}",responseMessage.getPayload());

        Optional<IncomingAmqpMetadata> incomingMetadata = responseMessage.getMetadata(IncomingAmqpMetadata.class);
        AtomicReference<String> correlId = new AtomicReference<>("");
        AtomicReference<String> subject = new AtomicReference<>("");

        // Getting MQ Metadata Details
        incomingMetadata
                .ifPresent(incomingAmqpMetadata -> {
                    correlId.set(incomingAmqpMetadata.getCorrelationId());
                    subject.set(incomingAmqpMetadata.getSubject());
                });

        logger.info("MQ Metadata details:-  correlation Id - {} | subject - {}",correlId,subject);

        // Store the response message to later uses
        processedResponseMessages.add(responseMessage);

        return responseMessage.ack();
    }

    /**
     * Get Response Message
     */
    public List<Message<String>> getAllProcessedMessages() {
        return new ArrayList<>(processedResponseMessages);
    }
}
