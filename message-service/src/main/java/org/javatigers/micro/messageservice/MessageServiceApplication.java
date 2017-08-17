package org.javatigers.micro.messageservice;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.util.stream.Stream;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;
import lombok.NoArgsConstructor;

@EnableBinding(MessageChannels.class)
@EnableDiscoveryClient
@SpringBootApplication
public class MessageServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MessageServiceApplication.class, args);
	}
}

interface MessageChannels {
	
	@Input
	SubscribableChannel input();
}

@MessageEndpoint
class MessageProcessor {
	
	private final MessageRepository messageRepository;
	
	public MessageProcessor(MessageRepository messageRepository) {
		this.messageRepository = messageRepository;
	}
	
	@ServiceActivator(inputChannel = "input")
	public void onNewMessage(String message) {
		messageRepository.save(new Message (message));
	}
}

@RestController
@RefreshScope
class PropsValueController {
	
	private final String propVal;
	
	public PropsValueController (@Value("${message}") String propVal) {
		this.propVal = propVal;
	}
	
	@RequestMapping(method = GET, value = "/message")
	public String readPropVal () {
		return this.propVal;
	}
} 

@Component
class SampleDataCLR implements CommandLineRunner {

	private MessageRepository messageRepository;
	
	@Autowired
	public SampleDataCLR(MessageRepository messageRepository) {
		this.messageRepository = messageRepository;
	}
	
	@Override
	public void run(String... args) throws Exception {
		Stream.<String>of("Hi", "Hello", "welcome", "abc")
			.map(Message::new)
			.forEach(messageRepository::saveAndFlush);
		
		messageRepository.findAll()
			.forEach(System.out::println);
	}
	
}

@RepositoryRestResource
interface MessageRepository extends JpaRepository<Message, Long> {
	
}

@Entity
@Data
@NoArgsConstructor
class Message {
	
	public Message (String message) {
		this.message = message;
	}
	@Id
	@GeneratedValue
	private Long id;
	
	private String message;
	
	public String toString () {
		return "Message { " + "id = " + id + " , message = " + message + " }";
	}
}
