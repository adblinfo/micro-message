package org.javatigers.client.messageclient;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.hateoas.Resources;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.MessageChannel;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

//create binding message channels with spring cloud stream.
@EnableResourceServer
@IntegrationComponentScan
@EnableBinding(MessageChannels.class)
@EnableFeignClients
@EnableDiscoveryClient
@EnableZuulProxy
@EnableCircuitBreaker
@SpringBootApplication
public class MessageClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(MessageClientApplication.class, args);
	}
}

interface MessageChannels {
	
	//Create multiple message channels for service basis like one for orders, customers, products.
	@Output
	MessageChannel output ();
}

@FeignClient("message-service")	
interface MessagesReader {
	
	@RequestMapping (method = RequestMethod.GET, value = "/messages")
	Resources<Message> readMessage ();
}

@MessagingGateway
interface MessageWriter {
	@Gateway(requestChannel = "output")
	void write (String message);
}

class Message {
	private String message;
	
	public String getMessage () {
		return this.message;
	}
}

@RestController
@RequestMapping("/messages")
class MessageAPIGateway {

	private final MessagesReader messageReader;
	private final MessageWriter messageWriter;
	@Autowired
	public MessageAPIGateway (MessageWriter messageWriter, MessagesReader messageReader) {
		this.messageWriter = messageWriter;
		this.messageReader = messageReader;
	}
	
	@RequestMapping(method = POST)
	public void write (@RequestBody Message message) {
		messageWriter.write(message.getMessage());
	}
	
	public List<String> fallbackMessage () {
		return Arrays.asList("Default");
	}
	
	@HystrixCommand(fallbackMethod = "fallbackMessage")
	@RequestMapping (method = RequestMethod.GET, value = "/messages")
	public List<String> messages () {
		return this.messageReader.readMessage()
				.getContent()
				.stream()
				.map(Message::getMessage)
				.collect(Collectors.toList());
	}
} 


