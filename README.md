# Loudent Library Service

[![Build](https://github.com/loudent/library/actions/workflows/ci.yml/badge.svg)](https://github.com/loudent2/library/actions)
[![codecov](https://codecov.io/gh/loudent2/library/branch/main/graph/badge.svg)](https://codecov.io/gh/loudent2/library) 
![Docker](https://img.shields.io/badge/docker-ready-blue)

A showcase Java Spring Boot application for managing a library catalog, user accounts, and borrowing activity. This service demonstrates clean architecture, asynchronous operations, unit test coverage, OpenAPI integration, and modern Java development practices.

---

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Run Locally](#run-locally)
  - [OpenAPI Code Generation](#openapi-code-generation)
  - [Run with Docker Compose](#run-with-docker-compose)
  - [Build Docker Image](#build-docker-image)
- [Design Principles](#design-principles)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [API Documentation](#api-documentation)
- [Common Makefile Commands](#common-makefile-commands)
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

## Observability

  | Endpoint                  | Purpose                      |
  |---------------------------|------------------------------|
  | `/actuator/health`        | Health check for containers  |
  | `/actuator/info`          | Application info             |

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
- Docker Compose (for local integration testing)

---


## Getting Started

### Prerequisites

- Java 17
- Docker (for running DynamoDB locally or containerizing the app)

---

### Build the Application

Before running the service locally or in Docker, build the application and generate the OpenAPI sources:

```bash
./gradlew clean generateLibraryApi build
```

You should re-run `generateLibraryApi` whenever the OpenAPI spec (`spec/libraryservice.yml`) changes.

---

### Run Locally

Start only DynamoDB in Docker and run the app from your IDE or terminal:

```bash
docker compose up
```

Then in another terminal:

```bash
./gradlew bootRun --args='--spring.profiles.active=ide'
```

---

### OpenAPI Code Generation

This project uses interface-first development with OpenAPI 3.x.

Generate the Java API interfaces and models (if you didn't already do this during the build step):

```bash
./gradlew generateLibraryApi
```

To clean up generated sources:

```bash
./gradlew cleanOpenApiGenerated
```

---

### Build Docker Image

Build the Library Service Docker image locally using Jib (no Dockerfile needed):

```bash
./gradlew jibDockerBuild
```

---

### Run with Docker Compose

#### Run both the Library Service and DynamoDB:

```bash
docker compose --profile library up
```

The app will use the Docker bridge network and automatically connect to the DynamoDB container. By default, it runs with `--spring.profiles.active=dev`.

To stop:

```bash
docker compose down
```


## Design Principles

- **Interface-First APIs:** All endpoints are defined via OpenAPI and implemented manually.
- **Compound Book IDs:** Format `<isbn>.<uniqueId>` enables referencing specific book copies across the system.
- **Async & Resilience:** Activity operations are non-blocking and tolerate individual failures gracefully.
- **Clean Code:** 100% unit test coverage, Spotless formatting, strict warning flags.
- **Modern Builds:** Jib builds images directly from Gradle with no Dockerfile.
- **Environment-Aware Configuration:** Spring profiles separate local and container environments.

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
docker/
  init-dynamodb.sh     # DynamoDB table creation and seed data
```

---

## Testing

Run all unit tests:

```bash
./gradlew test
```

View the coverage report:

```bash
open build/reports/jacoco/test/html/index.html
```
---

## 🔥 Smoke Testing the API

You can verify the API behavior using a Python-based smoke test script that simulates user actions like checking out and checking in books.

### ✅ Prerequisites

- Python 3 installed
- `pip3` available
- use 'make compose-up` to start a docker container.

### 🧪 Setup

1. **Create a virtual environment**:
   ```bash
   python3 -m venv venv
   source venv/bin/activate  # On macOS/Linux
   ```

2. **Install dependencies**:
   ```bash
   pip install requests
   ```

3. **Run the smoke test script**:
   ```bash
   python smoketest/library_local_smoke_test.py
   ```

4. **Deactivate the environment** (optional):
   ```bash
   deactivate
   ```

---

### 🔍 What It Tests

The script runs the following scenario using user `ACC123456` and book copies from `"Clean Code"` (`ISBN: 9783333333333`):

- Verify initial user state and catalog availability
- Check out two books
- Confirm user now has books and catalog reflects the change
- Partially check in one book
- Fully check in the remaining book
- Confirm everything is back to the original state

Each API call prints:
- Status code
- JSON response
- `✅ SUCCESS` or `❌ FAILURE`

The script exits with status `0` if all tests pass, or `1` if any fail — suitable for CI usage.


---

### Example API Usage

With the service running (on port 8080 by default), try some basic API calls:

#### Get Book by ISBN
```bash
curl --location 'http://localhost:8080/catalog/isbn/9781111111111'
```

#### Get Book by Title
```bash
curl --location 'http://localhost:8080/catalog/title' \
--header 'Content-Type: application/json' \
--data '{
    "title": "Sample Book"
}'
```

#### Get User by Account
```bash
curl --location 'http://localhost:8080/user/ACC123456'
```

#### Check out books
```bash
curl --location 'http://localhost:8080/activity/checkout' \
--header 'Content-Type: application/json' \
--data '{
  "accountNumber": "ACC123456",
  "bookIds": [
    "9781234567890.1",
    "9781234567890.2",
    "9789876543210.1"
  ]
}'
```

#### Check in books
```bash
curl --location 'http://localhost:8080/activity/checkin' \
--header 'Content-Type: application/json' \
--data '{
  "bookIds": [
    "9781234567890.1",
    "9781234567890.2",
    "9789876543210.1"
  ]
}'
```

#### Search the Catalog
```bash
curl --location 'http://localhost:8080/catalog/search' \
--header 'Content-Type: application/json' \
--data '{
  "authorLastName": "Doe"
}'
```

---

## API Documentation

You can generate human-readable HTML documentation from the OpenAPI spec:

```bash
make docs
```
The output will be available in build/generated/library-docs/index.html.

---

# Common Makefile Commands

Run these from the project root:

| Command             | Description                                          |
|---------------------|------------------------------------------------------|
| `make build`        | Build the application                                |
| `make test`         | Run unit tests                                       |
| `make openapi`      | Generate API interfaces and models from OpenAPI spec |
| `make clean-openapi`| Delete generated OpenAPI sources                     |
| `make image`        | Build Docker image using Jib                         |
| `make dynamodb-up`  | Start only DynamoDB container                        |
| `make compose-up`   | Start Library and DynamoDB containers                |
| `make compose-down` | Stop containers                                      |
| `make docs`         | Stop generates openapi spec documentation            |

---
## Contributing

This project is designed as a demonstration and is not accepting external contributions.

---

## License

MIT License. See [LICENSE](LICENSE) for details.

