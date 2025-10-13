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

## License
This project is licensed under the MIT License.

