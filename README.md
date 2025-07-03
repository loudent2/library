# Loudent Library Service

A showcase Java Spring Boot application for managing a library catalog, user accounts, and borrowing activity. This service demonstrates clean architecture, asynchronous operations, unit test coverage, OpenAPI integration, and modern Java development practices.

---

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Run Locally](#run-locally)
  - [OpenAPI Code Generation](#openapi-code-generation)
  - [Build Docker Image](#build-docker-image)
- [Design Principles](#design-principles)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Contributing](#contributing)
- [License](#license)

---

## Features

- **Book Catalog**

  - Search by title
  - Lookup by ISBN
  - Tracks multiple instances of each book via compound book IDs: `<isbn>.<uniqueId>`

- **User Accounts**

  - Fetch user by account number
  - Lists borrowed books (resolved via Activity service)

- **Activity Tracking**

  - Check-in and check-out of individual book instances
  - Resilient handling of partial failures with detailed per-book results

- **Observability**

  - Micrometer metrics for sync and async method execution
  - Log4j2 logging with per-request traceability

- **API Design**

  - OpenAPI 3.0-based interface-first development
  - Swagger annotations on models and interfaces
  - CompletableFuture-based async controllers

---

## Technology Stack

- Java 17
- Spring Boot 3.4
- DynamoDB Enhanced Client
- OpenAPI Generator 7.x
- Micrometer + Log4j2
- JUnit 5 + Mockito
- Spotless (Google Java Format)
- Jib (for Docker image builds)

---

## Getting Started

### Prerequisites

- Java 17
- Docker (optional: for container builds)

### Run Locally

```bash
# Build and run the service
./gradlew bootRun
```

### OpenAPI Code Generation

This project uses interface-first development with OpenAPI 3.x. To generate the Java API interfaces and models:

```bash
./gradlew generateLibraryApi
```

To remove previously generated sources:

```bash
./gradlew cleanOpenApiGenerated
```

Run `generateLibraryApi` whenever the OpenAPI spec (`spec/libraryservice.yml`) changes.

### Build Docker Image

```bash
./gradlew jibDockerBuild
```

---

## Design Principles

- **Interface-First APIs**: All endpoints are defined via OpenAPI and implemented manually.
- **Compound Book IDs**: The format `<isbn>.<uniqueId>` enables referencing specific book copies across the system.
- **Async & Resilience**: Activity operations are non-blocking and tolerate individual failures gracefully.
- **Clean Code**: 100% unit test coverage, Spotless formatting, strict warning flags.
- **No Dockerfile Needed**: Jib builds images directly from your build configuration.

---

## Project Structure

```text
src/
  main/java/com/loudent/library/
    api/               # REST controllers
    service/           # Business logic
    dao/               # Data access (DynamoDB)
    oas/codegen/       # OpenAPI-generated interfaces and models
    aspect/            # Method timing and logging aspects
    config/            # Application configuration
    util/              # Shared utilities
```

---

## Testing

- Run all tests:

  ```bash
  ./gradlew test
  ```

- View coverage report:

  ```bash
  open build/reports/jacoco/test/html/index.html
  ```

---

## Contributing

This project is designed for demonstration purposes and is not accepting external contributions.

---

## License

MIT License. See `LICENSE` file for details.

