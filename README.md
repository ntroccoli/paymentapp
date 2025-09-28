# Simple Payment Application

A simple Spring Boot application demonstrating a payment processing API with webhook notifications.

It uses an in-memory H2 database for storage and includes a built-in webhook receiver for testing.

## Requirements
- Java 21 SDK installed (JDK 21)

## Run the app
- Linux/macOS:
```bash
./mvnw spring-boot:run
```
- Windows (cmd.exe):
```bat
mvnw.cmd spring-boot:run
```
- Windows (PowerShell):
```powershell
.\mvnw.cmd spring-boot:run
```
Then open Swagger UI: http://localhost:8080/swagger-ui

## Available endpoints
- POST /payments — Create a payment. 201 Created (with Location header) or 200 OK if the orderNumber already exists.
- GET /payments?orderNumber=ORD-123 — Get a payment by order number. 200 or 404.
- GET /payments/{transactionId} — Get a payment by transaction id. 200 or 404.
- POST /webhooks — Register a webhook URL to be called when a payment is created.
- POST /webhooks/receiver — Local loopback webhook receiver for development/testing. Accepts the same payload sent to external webhooks.


## Webhook notifications
- When a payment is created, the app will POST a small JSON payload to every registered webhook URL. The payload includes only safe fields: transactionId, orderNumber, status.
- A simple retry with exponential backoff is used (up to 3 attempts per URL). Every call attempt is logged.

### Local loopback testing
This project includes a built-in webhook receiver at `/webhooks/receiver`.

Notes
- The local receiver is for development only.

## Running H2 Console
1) Start the app, then go to: http://localhost:8080/h2-console
2) Set JDBC URL to: jdbc:h2:mem:paymentsdb
3) Username: sa, Password: (leave empty)
4) Click Connect

## Export a fresh OpenAPI spec (openapi.yaml)
Exports once and exits using the "export" profile.
- Linux/macOS:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=export
```
- Windows (cmd.exe):
```bat
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=export
```
- Windows (PowerShell):
```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=export"
```
This writes openapi.yaml to the project root and then terminates. You can override the file name with -Dopenapi.export.output-file=somefile.yaml.

## API docs
- Swagger UI: http://localhost:8080/swagger-ui
- OpenAPI (JSON): http://localhost:8080/api-docs
- OpenAPI (YAML): http://localhost:8080/api-docs.yaml

## GitHub Copilot Prompts
- I'm using org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13 in this app and I want to update the swagger documentation, and create an OpenAPI Specification and store it with examples at the root of your project, ensuring proper return codes and meaningful information. What is the best way to use that in a Spring Boot 3 application using the best practices and design patterns? No unit tests are needed for this and keep the solution as simple as possible.
    - Response Plan:
      - Add clear, rich OpenAPI annotations and stable operationIds.
      - Define top-level API info (title, version, server).
      - Use ProblemDetail for errors with examples and meaningful codes.
      - Add a tiny one-shot exporter profile to write openapi.yaml at the project root.
      - Verify build and export.

- Implement simple unit tests and integration tests for the app using Spring Boot best practices.
    - Response: I'll add unit tests for the service, controller, repository, and encryption, plus an end-to-end integration test using MockMvc and H2, then run the Maven test suite to verify everything passes.

- Implement the WebhookService.notifyWebhooks method in order to retrieve the existing webhooks from the database and call them using Resttemplate using a good and simple retry design pattern. Log each of the calls. Create a sample controller method to be initially called as test in this application and update the readme for this scenario. Unit tests and openAPI/Swagger documentation are not necessary now and should be ignored.
    - Response Planned edits:
      - Implement WebhookService.notifyWebhooks with RestTemplate POST calls, retry with backoff, and comprehensive logging.
      - Add @EnableAsync to PaymentappApplication to activate @Async methods.
      - Add a simple test endpoint in WebhookController to trigger notifications with a sample Payment instance.

- Update the registerWebhook to use a better exception than the IllegalArgumentException to be handled in the GlobalExceptionHandler and update the swagger documentation for the webhook controller with good examples and response status codes.
    - Response Planned edits:
        - Add a new exception class WebhookAlreadyExistsException.
        - Update ProblemType with a RESOURCE_CONFLICT URI.
        - Add an exception handler for conflict in GlobalExceptionHandler.
        - Change WebhookService.registerWebhook to throw the new exception.
        - Enhance Swagger annotations in WebhookController with examples and response codes.

- Create unit tests and integration tests for the webhooks functionality.
    - Response: I'll scan the repository to locate the webhooks-related code (controllers, services, models) so I can design appropriate unit and integration tests, then implement and run them to verify green.

## License
This project is licensed under the MIT License.

