package org.domiot.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

@Configuration
@Slf4j
public class MqttConfiguration {

    @Value("${mqtt.url}")
    private String url;// tcp://localhost:1883
    @Value("${mqtt.username}")
    private String userName; // johndoe
    @Value("${mqtt.password}")
    private String password; // secret
    @Value("${mqtt.caFilePath}")
    private String caFilePath; // /etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem
    @Value("${mqtt.crtFilePath}")
    private String crtFilePath; // null
    @Value("${mqtt.clientKeyFilePath}")
    private String clientKeyFilePath; // null

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer inbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(url, "mqtt-backend", mqttClientFactory(), "#");
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{url});
        options.setUserName(userName);
        options.setPassword(password.toCharArray());
        factory.setConnectionOptions(options);
        return factory;
    }
}
