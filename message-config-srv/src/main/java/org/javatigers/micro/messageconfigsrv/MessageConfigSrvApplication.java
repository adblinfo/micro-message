package org.javatigers.micro.messageconfigsrv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@EnableConfigServer
@SpringBootApplication
public class MessageConfigSrvApplication {

	public static void main(String[] args) {
		SpringApplication.run(MessageConfigSrvApplication.class, args);
	}
}
