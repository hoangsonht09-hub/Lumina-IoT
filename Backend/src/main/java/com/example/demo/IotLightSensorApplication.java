package com.example.demo;

import com.example.demo.config.AuthProperties;
import com.example.demo.config.MqttProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties({AuthProperties.class, MqttProperties.class})
public class IotLightSensorApplication {
    public static void main(String[] args) {
        SpringApplication.run(IotLightSensorApplication.class, args);
    }
}
