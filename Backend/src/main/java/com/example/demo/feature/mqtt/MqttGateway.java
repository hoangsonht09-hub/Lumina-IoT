package com.example.demo.feature.mqtt;

import com.example.demo.common.BusinessException;
import com.example.demo.config.MqttProperties;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MqttGateway implements MqttPublisher, MqttCallbackExtended {
    private static final Logger log = LoggerFactory.getLogger(MqttGateway.class);

    private final MqttProperties properties;
    private final IotPayloadParser parser;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private volatile MqttAsyncClient client;

    public MqttGateway(MqttProperties properties, IotPayloadParser parser,
                       ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.parser = parser;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(initialDelay = 1000, fixedDelayString = "${app.mqtt.reconnect-delay-ms:5000}")
    public void ensureConnected() {
        if (!properties.enabled() || isConnected() || !connecting.compareAndSet(false, true)) return;
        try {
            if (client == null) {
                String clientId = properties.clientId() + "-" + UUID.randomUUID().toString().substring(0, 8);
                client = new MqttAsyncClient(properties.brokerUri(), clientId, new MemoryPersistence());
                client.setCallback(this);
            }
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(3);
            options.setKeepAliveInterval(20);
            if (properties.username() != null && !properties.username().isBlank()) {
                options.setUserName(properties.username());
                options.setPassword(properties.password() == null ? new char[0] : properties.password().toCharArray());
            }
            client.connect(options, null, new org.eclipse.paho.client.mqttv3.IMqttActionListener() {
                @Override
                public void onSuccess(org.eclipse.paho.client.mqttv3.IMqttToken asyncActionToken) {
                    connecting.set(false);
                    log.info("MQTT connected to {}", properties.brokerUri());
                }

                @Override
                public void onFailure(org.eclipse.paho.client.mqttv3.IMqttToken asyncActionToken, Throwable exception) {
                    connecting.set(false);
                    log.warn("MQTT connection failed: {}", exception.getMessage());
                }
            });
        } catch (MqttException exception) {
            connecting.set(false);
            log.warn("Cannot initialize MQTT client: {}", exception.getMessage());
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try {
            client.subscribe(new String[]{properties.sensorTopic(), properties.statusTopic()}, new int[]{1, 1});
            log.info("MQTT subscribed to {} and {}", properties.sensorTopic(), properties.statusTopic());
        } catch (MqttException exception) {
            log.error("Cannot subscribe MQTT topics", exception);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost: {}", cause == null ? "unknown" : cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        try {
            if (properties.sensorTopic().equals(topic)) {
                eventPublisher.publishEvent(parser.parseSensorPayload(payload));
            } else if (properties.statusTopic().equals(topic)) {
                eventPublisher.publishEvent(parser.parseStatusPayload(payload));
            }
        } catch (RuntimeException exception) {
            log.warn("Ignored invalid MQTT message on {}: {}", topic, exception.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // QoS 1 delivery is synchronously awaited in publishCommand.
    }

    @Override
    public void publishCommand(String command) {
        if (!properties.enabled()) return;
        if (!isConnected()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "MQTT_OFFLINE",
                    "Backend chưa kết nối được MQTT broker");
        }
        try {
            MqttMessage message = new MqttMessage(command.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            message.setRetained(false);
            client.publish(properties.commandTopic(), message).waitForCompletion(3000);
        } catch (MqttException exception) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "MQTT_PUBLISH_FAILED",
                    "Không thể gửi lệnh tới phần cứng");
        }
    }

    @Override
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    @Override
    public boolean isEnabled() {
        return properties.enabled();
    }
}
