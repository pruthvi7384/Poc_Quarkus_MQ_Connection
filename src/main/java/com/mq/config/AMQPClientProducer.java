package com.mq.config;

import io.vertx.amqp.AmqpClientOptions;
import io.vertx.core.net.JksOptions;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.time.Instant;

public class AMQPClientProducer {

    @ConfigProperty(name = "amqp-port", defaultValue = "5672")
    Integer amqpPort;

    @ConfigProperty(name = "amqp-use-ssl", defaultValue = "false")
    Boolean amqpUseSSL;

    @ConfigProperty(name = "amqp-container-id", defaultValue = "auditLog")
    String amqpContainerId;

    @ConfigProperty(name = "amqp-dynamic-containerid", defaultValue = "false")
    Boolean amqpDynamicContaninerId;

    @ConfigProperty(name = "amqp-tcp-keepalive", defaultValue = "true")
    Boolean amqpTcpKeepAlive;

    @ConfigProperty(name = "amqp-cipher-suite", defaultValue = "TLS_RSA_WITH_AES_128_GCM_SHA256")
    String amqpCipherSuite;

    @ConfigProperty(name = "quarkus.certificate.file.url")
    String jksFilePath;

    @ConfigProperty(name = "quarkus.certificate.file.password", defaultValue = "changeit")
    String jksFilePassword;

    @Produces
    @Named("amqp-client-named-options")
    public AmqpClientOptions getNamedOptions() {

        JksOptions jksOptions = new JksOptions()
            .setPath(jksFilePath)
            .setPassword(jksFilePassword);

        if (Boolean.TRUE.equals(amqpDynamicContaninerId)) {
            amqpContainerId += Instant.now().toEpochMilli();
        }

        return new AmqpClientOptions()
            .setSsl(amqpUseSSL)
            .setHostnameVerificationAlgorithm("")
            .setKeyStoreOptions(jksOptions)
            .setTrustStoreOptions(jksOptions)
            .addEnabledCipherSuite(amqpCipherSuite)
            .setContainerId(amqpContainerId)
            .setTcpKeepAlive(amqpTcpKeepAlive)
            .setPort(amqpPort)
            ;
    }

}

