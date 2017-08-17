# [projects]MicroMessage

This project is intended to demonstrate end-to-end best practices for building a cloud native, microservice architecture using Spring Boot&Cloud.

## Table of Contents

   * [Application 'Micro Message'](#micro-message)
      * [What is cloud native](#what-is-cloud-native)
      * [Architecture](#architecture)
      * [Backing services](#backing-services)
         * [API Gateway (edge service)](#message-client)
         * [Config server](#config-server)
         * [Service registry (Eureka)](#service-registry-eureka)
         * [Authorization (Oauth2) server](#authorization-oauth2-server)
		 * [Zipkin server(distributed tracing)](#message-zipkin)
      * [Backend Microservices](#backend-microservices)
         * [Message Service](#message-service)
         * [Message Client](#message-client)
      * [Running Instructions](#running-instructions)
         * [Via gradle (spring boot)](#via-gradle-spring-boot)
            * [Usage](#usage)
               * [Get a token:](#get-a-token)
               * [MessageService:](#message-service)
			   * [MessageClient:](#message-client)
      * [References and further reading](#references-and-further-reading)


## What is cloud native

To understand “cloud native,” we must first understand “cloud.”
In the context of this application, cloud refers to Platform as a Service. PaaS providers expose a platform that hides infrastructure details from the application developer, where that platform resides on top of Infrastructure as a Service (IaaS). 

A cloud-native application is an application that has been designed and implemented to run on a Platform-as-a-Service installation and to embrace horizontal elastic scaling.

## Architecture

The microservice architectural style is an approach to developing a single application as a suite of small services, each running in its own process and communicating with lightweight mechanisms, often an HTTP resource API.
![API gateway](https://i.imgsafe.org/cb23e2cd16.png)

## Backing services

The premise is that there are third-party service dependencies that should be treated as attached resources to your cloud native applications. The key trait of backing services are that they are provided as bindings to an application in its deployment environment by a cloud platform. Each of the backing services must be located using a statically defined route

###  API Gateway

Implementation of an API gateway that is the single entry point for all clients. The API gateway handles requests in one of two ways. Some requests are simply proxied/routed to the appropriate service. It handles other requests by fanning out to multiple services.

### Config server

The configuration service is a vital component of any microservices architecture. Based on the twelve-factor app methodology, configurations for your microservice applications should be stored in the environment and not in the project.

The configuration service is essential because it handles the configurations for all of the services through a simple point-to-point service call to retrieve those configurations. The advantages of this are multi-purpose.

Let's assume that we have multiple deployment environments. If we have a staging environment and a production environment, configurations for those environments will be different. A configuration service might have a dedicated Git repository for the configurations of that environment. None of the other environments will be able to access this configuration, it is available only to the configuration service running in that environment.

When the configuration service starts up, it will reference the path to those configuration files and begin to serve them up to the microservices that request those configurations. Each microservice can have their configuration file configured to the specifics of the environment that it is running in. In doing this, the configuration is both externalized and centralized in one place that can be version-controlled and revised without having to restart a service to change a configuration.

With management endpoints available from Spring Cloud, you can make a configuration change in the environment and signal a refresh to the discovery service that will force all consumers to fetch the new configurations.

### Service registry (Eureka)

Netflix Eureka is a service registry. It provides a REST API for service instance registration management and for querying available instances. Netflix Ribbon is an IPC client that works with Eureka to load balance requests across the available service instances.

When using client-side discovery, the client is responsible for determining the network locations of available service instances and load balancing requests across them. The client queries a service registry, which is a database of available service instances. The client then uses a load balancing algorithm to select one of the available service instances and makes a request.

The client-side discovery pattern has a variety of benefits and drawbacks. This pattern is relatively straightforward and, except for the service registry, there are no other moving parts. Also, since the client knows about the available services instances it can make intelligent, application-specific load balancing decisions such as using hashing consistently. One significant drawback of this pattern is that it couples the client to the service registry. You must implement client-side service discovery logic for each programming language and framework used by your service clients


### Authorization (Oauth2) server

For issuing tokens and authorize requests.


## Backend Microservices

While the backing services in the middle layer are still considered to be microservices, they solve a set of concerns that are purely operational and security-related. The business logic of this application sits almost entirely in our bottom layer.

## Streams

While REST is an easy, powerful approach to building services, it doesn't provide much in the way of guarantees about state. A failed write needs to be retried, requiring more work of the client. Messaging, on the other hand, guarantees that eventually the intended write will be processed. Eventual consistency works most of the time; even banks don't use distributed transactions! In this lab, we'll look at Spring Cloud Stream which builds atop Spring Integration and the messaging subsystem from Spring XD. Spring Cloud Stream provides the notion of binders that automatically wire up message egress and ingress given a valid connection factory and an agreed upon destination (e.g.: app-messages or items).

start ./bin/rabbitmq.sh.
This will install a RabbitMQ instance that is available at $DOCKER_IP. You'll also be able to access the console, which is available http://$DOCKER_IP:15672. The username and password to access the console are guest/guest.
add org.springframework.cloud:spring-cloud-starter-stream-rabbit to both the reservation-client and reservation-service.
Sources - like water from a faucet - describe where messages may come from. In our example, messages come from the reservation-client that wishes to write messages to the reservation-service from the API gateway.
add @EnableBinding(Source.class) to the reservation-client DemoApplication
create a new REST endpoint - a POST endpoint that accepts a @RequestBody Reservation reservation - in the ReservationApiGatewayRestController to accept new reservations
observe that the Source.class describes one or more Spring MessageChannels which are themselves annotated with useful qualifiers like @Output("output").
in the new endpoint, inject the Spring MessageChannel and qualify it with @Output("output") - the same one as in the Source.class definition.
use the MessageChannel to send a message to the reservation-service. Connect the two modules through a agreed upon name, which we'll call reservations.
Observe that this is specified in the config server for us in the reservation-service module: spring.cloud.stream.bindings.output=reservations. output is arbitrary and refers to the (arbitrary) channel of the same name described and referenced from the Source.class definition.
Sinks receive messages that flow to this service (like the kitchen sink into which water from the faucet flows).
add @EnableBinding(Sink.class) to the reservation-service DemoApplication
observe that the Sink.class describes one or more Spring MessageChannels which are themselves annotated with useful qualifiers like @Input("input").
create a new @MessagingEndpoint that has a @ServiceActivator-annotated handler method to receive messages whose payload is of type String, the reservationName from the reservation-client.
use the String to save new Reservations using an injected ReservationRepository
Observe that this is specified in the config server for us in the reservation-client module: spring.cloud.stream.bindings.input=reservations. input is arbitrary and refers to the (arbitrary) channel of the same name described in the Sink.class definition.

### Message Service

Message are exposed as REST resources using Spring Data RESTs capability to automatically expose Spring Data JPA repositories contained in the application.

### Message Client 

Message Client serves as proxy to original Message Service.

We're using Spring Data REST to expose the OrderRepository as REST resource without additional effort.

Spring Hateoas provides a generic Resource abstraction that we leverage to create hypermedia-driven representations. Spring Data REST also leverages this abstraction so that we can deploy ResourceProcessor implementations (e.g. PaymentorderResourceProcessor) to enrich the representations for Order instance with links to the PaymentController.


## Running Instructions
### Via gradle (spring boot)

Make sure you have Rabbit MQ running on localhost (on default ports).

```bash
$ cd micro-message/message-config-server
$ gradlew bootRun
```
```bash
$ cd micro-message/message-eureka
$ gradlew bootRun
```
```bash
$ cd micro-message/message-oauth2
$ gradlew bootRun
```
```bash
$ cd micro-message/message-hystrix-dashboard
$ gradlew bootRun
```
```bash
$ cd micro-message/message-zipkin
$ gradlew bootRun
```
```bash
$ cd micro-message/message-dataflow
$ gradlew bootRun
```
...
- Repeat this for all other services that you want to run. Please note that the order is important (config-server, erureka, authserver)
- After you run services, trigger shell scripts under script folder of each service to create sample data.

#### Usage

##### Get a token: 
```bash
$ curl -X POST -vu acme:acmesecret http://localhost:9999/uaa/oauth/token -H "Accept: application/json" -d "password=spring&username=ad&grant_type=password&client_secret=acmesecret&client_id=acme"
```

##### Message client: 
```bash
$ curl http://localhost:9999/ -H "Authorization: Bearer <YOUR TOKEN>"
```

## References and further reading

  * http://martinfowler.com/articles/microservices.html
  * http://microservices.io/
  * http://www.slideshare.net/chris.e.richardson/developing-eventdriven-microservices-with-event-sourcing-and-cqrs-phillyete
  * http://12factor.net/
  * http://pivotal.io/platform/migrating-to-cloud-native-application-architectures-ebook
  * http://pivotal.io/beyond-the-twelve-factor-app
  * https://blog.docker.com/2016/02/containers-as-a-service-caas/
  * http://www.kennybastani.com/2016/04/event-sourcing-microservices-spring-cloud.html
  


